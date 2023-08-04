/* ==================================================================
 * SimpleProxyConfigurationProvider.java - 3/08/2023 6:13:18 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.net.proxy.service.impl;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.net.proxy.util.CertificateUtils.canonicalSubjectDn;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.net.proxy.service.ProxyConfigurationProvider;
import net.solarnetwork.central.net.proxy.util.CertificateUtils;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StringUtils;

/**
 * Simple proxy configuration provider designed for testing.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleProxyConfigurationProvider implements ProxyConfigurationProvider {

	/**
	 * The default external server command.
	 * 
	 * <p>
	 * The default command is equivalent to:
	 * </p>
	 * 
	 * <pre>{@code
	 * /usr/local/bin/ncat -l {port} -k -c '/usr/bin/xargs -n1 echo'
	 * }</pre>
	 * 
	 * @see #setExternalServerCommand(String[])
	 */
	public static List<String> DEFAULT_EXTERNAL_SERVER_COMMAND = Collections.unmodifiableList(
			Arrays.asList("/usr/local/bin/ncat", "-l", "{port}", "-k", "-c", "/usr/bin/xargs -n1 echo"));

	private static final Logger log = LoggerFactory.getLogger(SimpleProxyConfigurationProvider.class);

	private final DynamicPortRegistrar portRegistrar;
	private final List<SimplePrincipalMapping> userMappings;
	private String[] externalServerCommand = DEFAULT_EXTERNAL_SERVER_COMMAND.toArray(String[]::new);

	/**
	 * Constructor.
	 * 
	 * @param portRegistrar
	 *        the port registrar to use
	 * @param userMappings
	 *        the user mappings to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleProxyConfigurationProvider(DynamicPortRegistrar portRegistrar,
			List<SimplePrincipalMapping> userMappings) {
		super();
		this.portRegistrar = requireNonNullArgument(portRegistrar, "portRegistrar");
		this.userMappings = requireNonNullArgument(userMappings, "userMappings");
	}

	@Override
	public ProxyConnectionSettings authorize(ProxyConnectionRequest request)
			throws AuthorizationException {
		final String ident = requestIdentity(request);
		if ( log.isDebugEnabled() ) {
			log.debug("Connection authorization request received for: [{}]", ident);
		}
		final X509Certificate[] clientChain = request.principalIdentity();

		for ( SimplePrincipalMapping mapping : userMappings ) {
			if ( mapping.subjectUserMapping().containsKey(ident) ) {
				KeyStore trustStore = mapping.trustStore();
				try {
					PKIXCertPathValidatorResult vr = CertificateUtils
							.validateCertificateChain(trustStore, clientChain);
					if ( log.isInfoEnabled() ) {
						TrustAnchor ta = vr.getTrustAnchor();
						log.info(
								"Validated connection authorization request identity [{}], trusted by [{}]",
								ident, ta.getTrustedCert().getSubjectX500Principal());
					}

					return new DynamicConnectionSettings(request, trustStore);
				} catch ( Exception e ) {
					throw new AuthorizationException(Reason.ACCESS_DENIED, ident, e);
				}
			}
		}
		return null;
	}

	private String requestIdentity(ProxyConnectionRequest request) {
		final String ident = request.principalIdentity() != null
				&& request.principalIdentity().length > 0
						? canonicalSubjectDn(request.principalIdentity()[0])
						: request.principal();
		return ident;
	}

	@Override
	public Iterable<X509Certificate> acceptedIdentityIssuers() {
		List<X509Certificate> result = new ArrayList<>(8);
		for ( SimplePrincipalMapping mapping : userMappings ) {
			KeyStore keyStore = mapping.trustStore();
			try {
				Enumeration<String> aliases = keyStore.aliases();
				while ( aliases.hasMoreElements() ) {
					String alias = aliases.nextElement();
					Certificate cert = keyStore.getCertificate(alias);
					if ( cert instanceof X509Certificate x509 ) {
						result.add(x509);
					}
				}
			} catch ( KeyStoreException e ) {
				log.error("Error reading key store aliases from principal mapping: {}", e.toString());
			}
		}
		return result;
	}

	/**
	 * {@link ProxyConnectionSettings} that also implements
	 * {@link ServiceLifecycleObserver} and resolves a dynamic unused port when
	 * {@link #serviceDidStartup()} is invoked.
	 */
	private final class DynamicConnectionSettings
			implements ProxyConnectionSettings, ServiceLifecycleObserver {

		private final ProxyConnectionRequest request;
		private final KeyStore trustStore;
		private int port = 0;

		private String[] serverCommand;
		private Process server;

		private DynamicConnectionSettings(ProxyConnectionRequest request, KeyStore trustStore) {
			super();
			this.request = requireNonNullArgument(request, "request");
			this.trustStore = requireNonNullArgument(trustStore, "trustStore");
		}

		@Override
		public ProxyConnectionRequest connectionRequest() {
			return request;
		}

		@Override
		public KeyStore clientTrustStore() {
			return trustStore;
		}

		@Override
		public String destinationHost() {
			return "127.0.0.1";
		}

		@Override
		public int destinationPort() {
			return port;
		}

		@Override
		public synchronized void serviceDidStartup() {
			if ( port > 0 ) {
				return;
			}
			final int newPort = portRegistrar.reserveNewPort();

			final Map<String, Object> cmdParameters = Collections.singletonMap("port", newPort);
			final int cmdLen = externalServerCommand.length;
			String[] cmd = new String[cmdLen];
			for ( int i = 0; i < cmdLen; i++ ) {
				cmd[i] = StringUtils.expandTemplateString(externalServerCommand[i], cmdParameters);
			}

			// start up dynamic server to proxy to
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(cmd);
			try {
				serverCommand = cmd;
				server = pb.start();
				log.info("Started dynamic echo server {} with command: {}", server.pid(),
						Arrays.stream(cmd).collect(joining(" ")));
				try {
					Thread.sleep(200); // give server a chance to start listening; could make time configurable
				} catch ( InterruptedException e ) {
					// ignore
				}
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			}

			port = newPort;
		}

		@Override
		public synchronized void serviceDidShutdown() {
			if ( server != null && server.isAlive() ) {
				log.info("Stopping dynamic echo server {} [{}]", server.pid(),
						Arrays.stream(serverCommand).collect(joining(" ")));
				server.destroy();
				server = null;
				serverCommand = null;
			}
			final int port = destinationPort();
			if ( port > 0 ) {
				portRegistrar.releasePort(port);
				this.port = 0;
			}
		}

	}

	/**
	 * Get the external server command.
	 * 
	 * @return the command
	 */
	public String[] getExternalServerCommand() {
		return externalServerCommand;
	}

	/**
	 * Set the external server command.
	 * <p>
	 * This is the OS-specific command to run that starts up the external server
	 * to proxy the connection to. The command can include a <code>{port}</code>
	 * placeholder that will be replaced with a dynamically allocated port for
	 * the server to bind to.
	 * </p>
	 * 
	 * @param externalServerCommand
	 *        the command to set
	 */
	public void setExternalServerCommand(String[] externalServerCommand) {
		this.externalServerCommand = externalServerCommand;
	}

}
