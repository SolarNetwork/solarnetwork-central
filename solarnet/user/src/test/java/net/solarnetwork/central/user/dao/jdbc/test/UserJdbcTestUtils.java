/* ==================================================================
 * UserJdbcTestUtils.java - 22/03/2025 7:53:47 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.jdbc.test;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.stream.Collectors.joining;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.central.user.domain.UserSecretEntity;

/**
 * Helper methods for user JDBC tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class UserJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(UserJdbcTestUtils.class);

	private UserJdbcTestUtils() {
		// not available
	}

	/**
	 * Create a new user key pair instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param key
	 *        the key
	 * @param keystore
	 *        the keystore
	 * @return the entity
	 */
	public static UserKeyPairEntity newUserKeyPairEntity(Long userId, String key, byte[] keystore) {
		var ts = now().truncatedTo(MILLIS);
		return new UserKeyPairEntity(userId, key, ts, ts, keystore);
	}

	/**
	 * List user key pair rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allUserKeyPairEntityData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaruser.user_keypair ORDER BY user_id, skey");
		log.debug("solaruser.user_keypair table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new user secret instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param topic
	 *        the topic
	 * @param key
	 *        the key
	 * @param secret
	 *        the secret
	 * @return the entity
	 */
	public static UserSecretEntity newUserSecretEntity(Long userId, String topic, String key,
			String secret) {
		var ts = now().truncatedTo(MILLIS);
		return new UserSecretEntity(userId, topic, key, ts, ts, secret);
	}

	/**
	 * List user secret rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allUserSecretEntityData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaruser.user_secret ORDER BY user_id, topic, skey");
		log.debug("solaruser.user_secret table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
