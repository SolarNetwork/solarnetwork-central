These DTDs are here because of the way iBatis uses the thread context ClassLoader
to try to resolve these entities during XML parsing. Without making these resources
available the XML parser will attempt to download the DTD off the network. Perhaps
a better long term solution is to deploy a full XML Catalog Resolver, but for now
this is simple to implement and contains the issue.
