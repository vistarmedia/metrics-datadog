package com.yammer.metrics.reporting.model;

import java.util.List;

public class DatadogGauge extends DatadogSeries<Number> {
  public DatadogGauge(String name, Number count, Long epoch, String host, List<String> tags) {
    super(name, count, epoch, host, tags);
  }

  public String getType() {
    return "gauge";
  }
}
