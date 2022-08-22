/* ==================================================================
 * JdbcFlexibilityProviderDaoTests.java - 16/08/2022 6:07:00 pm
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

package net.solarnetwork.central.oscp.dao.jdbc.test;

import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allTokenData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcFlexibilityProviderDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcFlexibilityProviderDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCapacityProviderConfigurationDao capacityProviderDao;
	private JdbcCapacityOptimizerConfigurationDao capacityOptimizerDao;

	private JdbcFlexibilityProviderDao dao;
	private Long userId;

	private String lastToken;
	private UserLongCompositePK lastAuthId;

	@BeforeEach
	public void setup() {
		capacityProviderDao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
		capacityOptimizerDao = new JdbcCapacityOptimizerConfigurationDao(jdbcTemplate);
		dao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert_authToken() {
		// GIVEN
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(userId);

		// WHEN
		String result = dao.createAuthToken(id);

		// THEN
		assertThat("New token returned", result, is(notNullValue()));

		List<Map<String, Object>> data = allTokenData(jdbcTemplate, OscpRole.FlexibilityProvider);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID has been assigned", row, hasEntry("user_id", userId));
		assertThat("Row ID has been assigned", row,
				hasEntry(equalTo("id"), allOf(instanceOf(Long.class), notNullValue())));
		assertThat("Row creation date assigned", row, hasEntry(equalTo("created"), notNullValue()));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", row.get("created")));
		assertThat("Row enabled assigned", row, hasEntry("enabled", true));
		assertThat("Row token matches return value", row, hasEntry("token", result));
		lastToken = result;
		lastAuthId = new UserLongCompositePK(userId, (Long) row.get("id"));
	}

	@Test
	public void idForToken() {
		// GIVEN
		insert_authToken();

		// WHEN
		UserLongCompositePK result = dao.idForToken(lastToken);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result user ID matches", result.getUserId(), is(equalTo(userId)));

		List<Map<String, Object>> data = allTokenData(jdbcTemplate, OscpRole.FlexibilityProvider);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row entity ID matches returned value", result.getEntityId(),
				is(equalTo(row.get("id"))));
	}

	@Test
	public void idForToken_disabled() {
		// GIVEN
		insert_authToken();
		jdbcTemplate.update("UPDATE solaroscp.oscp_fp_token SET enabled = FALSE");

		// WHEN
		UserLongCompositePK result = dao.idForToken(lastToken);

		// THEN
		assertThat("Result NOT returned for disabled row", result, is(nullValue()));
	}

	@Test
	public void idForToken_noMatch() {
		// GIVEN
		insert_authToken();

		// WHEN
		UserLongCompositePK result = dao.idForToken(lastToken + "_NOT");

		// THEN
		assertThat("Result NOT returned for unmatched token", result, is(nullValue()));
	}

	@Test
	public void roleForAuthorization_noConfiguration() {
		// GIVEN
		insert_authToken();

		// WHEN
		AuthRoleInfo info = dao.roleForAuthorization(lastAuthId);

		// THEN
		assertThat("Info not returned when no configuration exists", info, is(nullValue()));
	}

	@Test
	public void roleForAuthorization_cp() {
		// GIVEN
		insert_authToken();

		CapacityProviderConfiguration cp = capacityProviderDao
				.get(capacityProviderDao.create(userId, OscpJdbcTestUtils.newCapacityProviderConf(userId,
						lastAuthId.getEntityId(), Instant.now())));

		// WHEN
		AuthRoleInfo info = dao.roleForAuthorization(lastAuthId);

		// THEN
		assertThat("Info returned", info, is(notNullValue()));
		assertThat("Role is Capacity Provider", info.role(), is(equalTo(OscpRole.CapacityProvider)));
		assertThat("Info ID is for Capacity Provider", info.id(), is(equalTo(cp.getId())));
	}

	@Test
	public void roleForAuthorization_co() {
		// GIVEN
		insert_authToken();

		CapacityOptimizerConfiguration co = capacityOptimizerDao
				.get(capacityOptimizerDao.create(userId, OscpJdbcTestUtils
						.newCapacityOptimizerConf(userId, lastAuthId.getEntityId(), Instant.now())));

		// WHEN
		AuthRoleInfo info = dao.roleForAuthorization(lastAuthId);

		// THEN
		assertThat("Info returned", info, is(notNullValue()));
		assertThat("Role is Capacity Provider", info.role(), is(equalTo(OscpRole.CapacityOptimizer)));
		assertThat("Info ID is for Capacity Provider", info.id(), is(equalTo(co.getId())));
	}

}
