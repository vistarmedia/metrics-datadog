package com.yammer.metrics.reporting.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public abstract class DatadogSeries<T extends Number> {
  abstract protected String getType();

  private String name;
  private T count;
  private Long epoch;
  private String host;

  public DatadogSeries(String name, T count, Long epoch, String host) {
    this.name = name;
    this.count = count;
    this.epoch = epoch;
    this.host = host;
  }

  @JsonInclude(Include.NON_NULL)
  public String getHost() {
    return host;
  }
  
  public String getMetric() {
    return name;
  }

  public List<List<Number>> getPoints() {
    List<Number> point = new ArrayList<Number>();
    point.add(epoch);
    point.add(count);

    List<List<Number>> points = new ArrayList<List<Number>>();
    points.add(point);
    return points;
  }
}
