/* ==================================================================
 * UserDnp3Biz.java - 1/08/2023 10:16:10 am
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

package net.solarnetwork.central.user.dnp3.biz;

import java.security.cert.X509Certificate;
import java.util.Collection;
import net.solarnetwork.central.dnp3.dao.CertificateFilter;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationInput;
import net.solarnetwork.dao.FilterResults;

/**
 * Service API for SolarUser DNP3 support.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserDnp3Biz {

	/**
	 * Save a collection of X.509 "trusted root" certificates.
	 * 
	 * @param userId
	 *        the user to save the certificates for
	 * @param certificates
	 *        the certificates to save
	 * @return the saved certificate entities
	 */
	Collection<TrustedIssuerCertificate> saveTrustedIssuerCertificates(Long userId,
			X509Certificate[] certificates);

	/**
	 * List the available trusted issuer certificates for a given user,
	 * optionally filtered.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @param filter
	 *        an optional filter
	 * @return the matching certificates; never {@literal null}
	 */
	FilterResults<TrustedIssuerCertificate, UserStringCompositePK> trustedIssuerCertificatesForUser(
			Long userId, CertificateFilter filter);

	/**
	 * Create a new server configuration.
	 * 
	 * @param userId
	 *        the ID of the user to create the configuration for
	 * @param input
	 *        the configuration input
	 * @return the persisted configuration; never {@literal null}
	 */
	ServerConfiguration createServer(Long userId, ServerConfigurationInput input);

	/**
	 * Update an existing server configuration.
	 * 
	 * @param userId
	 *        the ID of the user to update the configuration for
	 * @param serverId
	 *        the ID of the server to update
	 * @param input
	 *        the configuration input
	 * @return the persisted configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if an
	 *         entity matching {@code userId} and {@code serverId} does not
	 *         exist
	 */
	ServerConfiguration updateServer(Long userId, Long serverId, ServerConfigurationInput input);

	/**
	 * List the available server configurations for a given user, optionally
	 * filtered.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @param filter
	 *        an optional filter
	 * @return the matching configurations; never {@literal null}
	 */
	FilterResults<ServerConfiguration, UserLongCompositePK> serversForUser(Long userId,
			ServerFilter filter);

}
