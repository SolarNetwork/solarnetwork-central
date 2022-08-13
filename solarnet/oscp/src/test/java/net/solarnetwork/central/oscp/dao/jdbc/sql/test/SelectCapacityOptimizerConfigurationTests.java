/* ==================================================================
 * SelectCapacityOptimizerConfigurationTests.java - 12/08/2022 5:02:05 pm
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

package net.solarnetwork.central.oscp.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectCapacityOptimizerConfiguration;

/**
 * Test cases for the {@link SelectCapacityOptimizerConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectCapacityOptimizerConfigurationTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array userIdsArray;

	@Mock
	private Array configurationIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenSetUserIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(userIdsArray);
	}

	private void givenSetConfigurationIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(configurationIdsArray);
	}

	private void thenPrepStatement(PreparedStatement result, ConfigurationFilter filter)
			throws SQLException {
		int p = 0;
		if ( filter.hasUserCriteria() ) {
			if ( filter.getUserIds().length == 1 ) {
				then(result).should().setObject(++p, filter.getUserId());
			} else {
				then(result).should().setArray(++p, userIdsArray);
			}
		}
		if ( filter.hasConfigurationCriteria() ) {
			if ( filter.getConfigurationIds().length == 1 ) {
				then(result).should().setObject(++p, filter.getConfigurationId());
			} else {
				then(result).should().setArray(++p, configurationIdsArray);
			}
		}
		if ( filter.getMax() != null ) {
			then(result).should().setInt(++p, filter.getMax());
		}
		if ( filter.getOffset() != null && filter.getOffset() > 0 ) {
			then(result).should().setInt(++p, filter.getOffset());
		}
	}

	@Test
	public void multi_sql() {
		// GIVEN
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setConfigurationIds(new Long[] { 3L, 4L });

		// WHEN
		String sql = new SelectCapacityOptimizerConfiguration(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-capacity-optimizer-conf.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void multi_prep() throws SQLException {
		// GIVEN
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setConfigurationIds(new Long[] { 3L, 4L });

		givenPrepStatement();
		givenSetUserIdsArrayParameter(filter.getUserIds());
		givenSetConfigurationIdsArrayParameter(filter.getConfigurationIds());

		// WHEN
		PreparedStatement result = new SelectCapacityOptimizerConfiguration(filter)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-capacity-optimizer-conf.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, filter);
	}

	@Test
	public void single_sql() {
		// GIVEN
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(1L);
		filter.setConfigurationId(2L);

		// WHEN
		String sql = new SelectCapacityOptimizerConfiguration(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-capacity-optimizer-conf-one.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void single_prep() throws SQLException {
		// GIVEN
		BasicConfigurationFilter filter = new BasicConfigurationFilter();
		filter.setUserId(1L);
		filter.setConfigurationId(2L);

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectCapacityOptimizerConfiguration(filter)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-capacity-optimizer-conf-one.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, filter);
	}
}
