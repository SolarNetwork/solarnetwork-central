/* ==================================================================
 * SelectChargePointStatusTests.java - 17/11/2022 9:07:01 am
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

package net.solarnetwork.central.ocpp.dao.jdbc.sql.test;

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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusFilter;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.SelectChargePointStatus;

/**
 * Test cases for the {@link SelectChargePointStatus} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectChargePointStatusTests {

	private static final int TEST_FETCH_SIZE = 567;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array userIdsArray;

	@Mock
	private Array chargePointIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenSetUserIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(userIdsArray);
	}

	private void givenSetChargePointIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(chargePointIdsArray);
	}

	private void thenPrepStatement(PreparedStatement result, ChargePointStatusFilter filter)
			throws SQLException {
		int p = 0;
		if ( filter.hasUserCriteria() ) {
			if ( filter.getUserIds().length == 1 ) {
				then(result).should().setObject(++p, filter.getUserId());
			} else {
				then(result).should().setArray(++p, userIdsArray);
			}
		}
		if ( filter.hasChargePointCriteria() ) {
			if ( filter.getChargePointIds().length == 1 ) {
				then(result).should().setObject(++p, filter.getChargePointId());
			} else {
				then(result).should().setArray(++p, chargePointIdsArray);
			}
		}
		if ( filter.hasStartDate() ) {
			then(result).should().setTimestamp(++p, Timestamp.from(filter.getStartDate()));
		}
		if ( filter.hasEndDate() ) {
			then(result).should().setTimestamp(++p, Timestamp.from(filter.getEndDate()));
		}
		if ( filter.getMax() != null ) {
			then(result).should().setInt(++p, filter.getMax());
		}
		if ( filter.getOffset() != null && filter.getOffset() > 0 ) {
			then(result).should().setInt(++p, filter.getOffset());
		}
	}

	@Test
	public void find_userAndChargePoint_sql() {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(1L);
		filter.setChargePointId(2L);

		// WHEN
		String sql = new SelectChargePointStatus(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-cpstatus-user-cp.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void find_userAndChargePoint_prep() throws SQLException {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(1L);
		filter.setChargePointId(2L);

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectChargePointStatus(filter, TEST_FETCH_SIZE)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-cpstatus-user-cp.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, filter);
	}

	@Test
	public void find_userAndChargePoints_sql() {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(1L);
		filter.setChargePointIds(new Long[] { 2L, 3L });

		// WHEN
		String sql = new SelectChargePointStatus(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-cpstatus-user-cps.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void find_userAndChargePoints_prep() throws SQLException {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(1L);
		filter.setChargePointIds(new Long[] { 2L, 3L });

		givenPrepStatement();
		givenSetChargePointIdsArrayParameter(filter.getChargePointIds());

		// WHEN
		PreparedStatement result = new SelectChargePointStatus(filter, TEST_FETCH_SIZE)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-cpstatus-user-cps.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, filter);
	}

	@Test
	public void find_usersAndChargePoints_sql() {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setChargePointIds(new Long[] { 2L, 3L });

		// WHEN
		String sql = new SelectChargePointStatus(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-cpstatus-users-cps.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void find_usersAndChargePoints_prep() throws SQLException {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setChargePointIds(new Long[] { 2L, 3L });

		givenPrepStatement();
		givenSetUserIdsArrayParameter(filter.getUserIds());
		givenSetChargePointIdsArrayParameter(filter.getChargePointIds());

		// WHEN
		PreparedStatement result = new SelectChargePointStatus(filter, TEST_FETCH_SIZE)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-cpstatus-users-cps.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, filter);
	}

	@Test
	public void find_userAndChargePoints_dateRange_sql() {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(1L);
		filter.setChargePointIds(new Long[] { 2L, 3L });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(Instant.now().truncatedTo(ChronoUnit.SECONDS));

		// WHEN
		String sql = new SelectChargePointStatus(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-cpstatus-user-cps-dateRange.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void find_userAndChargePoints_dateRange_prep() throws SQLException {
		// GIVEN
		BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(1L);
		filter.setChargePointIds(new Long[] { 2L, 3L });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(Instant.now().truncatedTo(ChronoUnit.SECONDS));

		givenPrepStatement();
		givenSetChargePointIdsArrayParameter(filter.getChargePointIds());

		// WHEN
		PreparedStatement result = new SelectChargePointStatus(filter, TEST_FETCH_SIZE)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-cpstatus-user-cps-dateRange.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, filter);
	}

}
