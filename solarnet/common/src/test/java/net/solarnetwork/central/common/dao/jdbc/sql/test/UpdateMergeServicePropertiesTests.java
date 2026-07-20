/* ==================================================================
 * UpdateMergeServicePropertiesTests.java - 21/07/2026 7:30:39 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateMergeServiceProperties;
import net.solarnetwork.central.dao.ModifiableServicePropertiesDao.MergeMode;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Test cases for the {@link UpdateMergeServiceProperties} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class UpdateMergeServicePropertiesTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(anyString())).willReturn(stmt);
	}

	@Test
	public void prep_common2_simple() throws SQLException {
		// GIVEN
		final String tableName = "my_table";
		final UserStringCompositePK pk = new UserStringCompositePK(randomLong(), randomString());
		final Map<String, Object> sprops = Map.of("foo", "bar");

		givenPrepStatement();

		// WHEN
		final var sql = UpdateMergeServiceProperties.common2Column(tableName, pk, MergeMode.Simple,
				sprops);
		final PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		then(stmt).should().setString(1, JsonUtils.getJSONString(sprops));
		then(stmt).should().setObject(2, pk.getUserId());
		then(stmt).should().setObject(3, pk.getEntityId());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).is(matching(
				equalToTextResource("merge-sprops-01.sql", TestSqlResources.class, SQL_COMMENT)));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_common2_recursive() throws SQLException {
		// GIVEN
		final String tableName = "my_table";
		final UserStringCompositePK pk = new UserStringCompositePK(randomLong(), randomString());
		final Map<String, Object> sprops = Map.of("foo", "bar");

		givenPrepStatement();

		// WHEN
		final var sql = UpdateMergeServiceProperties.common2Column(tableName, pk,
				MergeMode.RecursiveObjects, sprops);
		final PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		then(stmt).should().setString(1, JsonUtils.getJSONString(sprops));
		then(stmt).should().setBoolean(2, false);
		then(stmt).should().setObject(3, pk.getUserId());
		then(stmt).should().setObject(4, pk.getEntityId());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).is(matching(
				equalToTextResource("merge-sprops-02.sql", TestSqlResources.class, SQL_COMMENT)));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_common2_recursiveArrays() throws SQLException {
		// GIVEN
		final String tableName = "my_table";
		final UserStringCompositePK pk = new UserStringCompositePK(randomLong(), randomString());
		final Map<String, Object> sprops = Map.of("foo", "bar");

		givenPrepStatement();

		// WHEN
		final var sql = UpdateMergeServiceProperties.common2Column(tableName, pk,
				MergeMode.RecursiveObjectsAndArrays, sprops);
		final PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		then(stmt).should().setString(1, JsonUtils.getJSONString(sprops));
		then(stmt).should().setBoolean(2, true);
		then(stmt).should().setObject(3, pk.getUserId());
		then(stmt).should().setObject(4, pk.getEntityId());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).is(matching(
				equalToTextResource("merge-sprops-02.sql", TestSqlResources.class, SQL_COMMENT)));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_simple() throws SQLException {
		// GIVEN
		final String tableName = "my_table";
		final String[] pkColNames = new String[] { "pk1", "pk2" };
		final String spropColName = "x_props";
		final UserStringCompositePK pk = new UserStringCompositePK(randomLong(), randomString());
		final Map<String, Object> sprops = Map.of("foo", "bar");

		givenPrepStatement();

		// WHEN
		final var sql = new UpdateMergeServiceProperties(tableName, idx -> pkColNames[idx], spropColName,
				pk, MergeMode.Simple, sprops);
		final PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		then(stmt).should().setString(1, JsonUtils.getJSONString(sprops));
		then(stmt).should().setObject(2, pk.getUserId());
		then(stmt).should().setObject(3, pk.getEntityId());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).is(matching(
				equalToTextResource("merge-sprops-03.sql", TestSqlResources.class, SQL_COMMENT)));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_recursive() throws SQLException {
		// GIVEN
		final String tableName = "my_table";
		final String[] pkColNames = new String[] { "pk1", "pk2" };
		final String spropColName = "x_props";
		final UserStringCompositePK pk = new UserStringCompositePK(randomLong(), randomString());
		final Map<String, Object> sprops = Map.of("foo", "bar");

		givenPrepStatement();

		// WHEN
		final var sql = new UpdateMergeServiceProperties(tableName, idx -> pkColNames[idx], spropColName,
				pk, MergeMode.RecursiveObjects, sprops);
		final PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		then(stmt).should().setString(1, JsonUtils.getJSONString(sprops));
		then(stmt).should().setBoolean(2, false);
		then(stmt).should().setObject(3, pk.getUserId());
		then(stmt).should().setObject(4, pk.getEntityId());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).is(matching(
				equalToTextResource("merge-sprops-04.sql", TestSqlResources.class, SQL_COMMENT)));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_recursiveArrays() throws SQLException {
		// GIVEN
		final String tableName = "my_table";
		final String[] pkColNames = new String[] { "pk1", "pk2" };
		final String spropColName = "x_props";
		final UserStringCompositePK pk = new UserStringCompositePK(randomLong(), randomString());
		final Map<String, Object> sprops = Map.of("foo", "bar");

		givenPrepStatement();

		// WHEN
		final var sql = new UpdateMergeServiceProperties(tableName, idx -> pkColNames[idx], spropColName,
				pk, MergeMode.RecursiveObjectsAndArrays, sprops);
		final PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		then(stmt).should().setString(1, JsonUtils.getJSONString(sprops));
		then(stmt).should().setBoolean(2, true);
		then(stmt).should().setObject(3, pk.getUserId());
		then(stmt).should().setObject(4, pk.getEntityId());

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue()).is(matching(
				equalToTextResource("merge-sprops-04.sql", TestSqlResources.class, SQL_COMMENT)));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
