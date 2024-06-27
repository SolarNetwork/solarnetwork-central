/* ==================================================================
 * SelectUserFluxAggregatePublishConfigurationTests.java - 24/06/2024 4:05:08 pm
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

package net.solarnetwork.central.user.flux.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
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
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.dao.BasicFluxConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.UserFluxAggregatePublishConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.SelectUserFluxAggregatePublishConfiguration;

/**
 * Test cases for the {@link SelectUserFluxAggregatePublishConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectUserFluxAggregatePublishConfigurationTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array nodeIdsArray;

	@Mock
	private Array sourceIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenPrepKeyStatement() throws SQLException {
		givenPrepStatement();
		stmt.setFetchSize(1);
	}

	private void givenPrepFilterStatement(int fetchSize) throws SQLException {
		givenPrepStatement();
		stmt.setFetchSize(fetchSize);
	}

	private void givenSetNodeIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(nodeIdsArray);
	}

	private void givenSetSourceIdsArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(sourceIdsArray);
	}

	private void thenPrepStatement(PreparedStatement result, UserLongCompositePK key)
			throws SQLException {
		int p = 0;
		then(result).should().setObject(++p, key.getUserId());
		then(result).should().setObject(++p, key.getEntityId());
	}

	private void thenPrepStatement(PreparedStatement result,
			UserFluxAggregatePublishConfigurationFilter filter) throws SQLException {
		int p = 0;
		if ( filter.hasUserCriteria() ) {
			// assuming 1 user
			then(result).should().setObject(++p, filter.getUserId());
		}
		if ( filter.hasNodeCriteria() ) {
			then(result).should().setArray(++p, nodeIdsArray);
		}
		if ( filter.hasSourceCriteria() ) {
			then(result).should().setArray(++p, sourceIdsArray);
		}
	}

	@Test
	public void select_forKey() throws SQLException {
		// GIVEN
		UserLongCompositePK key = new UserLongCompositePK(randomLong(), randomLong());

		givenPrepKeyStatement();

		// WHEN
		PreparedStatement result = new SelectUserFluxAggregatePublishConfiguration(key)
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));

		thenPrepStatement(result, key);
		
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(new HamcrestCondition<>(equalToTextResource("select-conf-for-key.sql", getClass(), SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

	@Test
	public void select_forFilter() throws SQLException {
		// GIVEN
		BasicFluxConfigurationFilter filter = new BasicFluxConfigurationFilter();
		filter.setUserId(randomLong());
		filter.setNodeIds(new Long[] { randomLong(), randomLong() });
		filter.setSourceIds(new String[] { randomString(), randomString() });

		givenPrepFilterStatement(SelectUserFluxAggregatePublishConfiguration.DEFAULT_FETCH_SIZE);
		givenSetNodeIdsArrayParameter(filter.getNodeIds());
		givenSetSourceIdsArrayParameter(filter.getSourceIds());

		// WHEN
		PreparedStatement result = new SelectUserFluxAggregatePublishConfiguration(filter)
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));

		thenPrepStatement(result, filter);
		
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(new HamcrestCondition<>(equalToTextResource("select-conf-for-filter-01.sql", getClass(), SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

}
