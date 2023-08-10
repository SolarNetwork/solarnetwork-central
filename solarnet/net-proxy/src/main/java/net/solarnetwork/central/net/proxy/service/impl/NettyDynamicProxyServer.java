/* ==================================================================
 * NettyDynamicProxyServer.java - 1/08/2023 11:14:02 am
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

import static net.solarnetwork.central.security.CertificateUtils.canonicalSubjectDn;
import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509KeyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.central.net.proxy.domain.SimpleProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.service.DynamicProxyServer;
import net.solarnetwork.central.net.proxy.service.ProxyConfigurationProvider;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Netty implementation of {@link DynamicProxyServer}.
 * 
 * @author matt
 * @version 1.0
 */
public class NettyDynamicProxyServer
		implements DynamicProxyServer, ServiceLifecycleObserver, X509KeyManager {

	/** The {@code keyStoreAlias} property default value. */
	public static final String DEFAULT_KEYSTORE_ALIAS = "server";

	/** The wire-level logger name. */
	public static final String WIRE_LOG_NAME = "net.solarnetwork.central.net.proxy.WIRE";

	/** The SSL session key for the proxy connection settings. */
	public static final String SSL_SESSION_PROXY_SETTINGS_KEY = "ProxyConnectionSettings";

	private static final String[] DEFAULT_TLS_PROTOCOLS = new String[] { "TLSv1.3" };

	private static final Logger log = LoggerFactory.getLogger(NettyDynamicProxyServer.class);

	private final Queue<ProxyConfigurationProvider> providers = new ConcurrentLinkedQueue<>();
	private final SocketAddress bindAddress;
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;
	private String[] tlsProtocols = DEFAULT_TLS_PROTOCOLS;
	private String[] keyStoreAliases = new String[] { DEFAULT_KEYSTORE_ALIAS };
	private KeyStore keyStore;
	private boolean wireLogging = false;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The loopback address will be used.
	 * </p>
	 * 
	 * @param port
	 *        the server bind port
	 */
	public NettyDynamicProxyServer(int port) {
		this(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
	}

	/**
	 * Constructor.
	 * 
	 * @param address
	 *        the server bind address
	 * @param port
	 *        the server bind port
	 */
	public NettyDynamicProxyServer(String address, int port) {
		this(new InetSocketAddress(requireNonNullArgument(address, "address"), port));
	}

	/**
	 * Constructor.
	 * 
	 * @param bindAddress
	 *        the server bind address
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public NettyDynamicProxyServer(SocketAddress bindAddress) {
		super();
		this.bindAddress = requireNonNullArgument(bindAddress, "bindAddress");
		this.bossGroup = new NioEventLoopGroup(1);
		this.workerGroup = new NioEventLoopGroup();
	}

	@Override
	public void serviceDidStartup() {
		startProxyServer();
	}

	@Override
	public void serviceDidShutdown() {
		stopProxyServer();
	}

	@Override
	public synchronized void registerConfigurationProvider(ProxyConfigurationProvider provider) {
		requireNonNullArgument(provider, "provider");
		for ( ProxyConfigurationProvider p : providers ) {
			if ( p == provider ) {
				return;
			}
		}
		providers.add(provider);
	}

	@Override
	public synchronized boolean unregisterConfigurationProvider(ProxyConfigurationProvider provider) {
		requireNonNullArgument(provider, "provider");
		for ( Iterator<ProxyConfigurationProvider> itr = providers.iterator(); itr.hasNext(); ) {
			ProxyConfigurationProvider p = itr.next();
			if ( p == provider ) {
				itr.remove();
				return true;
			}
		}
		return false;
	}

	private synchronized void startProxyServer() {
		try {
			ServerBootstrap b = new ServerBootstrap();
			SslContext sslContext = null;
			if ( keyStore != null ) {
				sslContext = SslContextBuilder.forServer(this).trustManager(new InternalTrustManager())
						.protocols(tlsProtocols).clientAuth(ClientAuth.REQUIRE).build();
			}

			// @formatter:off
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_REUSEADDR, true)
				.childHandler(new ProxyChannelInitializer(sslContext))
				.childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.AUTO_READ, false)
				.bind(bindAddress).sync()
				.addListener((f) -> {
					log.info("Proxy server started on {} supporting TLS protocols [{}]", bindAddress, 
							Arrays.stream(tlsProtocols).collect(Collectors.joining(", ")));
				})
				.channel().closeFuture().addListener((f) -> {
					log.info("Proxy server stopped on {}", bindAddress);
				});
			// @formatter:on
		} catch ( InterruptedException e ) {
			log.warn("Proxy server ineterrupted: shutting down.");
		} catch ( SSLException e ) {
			log.error("Proxy server SSL error: {}", e.toString(), e);
		}
	}

	private synchronized void stopProxyServer() {
		try {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		} catch ( Exception e ) {
			log.warn("Error shutting down proxy server: {}", e.toString(), e);
		}
	}

	private class ProxyChannelInitializer extends ChannelInitializer<SocketChannel> {

		private final SslContext sslContext;

		private ProxyChannelInitializer(SslContext sslContext) {
			this.sslContext = sslContext;
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			if ( sslContext != null ) {
				ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
			}
			if ( wireLogging ) {
				ch.pipeline().addLast(new LoggingHandler(WIRE_LOG_NAME));
			}
			ch.pipeline().addLast(new ProxyFrontendHandler());
		}
	}

	@Override
	public String[] getClientAliases(String keyType, Principal[] issuers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return keyStoreAliases;
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		return getKeyStoreAlias();
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		try {
			Certificate[] certs = keyStore.getCertificateChain(alias);
			if ( certs == null ) {
				return null;
			}
			List<X509Certificate> result = new ArrayList<>(certs.length);
			for ( int i = 0, len = certs.length; i < len; i++ ) {
				X509Certificate cert = (X509Certificate) certs[i];
				// skip any self-signed (CA) certs
				if ( !cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal()) ) {
					result.add(cert);
				}
			}
			return result.toArray(X509Certificate[]::new);
		} catch ( KeyStoreException e ) {
			log.error("Error getting certificate chain for alias '{}': {}", alias, e.toString());
		}
		return null;
	}

	@Override
	public PrivateKey getPrivateKey(String alias) {
		try {
			Key key = keyStore.getKey(alias, new char[0]);
			return (PrivateKey) key;
		} catch ( UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e ) {
			log.error("Error getting private key for alias '{}': {}", alias, e.toString());
		}
		return null;
	}

	private final class InternalTrustManager extends X509ExtendedTrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			checkClientTrusted(chain, authType, (SSLEngine) null);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			throw new UnsupportedOperationException();
		}

		private static final X509Certificate[] EMPTY_CERT_ARRAY = new X509Certificate[0];

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			/*- Could returns known issuers, but over time the list can grow unbounded
			 	and this would "leak" information about supported issuers.
			List<X509Certificate> result = new ArrayList<>(32);
			for ( ProxyConfigurationProvider provider : providers ) {
				Iterable<X509Certificate> certs = provider.acceptedIdentityIssuers();
				if ( certs != null ) {
					for ( X509Certificate cert : certs ) {
						result.add(cert);
					}
				}
			}
			return result.toArray(X509Certificate[]::new);
			*/
			return EMPTY_CERT_ARRAY;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
			requireNonEmptyArgument(chain, "chain");
			log.debug("Validating client trust using {}: {}", authType, canonicalSubjectDn(chain[0]));
			chain[0].checkValidity();
			for ( ProxyConfigurationProvider provider : providers ) {
				ProxyConnectionRequest req = new SimpleProxyConnectionRequest(null, chain);
				try {
					ProxyConnectionSettings settings = provider.authorize(req);
					if ( settings != null ) {
						if ( engine != null ) {
							SSLSession session = engine.getHandshakeSession();
							if ( session == null ) {
								session = engine.getSession();
							}
							session.putValue(SSL_SESSION_PROXY_SETTINGS_KEY, settings);
							return;
						}
						throw new CertificateException("Internal TLS misconfiguration.");
					}
				} catch ( Exception e ) {
					Throwable cause = e.getCause() != null ? e.getCause() : e;
					log.warn("Unauthorized client certificate [{}]: {}", canonicalSubjectDn(chain[0]),
							e.getMessage());
					if ( cause instanceof CertificateException ce ) {
						throw ce;
					}
					throw new CertificateException("Client authorization failed.", e);
				}
			}
			log.warn(
					"Unauthorized client certificate [{}] (not supported by any proxy configuration provider)",
					chain[0].getSubjectX500Principal().getName());
			throw new CertificateException("Unauthorized client certificate.");
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
				throws CertificateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
				throws CertificateException {
			throw new UnsupportedOperationException();
		}

	}

	/**
	 * Get the wire-logging flag.
	 * 
	 * @return {@literal true} if wire-level logging should be enabled
	 */
	public boolean isWireLogging() {
		return wireLogging;
	}

	/**
	 * Set the wire-logging flag.
	 * 
	 * @param wireLogging
	 *        {@literal true} if wire-level logging should be enabled
	 */
	public void setWireLogging(boolean wireLogging) {
		this.wireLogging = wireLogging;
	}

	/**
	 * Get the supported TLS protocols.
	 * 
	 * @return the protocols to support
	 */
	public String[] getTlsProtocols() {
		return tlsProtocols;
	}

	/**
	 * Set the supported TLS protocols.
	 * 
	 * @param tlsProtocols
	 *        the protocols to support; if {@literal null} then the default
	 *        protocols will be set
	 */
	public void setTlsProtocols(String[] tlsProtocols) {
		this.tlsProtocols = (tlsProtocols != null && tlsProtocols.length > 0 ? tlsProtocols
				: DEFAULT_TLS_PROTOCOLS);
	}

	/**
	 * Set the SSL key store.
	 * 
	 * @return the key store
	 */
	public KeyStore getKeyStore() {
		return keyStore;
	}

	/**
	 * Get the SSL key store.
	 * 
	 * @param keyStore
	 *        the key store to set
	 */
	public void setKeyStore(KeyStore keyStore) {
		this.keyStore = keyStore;
	}

	/**
	 * Get the SSL key store alias for the server certificate.
	 * 
	 * @return the key store alias; defaults to {@link #DEFAULT_KEYSTORE_ALIAS}
	 */
	public final String getKeyStoreAlias() {
		return keyStoreAliases[0];
	}

	/**
	 * Set the SSL key store alias for the server certificate.
	 * 
	 * @param keyStoreAlias
	 *        the key store alias to set; if {@literal null} then
	 *        {@link #DEFAULT_KEYSTORE_ALIAS} will be used instead
	 */
	public final void setKeyStoreAlias(String keyStoreAlias) {
		String alias = (keyStoreAlias != null ? keyStoreAlias : DEFAULT_KEYSTORE_ALIAS);
		this.keyStoreAliases[0] = alias;
	}

}
