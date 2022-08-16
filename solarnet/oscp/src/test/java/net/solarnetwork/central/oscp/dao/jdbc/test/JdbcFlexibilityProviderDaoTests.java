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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcFlexibilityProviderDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcFlexibilityProviderDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcFlexibilityProviderDao dao;
	private Long userId;

	private String lastToken;

	@BeforeEach
	public void setup() {
		dao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	private List<Map<String, Object>> allFlexibilityProviderTokenData() {
		List<Map<String, Object>> data = jdbcTemplate
				.queryForList("select * from solaroscp.oscp_fp_token ORDER BY user_id, id");
		log.debug("solaroscp.oscp_fp_token table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	@Test
	public void insert_authToken() {
		// GIVEN
		UserLongCompositePK id = UserLongCompositePK.unassignedEntityIdKey(userId);

		// WHEN
		String result = dao.createAuthToken(id);

		// THEN
		assertThat("New token returned", result, is(notNullValue()));

		List<Map<String, Object>> data = allFlexibilityProviderTokenData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID has been assigned", row, hasEntry("user_id", userId));
		assertThat("Row ID has been assigned", row, hasEntry(equalTo("id"), notNullValue()));
		assertThat("Row creation date assigned", row, hasEntry(equalTo("created"), notNullValue()));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", row.get("created")));
		assertThat("Row enabled assigned", row, hasEntry("enabled", true));
		assertThat("Row token matches return value", row, hasEntry("token", result));
		lastToken = result;
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

		List<Map<String, Object>> data = allFlexibilityProviderTokenData();
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

}
