# Metrics Datadog Reporter
Simple Metrics reporter that sends The Goods to Datadog. Real person
documentation pending

## Usage

~~~scala
import com.yammer.metrics.reporting.DatadogReporter

...

DatadogReporter.enable(15, TimeUnit.SECONDS, myDatadogKey)
~~~

## Tags

You may optionally specify tags for either individual metrics or globally
to be applied to all metrics.  Tags may be simple strings (i.e. "foo") or
name-value pairs (i.e. "foo:bar").

### Metric Tags

To tag a specific metric you use square brackets in the metrics name, like so:

~~~
test[tag1] // Tag only
test[tag1:value1,tag2:value2,tag3] // Tag and value, multiple tags
~~~

Invalid characters will be stripped out.

### Global Tags

If you have global tags you'd like applied to all your metrics you can specify
them when enabling, like so:

~~~scala
DatadogReporter.enable(15, TimeUnit.SECONDS, myDatadogKey, Arrays.asList("tag1", "tag2:value2"))
~~~

## Maven

This repo is subject to change. Nuts!

* Remote:   https://s3.amazonaws.com/maven.vistarmedia.com/maven/snapshots
* Group:    com.vistarmedia
* Artifact: metrics-datadog
* Version:  0.0.18-SNAPSHOT
