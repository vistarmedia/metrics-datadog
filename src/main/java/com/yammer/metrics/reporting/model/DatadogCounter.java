package com.yammer.metrics.reporting.model;

public class DatadogCounter extends DatadogSeries<Long> {
  
  public DatadogCounter(String name, Long count, Long epoch) {
    super(name, count, epoch);
  }

  public String getType() {
    return "counter";
  }
}
