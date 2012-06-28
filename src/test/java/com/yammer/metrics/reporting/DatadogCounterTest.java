package com.yammer.metrics.reporting;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yammer.metrics.reporting.model.DatadogCounter;
import com.yammer.metrics.reporting.model.DatadogGauge;

public class DatadogCounterTest {

  @Test
  public void testSplitNameAndTags() {
    DatadogCounter counter = new DatadogCounter("test[tag1,tag2,tag3]", 1L,
        1234L, "Test Host");
    List<String> tags = counter.getTags();

    assertEquals(3, tags.size());
    assertEquals("tag1", tags.get(0));
    assertEquals("tag2", tags.get(1));
    assertEquals("tag3", tags.get(2));
  }

}
