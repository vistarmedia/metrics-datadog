# Metrics Datadog Reporter
Simple Metrics reporter that sends The Goods to Datadog. Real person
documentation pending

## Usage

~~~scala
import com.yammer.metrics.reporting.DatadogReporter

...

DatadogReporter.enable(15, TimeUnit.SECONDS, myDatadogKey)
~~~

## Maven

This repo is subject to change. Nuts!

* Remote:   https://s3.amazonaws.com/maven.vistarmedia.com/maven/snapshots
* Group:    com.vistarmedia
* Artifact: metrics-datadog
* Version:  0.0.18-SNAPSHOT
