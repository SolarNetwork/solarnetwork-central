# PKI integration with Dogtag Certificate System

This plugin provides an implementation of `net.solarnetwork.central.user.biz.NodePKIBiz` that uses a
remote [Dogtag][1] instance to submit SolarNode certificate requests, approve those requests, and
download the approved certificates.

For this integration to work, a Dogtag user with agent-level permissions must be created in Dogtag.
This user must be able to submit CSRs, approve those CSRs, and download their associated
certificates. This user must have a client certificate created and stored in a keystore for this
plugin to use. The Dogtag root/signing certificate must be included in this keystore as a trusted
certificate.


# Configuration

There are two Configuration Admin PIDs used by this plugin. See the examples in `example/configuration`.

## net.solarnetwork.central.user.pki.dogtag

These settings control aspects of the SSL connection to Dogtag.

| Setting                   | Default | Description |
|---------------------------|---------|-------------|
| `dogtag.keystore.path`    | `classpath:/dogtag-client.jks` | The resource path to the Java keystore to use as the trust and key store for secure communication with the Dogtag server. |
| `dogtag.keystore.pass`    | `changeit` | The keystore password. |
| `dogtag.ciphers.disabled` | _various RSA-CBC ciphers_ | An optional list of SSL ciphers to disable on the SSL connection to Dogtag. |

Dogtag 10.6 might suffer from a [problem using RSA-CBC style ciphers][3]. The default 
`dogtag.ciphers.disabled` value works around this issue by disabling the affected ciphers.

## net.solarnetwork.central.user.pki.dogtag.DogtagPKIBiz

These settings control properties of the
[DogtagPKIBiz](src/net/solarnetwork/central/user/pki/dogtag/DogtagPKIBiz.java) service.

| Setting           | Default | Description |
|-------------------|---------|-------------|
| `baseUrl`         | `https://ca.solarnetworkdev.net:8443` | The base URL to the Dogtag instance to use. |
| `dogtagProfileId` | `SolarNode` | The name of the certificate profile to use when submitting requests. This profile is expected to accept a PKCS#10 certificate as input. |
| `pingResultsCacheSeconds` | `300` | The maximum number of seconds to cache ping (health check) results. |


# Notes

In modern Java 8 environments, the following JVM parameter is necessary for the integration with
Dogtag 10.0. to work:

```
-Djdk.tls.useExtendedMasterSecret=false
```

This is not required for Dogtag 10.6+. See the [JDK release notes][2] for more information.

 [1]: http://pki.fedoraproject.org/
 [2]: http://www.oracle.com/technetwork/java/javase/8all-relnotes-2226344.html#JDK-8148421
 [3]: https://pagure.io/dogtagpki/issue/3099
