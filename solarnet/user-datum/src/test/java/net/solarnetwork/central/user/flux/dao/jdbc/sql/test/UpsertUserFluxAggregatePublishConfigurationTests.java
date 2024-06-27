/* ==================================================================
 * UpsertUserFluxAggregatePublishConfigurationTests.java - 25/06/2024 11:10:18 am
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
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
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
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.flux.dao.jdbc.sql.UpsertUserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;

/**
 * Test cases for the {@link UpsertUserFluxAggregatePublishConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class UpsertUserFluxAggregatePublishConfigurationTests {

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
		given(con.prepareStatement(any(), eq(Statement.RETURN_GENERATED_KEYS))).willReturn(stmt);
	}

	private void givenSetNodeIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(nodeIdsArray);
	}

	private void givenSetSourceIdsArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(sourceIdsArray);
	}

	private void thenPrepStatement(PreparedStatement result, UserLongCompositePK pk,
			UserFluxAggregatePublishConfiguration conf) throws SQLException {
		int p = 0;
		then(result).should().setTimestamp(eq(++p), any());
		then(result).should().setTimestamp(eq(++p), any());
		then(result).should().setObject(++p, pk.getUserId());
		if ( pk.entityIdIsAssigned() ) {
			then(result).should().setObject(++p, pk.getEntityId());
		}
		then(result).should().setArray(++p, nodeIdsArray);
		then(result).should().setArray(++p, sourceIdsArray);
		then(result).should().setBoolean(++p, conf.isPublish());
		then(result).should().setBoolean(++p, conf.isRetain());
	}

	@Test
	public void insert() throws SQLException {
		// GIVEN
		final UserLongCompositePK pk = UserLongCompositePK.unassignedEntityIdKey(randomLong());
		final var conf = new UserFluxAggregatePublishConfiguration(pk, Instant.now());
		conf.setNodeIds(new Long[] { 1L, 2L });
		conf.setSourceIds(new String[] { "a", "b" });

		givenPrepStatement();
		givenSetNodeIdsArrayParameter(conf.getNodeIds());
		givenSetSourceIdsArrayParameter(conf.getSourceIds());

		// WHEN
		PreparedStatement result = new UpsertUserFluxAggregatePublishConfiguration(pk.getUserId(), conf)
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS));

		thenPrepStatement(result, pk, conf);
		
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(new HamcrestCondition<>(equalToTextResource("upsert-insert-conf.sql", getClass(), SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

	@Test
	public void update() throws SQLException {
		// GIVEN
		final UserLongCompositePK pk = new UserLongCompositePK(randomLong(), randomLong());
		final var conf = new UserFluxAggregatePublishConfiguration(pk, Instant.now());
		conf.setNodeIds(new Long[] { 1L, 2L });
		conf.setSourceIds(new String[] { "a", "b" });

		givenPrepStatement();
		givenSetNodeIdsArrayParameter(conf.getNodeIds());
		givenSetSourceIdsArrayParameter(conf.getSourceIds());

		// WHEN
		PreparedStatement result = new UpsertUserFluxAggregatePublishConfiguration(pk.getUserId(), conf)
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS));

		thenPrepStatement(result, pk, conf);
		
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(new HamcrestCondition<>(equalToTextResource("upsert-update-conf.sql", getClass(), SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

}
