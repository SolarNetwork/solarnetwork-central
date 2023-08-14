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

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Locale;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import net.solarnetwork.central.dnp3.dao.CertificateFilter;
import net.solarnetwork.central.dnp3.dao.ServerDataPointFilter;
import net.solarnetwork.central.dnp3.dao.ServerFilter;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.dnp3.domain.ServerAuthConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurations;
import net.solarnetwork.central.user.dnp3.domain.ServerControlConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerMeasurementConfigurationInput;
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
	 * Update the enabled status of trusted issuer certificates, optionally
	 * filtered.
	 * 
	 * @param userId
	 *        the user ID to update configurations for
	 * @param filter
	 *        an optional filter
	 * @param enabled
	 *        the enabled status to set
	 */
	void updateTrustedIssuerCertificateEnabledStatus(Long userId, CertificateFilter filter,
			boolean enabled);

	/**
	 * Delete an existing trusted issuer certificate.
	 * 
	 * @param userId
	 *        the user ID to delete the configuration for
	 * @param subjectDn
	 *        the subject DN of the certificate to delete
	 */
	void deleteTrustedIssuerCertificate(Long userId, String subjectDn);

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
	 * Update the enabled status of servers, optionally filtered.
	 * 
	 * @param userId
	 *        the user ID to update configurations for
	 * @param filter
	 *        an optional filter
	 * @param enabled
	 *        the enabled status to set
	 */
	void updateServerEnabledStatus(Long userId, ServerFilter filter, boolean enabled);

	/**
	 * Delete an existing server configuration.
	 * 
	 * @param userId
	 *        the ID of the user to delete the configuration for
	 * @param serverId
	 *        the ID of the server to delete
	 */
	void deleteServer(Long userId, Long serverId);

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

	/**
	 * Create or update a server auth configuration.
	 * 
	 * @param userId
	 *        the user ID of the configuration to update
	 * @param serverId
	 *        the server ID of the configuration to update
	 * @param identifier
	 *        the identifier of the configuration to update
	 * @param input
	 *        the configuration input
	 * @return the persisted configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         server entity matching {@code userId} and {@code serverId} does
	 *         not exist
	 */
	ServerAuthConfiguration saveServerAuth(Long userId, Long serverId, String identifier,
			ServerAuthConfigurationInput input);

	/**
	 * Update the enabled status of server auths, optionally filtered.
	 * 
	 * @param userId
	 *        the user ID to update configurations for
	 * @param filter
	 *        an optional filter
	 * @param enabled
	 *        the enabled status to set
	 */
	void updateServerAuthEnabledStatus(Long userId, ServerFilter filter, boolean enabled);

	/**
	 * Delete an existing server auth configuration.
	 * 
	 * @param userId
	 *        the user ID of the configuration to update
	 * @param serverId
	 *        the server ID of the configuration to update
	 * @param identifier
	 *        the identifier of the configuration to update
	 */
	void deleteServerAuth(Long userId, Long serverId, String identifier);

	/**
	 * List the available server auth configurations for a given user,
	 * optionally filtered.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @param filter
	 *        an optional filter
	 * @return the matching configurations; never {@literal null}
	 */
	FilterResults<ServerAuthConfiguration, UserLongStringCompositePK> serverAuthsForUser(Long userId,
			ServerFilter filter);

	/**
	 * Create or update a server measurement configuration.
	 * 
	 * @param userId
	 *        the user ID of the configuration to update
	 * @param serverId
	 *        the server ID of the configuration to update
	 * @param index
	 *        the index of the configuration to update
	 * @param input
	 *        the configuration input
	 * @return the persisted configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         server entity matching {@code userId} and {@code serverId} does
	 *         not exist
	 */
	ServerMeasurementConfiguration saveServerMeasurement(Long userId, Long serverId, Integer index,
			ServerMeasurementConfigurationInput input);

	/**
	 * Update the enabled status of server measurements, optionally filtered.
	 * 
	 * @param userId
	 *        the user ID to update configurations for
	 * @param filter
	 *        an optional filter
	 * @param enabled
	 *        the enabled status to set
	 */
	void updateServerMeasurementEnabledStatus(Long userId, ServerDataPointFilter filter,
			boolean enabled);

	/**
	 * Delete a server measurement configuration.
	 * 
	 * @param userId
	 *        the user ID of the configuration to update
	 * @param serverId
	 *        the server ID of the configuration to update
	 * @param index
	 *        the index of the configuration to update
	 */
	void deleteServerMeasurement(Long userId, Long serverId, Integer index);

	/**
	 * List the available server measurement configurations for a given user,
	 * optionally filtered.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @param filter
	 *        an optional filter
	 * @return the matching configurations; never {@literal null}
	 */
	FilterResults<ServerMeasurementConfiguration, UserLongIntegerCompositePK> serverMeasurementsForUser(
			Long userId, ServerDataPointFilter filter);

	/**
	 * Create or update a server control configuration.
	 * 
	 * @param userId
	 *        the user ID of the configuration to update
	 * @param serverId
	 *        the server ID of the configuration to update
	 * @param index
	 *        the index of the configuration to update the configuration index
	 * @param input
	 *        the configuration input
	 * @return the persisted configuration; never {@literal null}
	 * @throws AuthorizationException
	 *         with {@link AuthorizationException.Reason#UNKNOWN_OBJECT} if a
	 *         server entity matching {@code userId} and {@code serverId} does
	 *         not exist
	 */
	ServerControlConfiguration saveServerControl(Long userId, Long serverId, Integer index,
			ServerControlConfigurationInput input);

	/**
	 * Update the enabled status of server controls, optionally filtered.
	 * 
	 * @param userId
	 *        the user ID to update configurations for
	 * @param filter
	 *        an optional filter
	 * @param enabled
	 *        the enabled status to set
	 */
	void updateServerControlEnabledStatus(Long userId, ServerDataPointFilter filter, boolean enabled);

	/**
	 * Delete a server control configuration.
	 * 
	 * @param userId
	 *        the user ID of the configuration to update
	 * @param serverId
	 *        the server ID of the configuration to update
	 * @param index
	 *        the index of the configuration to update
	 */
	void deleteServerControl(Long userId, Long serverId, Integer index);

	/**
	 * List the available server control configurations for a given user,
	 * optionally filtered.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @param filter
	 *        an optional filter
	 * @return the matching configurations; never {@literal null}
	 */
	FilterResults<ServerControlConfiguration, UserLongIntegerCompositePK> serverControlsForUser(
			Long userId, ServerDataPointFilter filter);

	/**
	 * Get an example server configuration CSV.
	 * 
	 * <p>
	 * The supported {@code mimeType} values include:
	 * </p>
	 * 
	 * <ul>
	 * <li>text/csv</li>
	 * <li>application/vnd.openxmlformats-officedocument.spreadsheetml.sheet</li>
	 * </ul>
	 * 
	 * @param mimeType
	 *        the desired example mime type
	 * @return the resource
	 * @throws IllegalArgumentException
	 *         if {@code mimeType} is not supported
	 */
	Resource serverConfigurationCsvExample(MimeType mimeType);

	/**
	 * Import a CSV resource of server measurement and control configurations.
	 * 
	 * <p>
	 * The expected structure of the CSV is:
	 * </p>
	 * 
	 * <ol>
	 * <li><b>Node ID</b> - a datum stream node ID</li>
	 * <li><b>Source ID</b> - a datum stream source ID, or control ID for
	 * control types</li>
	 * <li><b>Property</b> - the datum stream property name; optional for
	 * control types</li>
	 * <li><b>Type</b> - a {@link MeasurementType} or {@link ControlType}</li>
	 * <li><b>Multiplier</b> - an optional number to multiple property values
	 * by</li>
	 * <li><b>Offset</b> - an optional number to add to property values</li>
	 * <li><b>Decimal Scale</b> - an optional integer decimal scale to round
	 * decimals to; empty or -1 for no rounding</li>
	 * </ol>
	 * 
	 * @param userId
	 *        the ID of the user to import configurations for
	 * @param serverId
	 *        the ID of the server to import configurations for
	 * @param csv
	 *        the CSV resource to import
	 * @param locale
	 *        a locale for messages, or {@literal null} to use the runtime
	 *        default
	 * @return the generated server configurations
	 * @throws IOException
	 *         if an IO error occurs
	 * @throws IllegalArgumentException
	 *         if a parsing error occurs
	 * @throws javax.validation.ConstraintViolationException
	 *         if a validation error occurs
	 */
	ServerConfigurations importServerConfigurationsCsv(Long userId, Long serverId, InputStreamSource csv,
			Locale locale) throws IOException;

	/**
	 * Export server measurement and control configurations as CSV.
	 * 
	 * @param userId
	 *        the ID of the user to import configurations for
	 * @param filter
	 *        an optional filter
	 * @param out
	 *        the output steram to write to
	 * @param locale
	 *        a locale for messages, or {@literal null} to use the runtime
	 *        default
	 * @throws IOException
	 *         if an IO error occurs
	 * @see #importServerConfigurationsCsv(Long, Long, InputStreamSource)
	 */
	void exportServerConfigurationsCsv(Long userId, ServerDataPointFilter filter, OutputStream out,
			Locale locale) throws IOException;

}
