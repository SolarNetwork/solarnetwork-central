# SolarFlux VerneMQ Webhook App

This project provides a web server application that can respond to [VerneMQ][vernemq] webhook
requests for authenticating and authorizing users. The VerneMQ server is assumed to be hosting MQTT
topics related to SolarNode data, posted by the [SolarFlux Upload][solarflux-upload] SolarNode
plugin. That plugin posts node data using MQTT topics like `node/N/datum/0/S`, where `N` is a _node
ID_ and `S` is a _source ID_. The `0` in the topic represents a _raw_ aggregation type, meaning
SolarNode is posting non-aggregated data to the topic. Example topics look like:

```
node/1/datum/0/Meter
node/2/datum/0/Building1/Room1/Light1
node/2/datum/0/Building1/Room1/Light2
```

**Note** that any leading `/` in a source ID is stripped from the topic name.

By default, this app will re-write topic names to include a `user/U` prefix, where `U` is the
SolarNetwork _user ID_ of the authenticated user. That means that subscribing  or publishing to the
example topics above will result in topics like the following actually being used:

```
user/123/node/1/datum/0/Meter
user/234/node/2/datum/0/Building1/Room1/Light1
user/234/node/2/datum/0/Building1/Room1/Light2
```

Three styles of authentication are supported, as outlined below.

## SolarNework token authentication

This style of authentication is designed to be used by arbitrary MQTT client applications that want
to subscribe to topics and have access to a token and corresponding token secret. Authorization to
MQTT topics is controlled by the Security Policy attached to the token used for authentication: only
the nodes, sources, and aggregation levels authorized by the Security Policy are allowed.

In this scenario, the following MQTT credentials are required:

| Property | Description |
|----------|-------------|
| Client ID | If `auth.requireTokenClientIdPrefix` is `true` then this must start with the token ID, followed by anything unique. The MQTT spec limits the client ID to 23 characters, but SolarFlux servers might allow longer names. |
| Username | A SolarNetwork security token ID. |
| Password | A SolarNetwork security token secret. |

## SolarNework signed token digest authentication

This style of authentication is designed to be used by arbitrary MQTT client applications that want
to subscribe to topics, without necessarily having direct access to a token secret. It can be useful
to provide either pre-signed token digests or a signing key to a client application, so the actual
token secret is not exposed to the client. Authorization to MQTT topics is controlled by the
Security Policy attached to the token used for authentication: only the nodes, sources, and
aggregation levels authorized by the Security Policy are allowed.

In this scenario, the following MQTT credentials are required:

| Property | Description |
|----------|-------------|
| Client ID | If `auth.requireTokenClientIdPrefix` is `true` then this must start with the token ID, followed by anything unique. The MQTT spec limits the client ID to 23 characters, but SolarFlux servers might allow longer names. |
| Username | A SolarNetwork security token ID. |
| Password | A [SolarNetwork V2][sn-auth-v2] signature, extended with a `Date=D` attribute. |

The request data used to sign the token request must be the following:

| Property | Value | Description|
|----------|-------|------------|
| Verb | `GET` | The canonical request HTTP verb. |
| Path | `/solarflux/auth` | The canonical request URI. This must match the configured `solarnetwork.api.authPath` [application property](#general-properties). |
| Host | `data.solarnetwork.net` | The signed `Host` header value. This must match the configured `solarnetwork.api.host` [application property](#general-properties). |
| Date |  | The signed `X-SN-Date` header value. The date must also be provided in the final signature as a `Date=D` attribute. See below for more details. |

Thus the canonical headers are `Host` and `X-SN-Date` and these must both be included as signed headers in the signature.

The final signature value must include an additional `Date=D` attribute, where `D`
is a Unix epoch value, i.e. the number of **seconds** since 1 Jan, 1970. **Note** this is
not specified in milliseconds, as is the default in programming languages like Java and 
JavaScript.

An example MQTT password thus looks like this:

```
SNWS2 Credential=a09sjds09wu9wjsd9uy2,SignedHeaders=host;x-sn-date,Signature=4d56e33d69300c163f414ee688e9771eabce45d5a425e480a8cc605e97263919,Date=1545069502
```

The signature can be shortened to include just the `Signature` and `Date` attributes, if desired. Thus the following
MQTT password is equivalent to the previous example:

```
Signature=4d56e33d69300c163f414ee688e9771eabce45d5a425e480a8cc605e97263919,Date=1545069502
```

Also note the the order of the attributes is not significant. Thus the following MQTT password is equivalent to the previous example:

```
Date=1545069502,Signature=4d56e33d69300c163f414ee688e9771eabce45d5a425e480a8cc605e97263919
```


## SolarNode authentication

This style of authentication is designed to be used by SolarNode devices posting data to SolarFlux.
In this scenario, this application does not perform authentication. Instead it assumes the SolarNode
has connected via TLS to a TLS-terminating proxy server that uses the node's X.509 certificate to
authenticate the node. This application also assumes that that proxy server has verified that 
the MQTT Client ID used to connect is the _node ID_ as presented in the X.509 certificate.

The `auth.nodeIpMask` [application property](#general-properties) should be configured to restrict
this type of authentication to only connections coming from the IP address range of the TLS proxy server(s).

Given all those assumptions, this application uses SolarNode style authentication when the
following MQTT credentials are used:

| Property | Description |
|----------|-------------|
| Client ID | A SolarNode ID. |
| Username | Must be `solarnode`. |
| Password | Not used. |

These connections are only allowed to publish messages to topics constrained by the node ID
specified in the Client ID, e.g.  `node/N/datum/0/S`.

Details on how to deploy a TLS proxy server is outside the scope of this document.


# Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build -x test

The finished WAR file will be `build/libs/solarflux-vernemq-webook-X.war` where `X` is the
version number.


# Running in Servlet Container

If the WAR is deployed into a servlet container (e.g. Tomcat) then a servlet
context path of `/solarflux-vernemq-webhook` is used by default. Assuming the container is
listening on port **8080**, to access the app from a browser you would visit
http://localhost:8080/solarflux-vernemq-webhook/.


# Running Standalone

The WAR file can be directly executed like this:

	$ java -Dspring.profiles.active=development -jar build/libs/solarflux-vernemq-webhook-0.1.war
	
This will start the web server on port **8080** by default. You can verify the
app has started up using a browser, or from the command line like this:

	$ curl http://localhost:8080/

You can add a context path to make it run like it does by default when deployed
in a container by passing a `server.contextPath` parameter, or change the port
via a `server.port` parameter, like this:

	$ java -Dserver.contextPath=/solarflux-vernemq-webhook -Dserver.port=8888 \
	-Dspring.profiles.active=development -jar build/libs/solarflux-vernemq-webhook-0.1.war


# Tweaking Environment Properties

When running standalone, or deploying Tomcat in Eclipse, you can override
application properties for the active profile by creating an `application-X.yml`
file in the root of the project. For example when running the **development**
profile, create a file named `application-development.yml`. Those settings
will override any settings from the `src/main/resources/application.yml` file
included in the app.

## General properties

The following properties control various aspects of the application:

| Property | Default | Description |
|----------|---------|-------------|
| `auth.nodeIpMask` |  | An IP address range in CIDR format to limit node-based authentication to, for example `192.168.0.0/24`. |
| `auth.requireTokenClientIdPrefix` | `true` | If true, for token authentication the MQTT client ID must start with the token ID. |
| `auth.userTopicPrefixEnabled` | `true` | If true, topics will be re-written to include a `user/X` prefix, where `X` is the ID of the authenticated user. |
| `auth.allowDirectTokenAuthentication` | `true` | If true, allow raw token secret values to be used for passwords (in addition to signed hashes). If false then only signed hashes are allowed. |
| `cache.actor.ttl` | `900` | The maximum length of time to cache user details, in seconds. |
| `mqtt.forceCleanSession` | `true` | If `true` then force the MQTT _clean session_ connection flag to `true`, which prevents session persistence. |
| `mqtt.maxQos` | `1` | The maximum MQTT Qos value to enforce for publish/subscribe. |
| `solarnewtork.api.authPath` | `/solarflux/auth` | For token authentication, the URL path to use. |
| `solarnetwork.api.host` | `data.solarnetwork.net` | For token authentication, the hostname to use. |
| `solarnetwork.api.maxDateSkew` | `900000` | For token authentication, the maximum date skew to use, in milliseconds. |

## Database connection properties

The following properties all start with a `spring.datasource.` prefix.

| Property | Default | Description |
|----------|---------|-------------|
| `url` | `jdbc:postgresql://localhost:5496/solarnetwork` | The JDBC URL to use. |
| `username` | `solarauth` | The JDBC username to use. |
| `password` | `solarauth` | The JDBC password to use. |


# Eclipse setup

The project is configured as an [Eclipse IDE][eclipse] project, and can
be run as a normal web project using a Tomcat server configuration. The 
project has the following requirements:

 * [Eclipse JEE IDE][eclipse], version Photon or later.
 * The [Buildship][buildship] plugin, version 2.2 or later.
 * The [Checkstyle][checkstyle-eclipse] plugin, version 8.12 or later.
 * [Tomcat][tomcat], version 8.5 or later.

The plugins can be found via the Eclipse Marketplace 
(**Help > Eclipse Marketplace...**). You can install Tomcat anywhere,
and then configure it under **Preferences > Server > Runtime Environments**.

When launching Tomcat from within Eclipse, you must update the launch 
configuration so the desired profile is set. Additionally it helps to define
the working directory for Tomcat to be the root of the project. Both settings
are controlled on the Tomcat launch configuration's **Arguments** tab. Add
the following to the **VM arguments** field:

	-Dspring.profiles.active=development
	
Then change the **Working directory** field to **Other** and enter

	${workspace_loc:solarflux-vernemq-webhook}


[buildship]: https://projects.eclipse.org/projects/tools.buildship
[checkstyle-eclipse]: http://eclipse-cs.sourceforge.net
[eclipse]: https://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/neon3
[sn-auth-v2]: https://github.com/SolarNetwork/solarnetwork/wiki/SolarNet-API-authentication-scheme-V2
[solarflux-upload]: https://github.com/SolarNetwork/solarnetwork-node/tree/develop/net.solarnetwork.node.upload.flux
[tomcat]: https://tomcat.apache.org/download-80.cgi
[vernemq]: https://vernemq.con
