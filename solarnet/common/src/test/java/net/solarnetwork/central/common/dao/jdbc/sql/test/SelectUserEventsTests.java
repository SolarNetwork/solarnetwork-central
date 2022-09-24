/* ==================================================================
 * SelectUserEventsTests.java - 3/08/2022 9:21:35 am
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

package net.solarnetwork.central.common.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.support.UuidUtils.createUuidV7Boundary;
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
import net.solarnetwork.central.common.dao.BasicUserEventFilter;
import net.solarnetwork.central.common.dao.UserEventFilter;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectUserEvent;
import net.solarnetwork.central.support.SearchFilterUtils;
import net.solarnetwork.util.SearchFilter;

/**
 * Test cases for the {@link SelectUserEvent} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectUserEventsTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array userIdsArray;

	@Mock
	private Array tagsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenSetUserIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(userIdsArray);
	}

	private void givenSetTagsArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(tagsArray);
	}

	private void verifyPrepStatement(PreparedStatement result, UserEventFilter filter)
			throws SQLException {
		int p = 0;
		if ( filter.hasUserCriteria() ) {
			if ( filter.getUserIds().length == 1 ) {
				then(result).should().setObject(++p, filter.getUserId());
			} else {
				then(result).should().setArray(++p, userIdsArray);
			}
		}
		if ( filter.hasTagCriteria() ) {
			then(result).should().setArray(++p, tagsArray);
		}
		if ( filter.hasSearchFilterCriteria() ) {
			then(result).should().setString(++p, SearchFilterUtils
					.toSqlJsonPath(SearchFilter.forLDAPSearchFilterString(filter.getSearchFilter())));
		}
		if ( filter.hasStartDate() ) {
			then(result).should().setObject(++p, createUuidV7Boundary(filter.getStartDate()));
		}
		if ( filter.hasEndDate() ) {
			then(result).should().setObject(++p, createUuidV7Boundary(filter.getEndDate()));
		}
		if ( filter.getMax() != null ) {
			then(result).should().setInt(++p, filter.getMax());
		}
		if ( filter.getOffset() != null && filter.getOffset() > 0 ) {
			then(result).should().setInt(++p, filter.getOffset());
		}
	}

	@Test
	public void sql() {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserId(1L);
		filter.setTag("A");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		// WHEN
		String sql = new SelectUserEvent(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-user-event.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep() throws SQLException {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserId(1L);
		filter.setTag("A");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		givenPrepStatement();
		givenSetTagsArrayParameter(filter.getTags());

		// WHEN
		PreparedStatement result = new SelectUserEvent(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-user-event.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verifyPrepStatement(result, filter);
	}

	@Test
	public void sql_page() {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserId(1L);
		filter.setTag("A");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		filter.setMax(100);
		filter.setOffset(200);

		// WHEN
		String sql = new SelectUserEvent(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-user-event-page.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_page() throws SQLException {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserId(1L);
		filter.setTag("A");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		filter.setMax(100);
		filter.setOffset(200);

		givenPrepStatement();
		givenSetTagsArrayParameter(filter.getTags());

		// WHEN
		PreparedStatement result = new SelectUserEvent(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-user-event-page.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verifyPrepStatement(result, filter);
	}

	@Test
	public void sql_multiUsers() {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setTags(new String[] { "A", "B" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		// WHEN
		String sql = new SelectUserEvent(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-user-event-multiusers.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_multiUsers() throws SQLException {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setTags(new String[] { "A", "B" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		givenPrepStatement();
		givenSetUserIdsArrayParameter(filter.getUserIds());
		givenSetTagsArrayParameter(filter.getTags());

		// WHEN
		PreparedStatement result = new SelectUserEvent(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-user-event-multiusers.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verifyPrepStatement(result, filter);
	}

	@Test
	public void sql_searchFilter() {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserIds(new Long[] { 1L });
		filter.setTags(new String[] { "A", "B" });
		filter.setSearchFilter("(foo=bar)");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		// WHEN
		String sql = new SelectUserEvent(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-user-event-searchfilter.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_searchFilter() throws SQLException {
		// GIVEN
		BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserIds(new Long[] { 1L });
		filter.setTags(new String[] { "A", "B" });
		filter.setSearchFilter("(foo=bar)");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		givenPrepStatement();
		givenSetTagsArrayParameter(filter.getTags());

		// WHEN
		PreparedStatement result = new SelectUserEvent(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-user-event-searchfilter.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verifyPrepStatement(result, filter);
	}

}
