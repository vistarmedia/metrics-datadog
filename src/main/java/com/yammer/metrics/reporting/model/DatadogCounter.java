package com.yammer.metrics.reporting.model;

import java.util.List;

public class DatadogCounter extends DatadogSeries<Long> {

  public DatadogCounter(String name, Long count, Long epoch, String host, List<String> tags) {
    super(name, count, epoch, host, tags);
  }

  public String getType() {
    return "counter";
  }
}
