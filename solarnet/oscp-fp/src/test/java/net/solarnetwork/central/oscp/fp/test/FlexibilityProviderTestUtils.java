/* ==================================================================
 * FlexibilityProviderTestUtils.java - 17/08/2022 3:37:12 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.test;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Testing utilities for Flexibility Provider.
 * 
 * @author matt
 * @version 1.0
 */
public final class FlexibilityProviderTestUtils {

	private static final Logger log = LoggerFactory.getLogger(FlexibilityProviderTestUtils.class);

	private FlexibilityProviderTestUtils() {
		// not available
	}

	/**
	 * Save a new user and Flexibility Provider authorization ID for the current
	 * actor.
	 * 
	 * <p>
	 * This is designed to support {@link WithMockAuthenticatedToken} tests.
	 * </p>
	 * 
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @return the new ID
	 */
	public static UserLongCompositePK saveUserAndFlexibilityProviderAuthIdForCurrentActor(
			JdbcOperations jdbcOps) {
		Authentication auth = SecurityUtils.getCurrentAuthentication();
		AuthRoleInfo info = OscpSecurityUtils.authRoleInfo();
		Long userId = info.id().getUserId();

		CommonDbTestUtils.insertUser(jdbcOps, userId, format("tester.%d@localhost", userId),
				format("tester.%d.secret", userId), format("Tester %d", userId));

		return saveFlexibilityProviderAuthId(jdbcOps, userId, auth.getPrincipal().toString());
	}

	/**
	 * Save a new Flexibility Provider authorization ID.
	 * 
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param userId
	 *        the user ID
	 * @param token
	 *        the token
	 * @return the new ID
	 */
	public static UserLongCompositePK saveFlexibilityProviderAuthId(JdbcOperations jdbcOps, Long userId,
			String token) {
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcOps.update((con) -> {
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO solaroscp.oscp_fp_token (user_id, token) VALUES (?, ?) RETURNING id",
					Statement.RETURN_GENERATED_KEYS);
			stmt.setObject(1, userId, Types.BIGINT);
			stmt.setString(2, token);
			return stmt;
		}, holder);
		Long fpId = holder.getKeyAs(Long.class);
		return new UserLongCompositePK(userId, fpId);
	}

	/**
	 * Create a new configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param entityId
	 *        the entity ID to use
	 * @param flexibilityProviderId
	 *        the flexibility provider ID
	 * @param created
	 *        the creation date
	 * @return the new instance
	 */
	public static CapacityProviderConfiguration newCapacityProvider(Long userId, Long entityId,
			Long flexibilityProviderId, Instant created) {
		CapacityProviderConfiguration conf = new CapacityProviderConfiguration(
				entityId == null ? UserLongCompositePK.unassignedEntityIdKey(userId)
						: new UserLongCompositePK(userId, entityId),
				created);
		conf.setModified(created);
		conf.setBaseUrl("http://example.com/" + randomUUID().toString());
		conf.setEnabled(true);
		conf.setFlexibilityProviderId(flexibilityProviderId);
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		conf.setToken(randomUUID().toString());
		return conf;
	}

	/**
	 * Create a new configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param entityId
	 *        the entity ID to use
	 * @param flexibilityProviderId
	 *        the flexibility provider ID
	 * @param created
	 *        the creation date
	 * @return the new instance
	 */
	public static CapacityOptimizerConfiguration newCapacityOptimizer(Long userId, Long entityId,
			Long flexibilityProviderId, Instant created) {
		CapacityOptimizerConfiguration conf = new CapacityOptimizerConfiguration(
				entityId == null ? UserLongCompositePK.unassignedEntityIdKey(userId)
						: new UserLongCompositePK(userId, entityId),
				created);
		conf.setModified(created);
		conf.setBaseUrl("http://example.com/" + randomUUID().toString());
		conf.setEnabled(true);
		conf.setFlexibilityProviderId(flexibilityProviderId);
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		conf.setToken(randomUUID().toString());
		return conf;
	}

	/**
	 * Dump the contents of the {@code solaroscp.oscp_cp_conf} table.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the table contents
	 */
	public static List<Map<String, Object>> allCapacityProviderConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_cp_conf ORDER BY user_id, id");
		log.debug("solaroscp.oscp_cp_conf table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Dump the contents of the {@code solaroscp.oscp_cp_token} table.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the table contents
	 */
	public static List<Map<String, Object>> allCapacityProviderTokenData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_cp_token ORDER BY user_id, id");
		log.debug("solaroscp.oscp_cp_token table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Dump the contents of the {@code solaroscp.oscp_co_conf} table.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the table contents
	 */
	public static List<Map<String, Object>> allCapacityOptimizerConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_co_conf ORDER BY user_id, id");
		log.debug("solaroscp.oscp_co_conf table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Dump the contents of the {@code solaroscp.oscp_co_token} table.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the table contents
	 */
	public static List<Map<String, Object>> allCapacityOptimizerTokenData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_co_token ORDER BY user_id, id");
		log.debug("solaroscp.oscp_co_token table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
