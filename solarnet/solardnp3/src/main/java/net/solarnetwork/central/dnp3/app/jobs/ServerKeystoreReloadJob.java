/* ==================================================================
 * ServerKeystoreReloadJob.java - 16/08/2023 9:23:50 am
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

package net.solarnetwork.central.dnp3.app.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Instant;
import net.solarnetwork.central.net.proxy.config.TlsServerSettings;
import net.solarnetwork.central.net.proxy.service.impl.NettyDynamicProxyServer;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.security.CertificateUtils;

/**
 * Job to reload the proxy server's TLS keystore.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerKeystoreReloadJob extends JobSupport {

	private final TlsServerSettings settings;
	private final NettyDynamicProxyServer server;

	private Instant lastModified;

	/**
	 * Constructor.
	 * 
	 * @param settings
	 *        the settings
	 * @para server the server
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerKeystoreReloadJob(TlsServerSettings settings, NettyDynamicProxyServer server) {
		super();
		this.settings = requireNonNullArgument(settings, "settings");
		this.server = requireNonNullArgument(server, "server");
		setGroupId("DNP3");
		setId("ServerKeystoreReload");
	}

	@Override
	public void run() {
		Path certPath = settings.certificateKey();
		try {
			if ( Files.isReadable(certPath) ) {
				Instant mod = Files.getLastModifiedTime(certPath).toInstant();
				if ( lastModified != null && !mod.isAfter(lastModified) ) {
					// not changed, do nothing
					return;
				}
				lastModified = mod;
			}
		} catch ( IOException e ) {
			log.error("Error checking certificate [{}] modification date: {}", certPath, e.getMessage());
			return;
		}
		log.info("Loading server TLS certificate [{}]", certPath);
		KeyStore keyStore = CertificateUtils.serverKeyStore(settings.certificatePath(),
				settings.certificateKey(), NettyDynamicProxyServer.DEFAULT_KEYSTORE_ALIAS);
		server.setKeyStore(keyStore);
	}

}
