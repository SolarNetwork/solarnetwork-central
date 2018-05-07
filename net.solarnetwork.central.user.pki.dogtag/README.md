# PKI integration with Dogtag Certificate System

This plugin provides an implementation of `net.solarnetwork.central.user.biz.NodePKIBiz`
that uses a remote [Dogtag][1] instance to submit SolarNode
certificate requests, approve those requests, and download the approved certificates.

# Notes

In version 10.0 of Dogtag, the following runtime parameter introduced in Java **8u161**
is required by the JVM for the integration to succeed:

		-Djdk.tls.useExtendedMasterSecret=false

See the [JDK release notes][2] for more information.


 [1]: http://pki.fedoraproject.org/
 [2]: http://www.oracle.com/technetwork/java/javase/8all-relnotes-2226344.html#JDK-8148421
