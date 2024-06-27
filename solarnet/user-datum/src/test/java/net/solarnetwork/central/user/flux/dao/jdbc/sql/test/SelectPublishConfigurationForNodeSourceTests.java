/* ==================================================================
 * SelectPublishConfigurationForNodeSourceTests.java - 24/06/2024 8:53:12 am
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.SelectPublishConfigurationForNodeSource;

/**
 * Test cases for the {@link SelectPublishConfigurationForNodeSource} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectPublishConfigurationForNodeSourceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void thenPrepStatement(PreparedStatement result, UserLongStringCompositePK key)
			throws SQLException {
		int p = 0;
		then(result).should().setObject(++p, key.getUserId());
		then(result).should().setObject(++p, key.getGroupId());
		then(result).should().setString(++p, key.getEntityId());
	}

	@Test
	public void select() throws SQLException {
		// GIVEN
		UserLongStringCompositePK key = new UserLongStringCompositePK(randomLong(), randomLong(),
				randomString());

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectPublishConfigurationForNodeSource(key)
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));

		thenPrepStatement(result, key);
		
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(new HamcrestCondition<>(equalToTextResource("select-settings-for-user-node-source.sql", getClass(), SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

	@Test
	public void select_staticCreate() throws SQLException {
		// GIVEN
		UserLongStringCompositePK key = new UserLongStringCompositePK(randomLong(), randomLong(),
				randomString());

		givenPrepStatement();

		// WHEN
		PreparedStatement result = SelectPublishConfigurationForNodeSource
				.forUserNodeSource(key.getUserId(), key.getGroupId(), key.getEntityId())
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));

		thenPrepStatement(result, key);
		
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(new HamcrestCondition<>(equalToTextResource("select-settings-for-user-node-source.sql", getClass(), SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

}
