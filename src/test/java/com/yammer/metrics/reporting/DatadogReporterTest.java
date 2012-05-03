package com.yammer.metrics.reporting;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.metrics.core.Clock;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.VirtualMachineMetrics;

public class DatadogReporterTest {

  MetricsRegistry metricsRegistry;
  MockTransport transport;
  VirtualMachineMetrics vm;
  Clock clock;
  static final MetricPredicate ALL = MetricPredicate.ALL;

  @Before
  public void setUp() {
    metricsRegistry = new MetricsRegistry();
    transport = new MockTransport();
    clock = Clock.defaultClock();
    vm = VirtualMachineMetrics.getInstance();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBasicSend() throws JsonParseException, JsonMappingException,
      IOException {
    DatadogReporter dd = new DatadogReporter(metricsRegistry, transport, clock);
    dd.printVmMetrics = false;

    Counter counter = metricsRegistry.newCounter(DatadogReporterTest.class,
        "my.counter");
    counter.inc();

    metricsRegistry.newGauge(DatadogReporterTest.class, "my.invocations",
        new Gauge<Long>() {
          private long numInovcations = 123;

          @Override
          public Long value() {
            return numInovcations++;
          }

        });

    assertEquals(0, transport.numRequests);
    dd.run();
    assertEquals(1, transport.numRequests);

    String body = new String(transport.lastRequest.getPostBody(), "UTF-8");
    Map<String, Object> request = new ObjectMapper().readValue(body,
        HashMap.class);

    assertEquals(1, request.keySet().size());
    List<Object> series = (List<Object>) request.get("series");

    assertEquals(2, series.size());
    Map<String, Object> counterEntry = (Map<String, Object>) series.get(0);
    Map<String, Object> gaugeEntry = (Map<String, Object>) series.get(1);

    assertEquals("com.yammer.metrics.reporting.DatadogReporterTest.my.counter",
        counterEntry.get("metric"));
    assertEquals("counter", counterEntry.get("type"));
    List<List<Number>> points = (List<List<Number>>) counterEntry.get("points");
    assertEquals(1, points.get(0).get(1));

    assertEquals(
        "com.yammer.metrics.reporting.DatadogReporterTest.my.invocations",
        gaugeEntry.get("metric"));
    assertEquals("gauge", gaugeEntry.get("type"));
    points = (List<List<Number>>) gaugeEntry.get("points");
    assertEquals(123, points.get(0).get(1));
  }

}
