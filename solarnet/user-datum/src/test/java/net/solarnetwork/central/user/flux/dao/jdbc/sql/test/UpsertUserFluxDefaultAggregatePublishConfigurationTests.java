/* ==================================================================
 * UpsertUserFluxDefaultAggregatePublishConfigurationTests.java - 25/06/2024 11:10:18 am
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
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.UpsertUserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;

/**
 * Test cases for the {@link UpsertUserFluxDefaultAggregatePublishConfiguration}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class UpsertUserFluxDefaultAggregatePublishConfigurationTests {

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

	private void thenPrepStatement(PreparedStatement result, Long userId,
			UserFluxDefaultAggregatePublishConfiguration conf) throws SQLException {
		int p = 0;
		then(result).should().setTimestamp(eq(++p), any());
		then(result).should().setTimestamp(eq(++p), any());
		then(result).should().setObject(++p, userId);
		then(result).should().setBoolean(++p, conf.isPublish());
		then(result).should().setBoolean(++p, conf.isRetain());
	}

	@Test
	public void insert() throws SQLException {
		// GIVEN
		final Long userId = randomLong();
		final var conf = new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.now());

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpsertUserFluxDefaultAggregatePublishConfiguration(userId, conf)
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture());

		thenPrepStatement(result, userId, conf);
		
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(new HamcrestCondition<>(equalToTextResource("upsert-default-conf.sql", getClass(), SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

}
