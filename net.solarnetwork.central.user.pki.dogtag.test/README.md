# Notes on Dogtag 10.0.x

In modern Java 8 environments, the following JVM parameter is necessary for the integration with
Dogtag to work:

```
-Djdk.tls.useExtendedMasterSecret=false
```

This is not required for Dogtag 10.6+.
