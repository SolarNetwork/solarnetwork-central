/* ==================================================================
 * DinJdbcTestUtils.java - 21/02/2024 7:44:58 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.dao.jdbc.test;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.din.domain.CredentialConfiguration;

/**
 * Helper methods for datum input endpoint JDBC tests.
 *
 * @author matt
 * @version 1.0
 */
public final class DinJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(DinJdbcTestUtils.class);

	private DinJdbcTestUtils() {
		// not available
	}

	/**
	 * Create a new credential configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @return the entity
	 */
	public static CredentialConfiguration newCredentialConfiguration(Long userId, String username,
			String password) {
		CredentialConfiguration conf = new CredentialConfiguration(unassignedEntityIdKey(userId),
				Instant.now());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		conf.setUsername(username);
		conf.setPassword(password);
		return conf;
	}

	/**
	 * List credential configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCredentialConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.din_credential ORDER BY user_id, id");
		log.debug("solardin.din_credential table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
