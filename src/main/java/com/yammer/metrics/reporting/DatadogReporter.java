package com.yammer.metrics.reporting;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Clock;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Sampling;
import com.yammer.metrics.core.Summarizable;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.VirtualMachineMetrics;
import com.yammer.metrics.reporting.Transport.Request;
import com.yammer.metrics.reporting.model.DatadogCounter;
import com.yammer.metrics.reporting.model.DatadogGauge;
import com.yammer.metrics.stats.Snapshot;

public class DatadogReporter extends AbstractPollingReporter implements
    MetricProcessor<Long> {

  public boolean printVmMetrics = true;
  protected final Locale locale = Locale.US;
  protected final Clock clock;
  private final String host;
  private final List<String> tags;
  protected final MetricPredicate predicate;
  protected final Transport transport;
  private static final Logger LOG = LoggerFactory
      .getLogger(DatadogReporter.class);
  private final VirtualMachineMetrics vm;

  private static final JsonFactory jsonFactory = new JsonFactory();
  private static final ObjectMapper mapper = new ObjectMapper(jsonFactory);
  private JsonGenerator jsonOut;

  public static void enable(long period, TimeUnit unit, String apiKey) {
    enable(period, unit, apiKey, null, null);
  }

  public static void enable(long period, TimeUnit unit, String apiKey,
      List<String> tags) {
    enable(period, unit, apiKey, null, tags);
  }

  public static void enable(long period, TimeUnit unit, String apiKey,
      String host, List<String> tags) {
    DatadogReporter dd = new DatadogReporter(Metrics.defaultRegistry(), apiKey,
        host, tags);
    dd.start(period, unit);
  }

  public static void enableForEc2Instance(long period, TimeUnit unit,
      String apiKey) throws IOException {
    String hostName = AwsHelper.getEc2InstanceId();
    DatadogReporter dd = new DatadogReporter(Metrics.defaultRegistry(), apiKey,
        hostName, null);
    dd.start(period, unit);
  }

  public DatadogReporter(MetricsRegistry registry, String apiKey) {
    this(registry, apiKey, null, null);
  }

  public DatadogReporter(MetricsRegistry registry, String apiKey, List<String> tags) {
    this(registry, apiKey, null, tags);
  }

  public DatadogReporter(MetricsRegistry registry, String apiKey, String host,
      List<String> tags) {
    this(registry, MetricPredicate.ALL, VirtualMachineMetrics.getInstance(),
        new HttpTransport("app.datadoghq.com", apiKey), Clock.defaultClock(),
        host, tags);
  }

  public DatadogReporter(MetricsRegistry metricsRegistry,
      MetricPredicate predicate, VirtualMachineMetrics vm, Transport transport,
      Clock clock, String host, List<String> tags) {
    super(metricsRegistry, "datadog-reporter");
    this.vm = vm;
    this.transport = transport;
    this.predicate = predicate;
    this.clock = clock;
    this.host = host;
    this.tags = tags;
  }

  @Override
  public void run() {
    Request request = null;
    try {
      request = transport.prepare();
      jsonOut = jsonFactory.createJsonGenerator(request.getBodyWriter());
      jsonOut.writeStartObject();
      jsonOut.writeFieldName("series");
      jsonOut.writeStartArray();
    } catch (IOException ioe) {
      LOG.error("Could not prepare request", ioe);
      return;
    }

    final long epoch = clock.time() / 1000;
    if (this.printVmMetrics) {
      pushVmMetrics(epoch);
    }
    pushRegularMetrics(epoch);

    try {
      jsonOut.writeEndArray();
      jsonOut.writeEndObject();
      jsonOut.flush();
      request.send();
    } catch (Exception e) {
      LOG.error("Error sending metrics", e);
    }
  }

  public void processCounter(MetricName name, Counter counter, Long epoch)
      throws Exception {
    pushCounter(name, counter.count(), epoch);
  }

  public void processGauge(MetricName name, Gauge<?> gauge, Long epoch)
      throws Exception {
    pushGauge(name, (Number) gauge.value(), epoch);
  }

  public void processHistogram(MetricName name, Histogram histogram, Long epoch)
      throws Exception {
    pushSummarizable(name, histogram, epoch);
    pushSampling(name, histogram, epoch);
  }

  public void processMeter(MetricName name, Metered meter, Long epoch)
      throws Exception {
    pushCounter(name, meter.count(), epoch);
    pushGauge(name, meter.meanRate(), epoch, "mean");
    pushGauge(name, meter.oneMinuteRate(), epoch, "1MinuteRate");
    pushGauge(name, meter.fiveMinuteRate(), epoch, "5MinuteRate");
    pushGauge(name, meter.fifteenMinuteRate(), epoch, "15MinuteRate");
  }

  public void processTimer(MetricName name, Timer timer, Long epoch)
      throws Exception {
    processMeter(name, timer, epoch);
    pushSummarizable(name, timer, epoch);
    pushSampling(name, timer, epoch);
  }

  private void pushSummarizable(MetricName name, Summarizable summarizable,
      Long epoch) {
    pushGauge(name, summarizable.min(), epoch, "min");
    pushGauge(name, summarizable.max(), epoch, "max");
    pushGauge(name, summarizable.mean(), epoch, "mean");
    pushGauge(name, summarizable.stdDev(), epoch, "stddev");
  }

  private void pushSampling(MetricName name, Sampling sampling, Long epoch) {
    final Snapshot snapshot = sampling.getSnapshot();
    pushGauge(name, snapshot.getMedian(), epoch, "median");
    pushGauge(name, snapshot.get75thPercentile(), epoch, "75percentile");
    pushGauge(name, snapshot.get95thPercentile(), epoch, "95percentile");
    pushGauge(name, snapshot.get98thPercentile(), epoch, "98percentile");
    pushGauge(name, snapshot.get99thPercentile(), epoch, "99percentile");
    pushGauge(name, snapshot.get999thPercentile(), epoch, "999percentile");
  }

  protected void pushRegularMetrics(long epoch) {
    for (Entry<String, SortedMap<MetricName, Metric>> entry : getMetricsRegistry()
        .groupedMetrics(predicate).entrySet()) {
      for (Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
        final Metric metric = subEntry.getValue();
        if (metric != null) {
          try {
            metric.processWith(this, subEntry.getKey(), epoch);
          } catch (Exception e) {
            LOG.error("Error pushing metric", e);
          }
        }
      }
    }
  }

  protected void pushVmMetrics(long epoch) {
    sendGauge("jvm.memory.heap_usage", vm.heapUsage(), epoch);
    sendGauge("jvm.memory.non_heap_usage", vm.nonHeapUsage(), epoch);
    for (Entry<String, Double> pool : vm.memoryPoolUsage().entrySet()) {
      String gaugeName = String.format("jvm.memory.memory_pool_usage[pool:%s]",
          pool.getKey());

      sendGauge(gaugeName, pool.getValue(), epoch);
    }

    pushGauge("jvm.daemon_thread_count", vm.daemonThreadCount(), epoch);
    pushGauge("jvm.thread_count", vm.threadCount(), epoch);
    pushCounter("jvm.uptime", vm.uptime(), epoch);
    sendGauge("jvm.fd_usage", vm.fileDescriptorUsage(), epoch);

    for (Entry<Thread.State, Double> entry : vm.threadStatePercentages()
        .entrySet()) {
      String gaugeName = String.format("jvm.thread-states[state:%s]",
          entry.getKey());
      sendGauge(gaugeName, entry.getValue(), epoch);
    }

    for (Entry<String, VirtualMachineMetrics.GarbageCollectorStats> entry : vm
        .garbageCollectors().entrySet()) {
      pushGauge("jvm.gc.time", entry.getValue().getTime(TimeUnit.MILLISECONDS), epoch);
      pushCounter("jvm.gc.runs", entry.getValue().getRuns(), epoch);
    }
  }

  private void pushCounter(MetricName metricName, Long count, Long epoch,
      String... path) {
    pushCounter(sanitizeName(metricName, path), count, epoch);

  }

  private void pushCounter(String name, Long count, Long epoch) {
    DatadogCounter counter = new DatadogCounter(name, count, epoch, host, tags);
    try {
      mapper.writeValue(jsonOut, counter);
    } catch (Exception e) {
      LOG.error("Error writing counter", e);
    }
  }

  private void pushGauge(MetricName metricName, Number count, Long epoch,
      String... path) {
    sendGauge(sanitizeName(metricName, path), count, epoch);
  }

  private void pushGauge(String name, long count, long epoch) {
    sendGauge(name, new Long(count), epoch);
  }

  private void sendGauge(String name, Number count, Long epoch) {
    DatadogGauge gauge = new DatadogGauge(name, count, epoch, host, tags);
    try {
      mapper.writeValue(jsonOut, gauge);
    } catch (Exception e) {
      LOG.error("Error writing gauge", e);
    }
  }

  protected String sanitizeName(MetricName name, String... path) {
    final StringBuilder sb = new StringBuilder(name.getGroup());
    sb.append('.');
    sb.append(name.getType()).append('.');

    if (name.hasScope()) {
      sb.append(name.getScope()).append('.');
    }

    String[] metricParts = name.getName().split("\\[");
    sb.append(metricParts[0]);

    for (String part : path) {
      sb.append('.').append(part);
    }

    for (int i = 1; i < metricParts.length; i++) {
      sb.append('[').append(metricParts[i]);
    }
    return sb.toString();
  }

}
