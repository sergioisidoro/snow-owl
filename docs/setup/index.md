# Set up Snow Owl

This section includes information on how to setup Snow Owl and get it running, including:

* Downloading
* Installing
* Starting
* Configuring

## Java (JVM) Version

Snow Owl is built using Java, and requires at least Java 8 in order to run. Only Oracle’s Java and the OpenJDK are supported. The same JVM version should be used on all Elasticsearch nodes and clients.

We recommend installing Java version 1.8.0_171 or a later version in the Java 8 release series. We recommend using a supported LTS version of Java.

The version of Java that Snow Owl will use can be configured by setting the JAVA_HOME environment variable.