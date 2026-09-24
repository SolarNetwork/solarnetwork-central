/* ==================================================================
 * SelectSolarNodeMetadataTests.java - 23/09/2026 6:45:00 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.ClassUtils.getResourceAsString;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectSolarNodeMetadata;

/**
 * Test cases for the {@link SelectSolarNodeMetadata} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectSolarNodeMetadataTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array nodeIdsArray;

	@Mock
	private Array tokenIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Test
	public void nodes_sql() {
		// GIVEN
		var filter = new BasicCoreCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });

		// WHEN
		String sql = new SelectSolarNodeMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		// @formatter:off
		and.then(sql)
			.as("Metadata is not restricted without token criteria")
			.isEqualToIgnoringWhitespace(getResourceAsString(
					"select-node-meta-nodes.sql", TestSqlResources.class, SQL_COMMENT))
			;
		// @formatter:on
	}

	@Test
	public void nodesAndToken_sql() {
		// GIVEN
		var filter = new BasicCoreCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setTokenIds(new String[] { randomString(), randomString() });

		// WHEN
		String sql = new SelectSolarNodeMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		// @formatter:off
		and.then(sql)
			.as("Metadata is restricted to the token policy's node metadata paths")
			.isEqualToIgnoringWhitespace(getResourceAsString(
					"select-node-meta-nodes-token.sql", TestSqlResources.class, SQL_COMMENT))
			;
		// @formatter:on
	}

	@Test
	public void token_sql() {
		// GIVEN
		var filter = new BasicCoreCriteria();
		filter.setTokenId(randomString());

		// WHEN
		String sql = new SelectSolarNodeMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		// @formatter:off
		and.then(sql)
			.as("Metadata is restricted to the token policy's node metadata paths")
			.isEqualToIgnoringWhitespace(getResourceAsString(
					"select-node-meta-token.sql", TestSqlResources.class, SQL_COMMENT))
			;
		// @formatter:on
	}

	@Test
	public void token_sql_count() {
		// GIVEN
		var filter = new BasicCoreCriteria();
		filter.setTokenId(randomString());

		// WHEN
		String sql = ((SqlProvider) new SelectSolarNodeMetadata(filter).countPreparedStatementCreator())
				.getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		// @formatter:off
		and.then(sql)
			.as("Count query restricts the results the same way, so totals agree with the rows")
			.contains("INNER JOIN solaruser.user_auth_token_login t")
			.contains("IS NOT NULL")
			;
		// @formatter:on
	}

	/**
	 * The {@code user_auth_token_login} view exposes the token secret as a
	 * {@code password} column, which must never appear in metadata queries.
	 */
	@Test
	public void token_sql_neverSelectsPassword() {
		// GIVEN
		var filter = new BasicCoreCriteria();
		filter.setNodeIds(new Long[] { 1L });
		filter.setTokenId(randomString());

		// WHEN
		var sql = new SelectSolarNodeMetadata(filter);
		String selectSql = sql.getSql();
		String countSql = ((SqlProvider) sql.countPreparedStatementCreator()).getSql();

		// THEN
		// @formatter:off
		and.then(selectSql)
			.as("Token secret is never selected")
			.doesNotContain("password")
			.doesNotContain("auth_secret")
			;
		and.then(countSql)
			.as("Token secret is never selected by the count query")
			.doesNotContain("password")
			.doesNotContain("auth_secret")
			;
		// @formatter:on
	}

	@Test
	public void nodesAndToken_prep() throws SQLException {
		// GIVEN
		final Long[] nodeIds = new Long[] { 1L, 2L };
		final String tokenId = randomString();

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
		given(con.createArrayOf(eq("bigint"), aryEq(nodeIds))).willReturn(nodeIdsArray);

		var filter = new BasicCoreCriteria();
		filter.setNodeIds(nodeIds);
		filter.setTokenId(tokenId);

		// WHEN
		PreparedStatement result = new SelectSolarNodeMetadata(filter).createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		then(stmt).should().setArray(1, nodeIdsArray);
		then(stmt).should().setString(2, tokenId);
		then(nodeIdsArray).should().free();

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.isEqualToIgnoringWhitespace(getResourceAsString(
					"select-node-meta-nodes-token.sql", TestSqlResources.class, SQL_COMMENT))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

}
