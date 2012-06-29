package com.yammer.metrics.reporting;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.yammer.metrics.reporting.model.DatadogCounter;
import com.yammer.metrics.reporting.model.DatadogGauge;

public class DatadogGaugeTest {

  @Test
  public void testSplitNameAndTags() {
    DatadogGauge gauge = new DatadogGauge(
        "test[tag1:value1,tag2:value2,tag3:value3]", 1L, 1234L, "Test Host");
    List<String> tags = gauge.getTags();

    assertEquals(3, tags.size());
    assertEquals("tag1:value1", tags.get(0));
    assertEquals("tag2:value2", tags.get(1));
    assertEquals("tag3:value3", tags.get(2));
  }

  @Test
  public void testStripInvalidCharsFromTags() {
    DatadogGauge gauge = new DatadogGauge(
        "test[tag1:va  lue1,tag2:va .%lue2,ta  %# g3:value3]", 1L, 1234L,
        "Test Host");
    List<String> tags = gauge.getTags();

    assertEquals(3, tags.size());
    assertEquals("tag1:value1", tags.get(0));
    assertEquals("tag2:value2", tags.get(1));
    assertEquals("tag3:value3", tags.get(2));
  }

}
