/* ==================================================================
 * BasicNetworkIdentificationBiz.java - 7/10/2021 10:09:41 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.biz;

import static net.solarnetwork.util.ByteUtils.UTF8;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.NetworkIdentity;

/**
 * Basic implementation of {@link NetworkIdentificationBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicNetworkIdentificationBiz implements NetworkIdentificationBiz {

	private final String networkIdentityKey;
	private final Resource termsOfService;
	private final String host;
	private final Integer port;
	private final boolean forceTLS;
	private final Map<String, String> networkServiceUrls;

	/**
	 * Constructor.
	 * 
	 * @param networkIdentityKey
	 *        the identity key
	 * @param termsOfService
	 *        the TOS resource
	 * @param host
	 *        the host
	 * @param port
	 *        the port
	 * @param forceTLS
	 *        {@literal true} if TLS must be used
	 * @param networkServiceURLs
	 *        map of network service URLs to advertise
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BasicNetworkIdentificationBiz(String networkIdentityKey, Resource termsOfService, String host,
			Integer port, boolean forceTLS, Map<String, String> networkServiceUrls) {
		super();
		this.networkIdentityKey = requireNonNullArgument(networkIdentityKey, "networkIdentityKey");
		this.termsOfService = requireNonNullArgument(termsOfService, "termsOfService");
		this.host = requireNonNullArgument(host, "host");
		this.port = requireNonNullArgument(port, "port");
		this.forceTLS = requireNonNullArgument(forceTLS, "forceTLS");
		this.networkServiceUrls = requireNonNullArgument(networkServiceUrls, "networkServiceUrls");
	}

	@Override
	public NetworkIdentity getNetworkIdentity() {
		String tos = "";
		try {
			tos = FileCopyUtils.copyToString(
					new BufferedReader(new InputStreamReader(termsOfService.getInputStream(), UTF8)));
		} catch ( IOException e ) {
			tos = "Error, missing TOS resource: " + e.getMessage();
		}
		BasicNetworkIdentity ident = new BasicNetworkIdentity(networkIdentityKey, tos, host, port,
				forceTLS);
		ident.setNetworkServiceURLs(networkServiceUrls);
		return ident;
	}

}
