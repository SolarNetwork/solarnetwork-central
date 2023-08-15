/* ==================================================================
 * Dnp3JdbcTestUtils.java - 5/08/2023 6:11:11 pm
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

package net.solarnetwork.central.dnp3.dao.jdbc.test;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.dnp3.test.Dnp3TestUtils.certificatesFromResource;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;

/**
 * Helper methods for DNP3 JDBC tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class Dnp3JdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(Dnp3JdbcTestUtils.class);

	private Dnp3JdbcTestUtils() {
		// not available
	}

	/**
	 * Create a new trusted issuer certificate instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param certResource
	 *        the certificate classpath resource
	 * @return the entity
	 */
	public static TrustedIssuerCertificate newTrustedIssuerCertificate(Long userId,
			String certResource) {
		X509Certificate cert = certificatesFromResource(certResource)[0];
		return newTrustedIssuerCertificate(userId, cert);
	}

	/**
	 * Create a new trusted issuer certificate instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param cert
	 *        the certificate
	 * @return the entity
	 */
	public static TrustedIssuerCertificate newTrustedIssuerCertificate(Long userId,
			X509Certificate cert) {
		TrustedIssuerCertificate conf = new TrustedIssuerCertificate(userId, cert, Instant.now());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		return conf;
	}

	/**
	 * List all trusted issuer certificate rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allTrustedIssuerCertificateData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardnp3.dnp3_ca_cert ORDER BY user_id, subject_dn");
		log.debug("solardnp3.dnp3_ca_cert table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new server configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param name
	 *        the name
	 * @return the entity
	 */
	public static ServerConfiguration newServerConfiguration(Long userId, String name) {
		ServerConfiguration conf = new ServerConfiguration(unassignedEntityIdKey(userId), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		conf.setName(name);
		return conf;
	}

	/**
	 * List server configuration rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allServerConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardnp3.dnp3_server ORDER BY user_id, id");
		log.debug("solardnp3.dnp3_server table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List server auth configuration rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allServerAuthConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardnp3.dnp3_server_auth ORDER BY user_id, server_id, ident");
		log.debug("solardnp3.dnp3_server_auth table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List server measurement configuration rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allServerMeasurementConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardnp3.dnp3_server_meas ORDER BY user_id, server_id, idx");
		log.debug("solardnp3.dnp3_server_meas table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List server control configuration rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allServerControlConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardnp3.dnp3_server_ctrl ORDER BY user_id, server_id, idx");
		log.debug("solardnp3.dnp3_server_ctrl table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
