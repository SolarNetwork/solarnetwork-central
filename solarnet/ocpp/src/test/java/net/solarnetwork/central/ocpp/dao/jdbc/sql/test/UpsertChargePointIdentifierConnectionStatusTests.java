/* ==================================================================
 * UpsertChargePointIdentifierConnectionStatusTests.java - 17/11/2022 11:23:43 am
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
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.UpsertChargePointIdentifierConnectionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;

/**
 * Test casees for the {@link UpsertChargePointIdentifierConnectionStatus}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpsertChargePointIdentifierConnectionStatusTests {

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

	private void thenPrepStatement(PreparedStatement result, Long userId, String chargePointIdentifier,
			ChargePointStatus status) throws SQLException {
		int p = 0;
		then(result).should().setString(++p, status.getConnectedTo());
		if ( status.getConnectedDate() != null ) {
			then(result).should().setTimestamp(++p, Timestamp.from(status.getConnectedDate()));
		}
		then(result).should().setObject(++p, userId);
		then(result).should().setString(++p, chargePointIdentifier);
	}

	@Test
	public void connected_sql() {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final String cpId = UUID.randomUUID().toString();
		final String instanceId = UUID.randomUUID().toString();
		final Instant connDate = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final ChargePointStatus status = new ChargePointStatus(unassignedEntityIdKey(userId),
				Instant.now(), instanceId, connDate);

		// WHEN
		String sql = new UpsertChargePointIdentifierConnectionStatus(userId, cpId, status).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("upsert-cpstatus-ident-date.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void connected_prep() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final String cpIdent = UUID.randomUUID().toString();
		final String instanceId = UUID.randomUUID().toString();
		final Instant connDate = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final ChargePointStatus status = new ChargePointStatus(unassignedEntityIdKey(userId),
				Instant.now(), instanceId, connDate);

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpsertChargePointIdentifierConnectionStatus(userId, cpIdent,
				status).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"upsert-cpstatus-ident-date.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, userId, cpIdent, status);
	}

	@Test
	public void disconnected_sql() {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final String cpId = UUID.randomUUID().toString();
		final ChargePointStatus status = new ChargePointStatus(unassignedEntityIdKey(userId),
				Instant.now(), null, null);

		// WHEN
		String sql = new UpsertChargePointIdentifierConnectionStatus(userId, cpId, status).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("upsert-cpstatus-ident.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void disconnected_prep() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final String cpIdent = UUID.randomUUID().toString();
		final ChargePointStatus status = new ChargePointStatus(unassignedEntityIdKey(userId),
				Instant.now(), null, null);

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpsertChargePointIdentifierConnectionStatus(userId, cpIdent,
				status).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("upsert-cpstatus-ident.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, userId, cpIdent, status);
	}

}
