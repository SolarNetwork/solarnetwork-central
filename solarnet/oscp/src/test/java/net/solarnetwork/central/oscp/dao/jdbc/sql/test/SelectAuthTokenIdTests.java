/* ==================================================================
 * SelectAuthTokenIdTests.java - 16/08/2022 6:00:24 pm
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

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectAuthTokenId;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Test cases for the {@link SelectAuthTokenId} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectAuthTokenIdTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void thenPrepStatement(PreparedStatement result, String token, boolean oauth)
			throws SQLException {
		then(result).should().setString(1, token);
		then(result).should().setBoolean(2, oauth);
	}

	@Test
	public void selectFlexibilityProvider_sql() {
		// GIVEN
		String token = randomUUID().toString();

		// WHEN
		String sql = new SelectAuthTokenId(OscpRole.FlexibilityProvider, token, false).getSql();

		// THEN
		assertThat("SQL generated", sql,
				is(equalTo("SELECT user_id, id FROM solaroscp.fp_id_for_token(?,?)")));
	}

	@Test
	public void selectFlexibilityProvider_prep() throws SQLException {
		// GIVEN
		String token = randomUUID().toString();
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectAuthTokenId(OscpRole.FlexibilityProvider, token, false)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				is(equalTo("SELECT user_id, id FROM solaroscp.fp_id_for_token(?,?)")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, token, false);
	}

	@Test
	public void selectCapacityProvider() {
		// GIVEN
		String token = randomUUID().toString();

		// WHEN
		String sql = new SelectAuthTokenId(OscpRole.CapacityProvider, token, false).getSql();

		// THEN
		assertThat("SQL generated", sql,
				is(equalTo("SELECT user_id, id FROM solaroscp.cp_id_for_token(?,?)")));
	}

	@Test
	public void selectCapacityOptimizer() {
		// GIVEN
		String token = randomUUID().toString();

		// WHEN
		String sql = new SelectAuthTokenId(OscpRole.CapacityOptimizer, token, false).getSql();

		// THEN
		assertThat("SQL generated", sql,
				is(equalTo("SELECT user_id, id FROM solaroscp.co_id_for_token(?,?)")));
	}

}
