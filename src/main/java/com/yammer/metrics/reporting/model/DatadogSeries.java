package com.yammer.metrics.reporting.model;

import java.util.ArrayList;
import java.util.List;

public abstract class DatadogSeries<T extends Number> {
  abstract protected String getType();

  private String name;
  private T count;
  private Long epoch;

  public DatadogSeries(String name, T count, Long epoch) {
    this.name = name;
    this.count = count;
    this.epoch = epoch;
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
