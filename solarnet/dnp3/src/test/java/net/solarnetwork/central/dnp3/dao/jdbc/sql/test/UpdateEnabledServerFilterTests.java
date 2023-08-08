/* ==================================================================
 * UpdateEnabledServerFilterTests.java - 8/08/2023 8:46:45 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpdateEnabledServerFilter;

/**
 * Test cases for the {@link UpdateEnabledServerFilter} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpdateEnabledServerFilterTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array serverIdsArray;

	@Mock
	private Array identifiersArray;

	@Mock
	private Array indexesArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void givenSetIdentifiersArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(identifiersArray);
	}

	private void givenSetIndexesArrayParameter(Integer[] value) throws SQLException {
		given(con.createArrayOf(eq("integer"), aryEq(value))).willReturn(indexesArray);
	}

	private void verifyPrepStatement(PreparedStatement result, Long userId, BasicFilter filter,
			boolean enabled) throws SQLException {
		int p = 0;
		verify(result).setBoolean(++p, enabled);
		verify(result).setObject(++p, userId);
		if ( filter.hasServerCriteria() ) {
			if ( filter.getServerIds().length == 1 ) {
				verify(result).setObject(++p, filter.getServerId());
			} else {
				verify(result).setArray(++p, serverIdsArray);
			}
		}
		if ( filter.hasIdentifierCriteria() ) {
			if ( filter.getIdentifiers().length == 1 ) {
				verify(result).setObject(++p, filter.getIdentifier());
			} else {
				verify(result).setArray(++p, identifiersArray);
			}
		}
		if ( filter.hasIndexCriteria() ) {
			if ( filter.getIndexes().length == 1 ) {
				verify(result).setObject(++p, filter.getIndex());
			} else {
				verify(result).setArray(++p, indexesArray);
			}
		}
		if ( filter.hasEnabledCriteria() ) {
			verify(result).setBoolean(++p, filter.getEnabled().booleanValue());
		}
	}

	@Test
	public void server_prep() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final boolean enabled = true;
		BasicFilter filter = new BasicFilter();
		filter.setEnabled(false);

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateEnabledServerFilter("solardnp3.dnp3_server", "id", userId,
				filter, enabled).createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sqlCaptor.getValue()).as("SQL generated").is(matching(
				equalToTextResource("update-server-enabled.sql", TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, userId, filter, enabled);
	}

	@Test
	public void serverAuth_prep() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final boolean enabled = true;
		BasicFilter filter = new BasicFilter();
		filter.setServerId(UUID.randomUUID().getMostSignificantBits());
		filter.setIdentifiers(new String[] { "a", "b" });
		filter.setEnabled(false);

		givenPrepStatement();
		givenSetIdentifiersArrayParameter(filter.getIdentifiers());

		// WHEN
		PreparedStatement result = new UpdateEnabledServerFilter("solardnp3.dnp3_server_auth",
				"server_id", userId, filter, enabled).createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sqlCaptor.getValue()).as("SQL generated")
				.is(matching(equalToTextResource("update-server-auth-enabled.sql",
						TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, userId, filter, enabled);
	}

	@Test
	public void serverControl_prep() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final boolean enabled = true;
		BasicFilter filter = new BasicFilter();
		filter.setServerId(UUID.randomUUID().getMostSignificantBits());
		filter.setIndexes(new Integer[] { 1, 2, 3 });
		filter.setEnabled(false);

		givenPrepStatement();
		givenSetIndexesArrayParameter(filter.getIndexes());

		// WHEN
		PreparedStatement result = new UpdateEnabledServerFilter("solardnp3.dnp3_server_ctrl",
				"server_id", userId, filter, enabled).createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sqlCaptor.getValue()).as("SQL generated")
				.is(matching(equalToTextResource("update-server-control-enabled.sql",
						TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, userId, filter, enabled);
	}

}
