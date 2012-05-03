package com.yammer.metrics.reporting.model;


public class DatadogGauge extends DatadogSeries<Number> {
  public DatadogGauge(String name, Number count, Long epoch) {
    super(name, count, epoch);
  }
  
  public String getType() {
    return "gauge";
  }
}
