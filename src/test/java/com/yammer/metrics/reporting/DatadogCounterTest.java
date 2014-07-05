package com.yammer.metrics.reporting;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.yammer.metrics.reporting.model.DatadogCounter;

public class DatadogCounterTest {

  @Test
  public void testSplitNameAndTags() {
    DatadogCounter counter = new DatadogCounter(
        "test[tag1:value1,tag2:value2,tag3:value3]", 1L, 1234L, "Test Host", null);
    List<String> tags = counter.getTags();

    assertEquals(3, tags.size());
    assertEquals("tag1:value1", tags.get(0));
    assertEquals("tag2:value2", tags.get(1));
    assertEquals("tag3:value3", tags.get(2));
  }

  @Test
  public void testStripInvalidCharsFromTags() {
    DatadogCounter counter = new DatadogCounter(
        "test[tag1:va  lue1,tag2:va .%lue2,ta  %# g3:value3]", 1L, 1234L, "Test Host", null);
    List<String> tags = counter.getTags();

    assertEquals(3, tags.size());
    assertEquals("tag1:value1", tags.get(0));
    assertEquals("tag2:value2", tags.get(1));
    assertEquals("tag3:value3", tags.get(2));
  }

  @Test
  public void testGlobalTags() {
    DatadogCounter counter = new DatadogCounter(
        "test[tag1:value1]", 1L, 1234L, "Test Host", Arrays.asList("tag2:value2"));
    List<String> tags = counter.getTags();

    assertEquals(2, tags.size());
    assertTrue(tags.contains("tag1:value1"));
    assertTrue(tags.contains("tag2:value2"));
  }
}
