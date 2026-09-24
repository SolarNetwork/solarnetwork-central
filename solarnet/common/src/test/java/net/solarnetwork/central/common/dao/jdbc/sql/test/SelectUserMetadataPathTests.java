/* ==================================================================
 * SelectUserMetadataPathTests.java - 24/09/2026 12:05:00 pm
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
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import net.solarnetwork.central.common.dao.jdbc.sql.SelectUserMetadataPath;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;

/**
 * Test cases for the {@link SelectUserMetadataPath} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectUserMetadataPathTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	@Test
	public void user_sql() {
		// GIVEN
		var filter = new BasicUserMetadataFilter();
		filter.setUserId(randomLong());

		// WHEN
		String sql = new SelectUserMetadataPath(filter, "/m/foo").getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		// @formatter:off
		and.then(sql)
			.as("Metadata is not restricted without token criteria")
			.is(matching(equalToTextResource("select-user-meta-path.sql", TestSqlResources.class,
					SQL_COMMENT)))
			;
		// @formatter:on
	}

	@Test
	public void userAndToken_sql() {
		// GIVEN
		var filter = new BasicUserMetadataFilter();
		filter.setUserId(randomLong());
		filter.setTokenId(randomString());

		// WHEN
		String sql = new SelectUserMetadataPath(filter, "/m/foo").getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		// @formatter:off
		and.then(sql)
			.as("The path is extracted from the metadata restricted to the token policy's paths")
			.is(matching(equalToTextResource("select-user-meta-path-token.sql", TestSqlResources.class,
					SQL_COMMENT)))
			;
		// @formatter:on
	}

	/**
	 * Only a single token criteria is supported, so extra token IDs are ignored
	 * rather than widening the query.
	 */
	@Test
	public void userAndTokens_sql_onlyFirstTokenUsed() {
		// GIVEN
		var filter = new BasicUserMetadataFilter();
		filter.setUserId(randomLong());
		filter.setTokenIds(new String[] { randomString(), randomString() });

		// WHEN
		String sql = new SelectUserMetadataPath(filter, "/m/foo").getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		// @formatter:off
		and.then(sql)
			.as("Only one token ID is matched, the same as for a single token criteria")
			.is(matching(equalToTextResource("select-user-meta-path-token.sql", TestSqlResources.class,
					SQL_COMMENT)))
			;
		// @formatter:on
	}

	/**
	 * User metadata joins the token view directly on the user ID. A join via
	 * {@code solaruser.user_node} would return nothing for a user who owns no
	 * nodes, and duplicate rows for a user who owns several.
	 */
	@Test
	public void token_sql_doesNotJoinUserNode() {
		// GIVEN
		var filter = new BasicUserMetadataFilter();
		filter.setUserId(randomLong());
		filter.setTokenId(randomString());

		// WHEN
		String sql = new SelectUserMetadataPath(filter, "/m/foo").getSql();

		// THEN
		// @formatter:off
		and.then(sql)
			.as("The token policy is reached directly from the user ID, not through the node tables")
			.doesNotContain("user_node")
			.contains("INNER JOIN solaruser.user_auth_token_login t ON t.user_id = um.user_id")
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
		var filter = new BasicUserMetadataFilter();
		filter.setUserId(randomLong());
		filter.setTokenId(randomString());

		// WHEN
		String sql = new SelectUserMetadataPath(filter, "/m/foo").getSql();

		// THEN
		// @formatter:off
		and.then(sql)
			.as("Token secret is never selected")
			.doesNotContain("password")
			.doesNotContain("auth_secret")
			;
		// @formatter:on
	}

	@Test
	public void userAndTokens_prep_onlyFirstTokenBound() throws SQLException {
		// GIVEN
		final Long userId = randomLong();
		final String path = "/m/foo";
		final String[] tokenIds = new String[] { randomString(), randomString() };

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		var filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		filter.setTokenIds(tokenIds);

		// WHEN
		PreparedStatement result = new SelectUserMetadataPath(filter, path)
				.createPreparedStatement(con);

		// THEN
		// @formatter:off
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		then(stmt).should().setString(1, path);
		then(stmt).should().setObject(2, userId);
		then(stmt).should().setString(3, tokenIds[0]);
		then(stmt).shouldHaveNoMoreInteractions();

		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sqlCaptor.getValue())
			.as("Generated SQL")
			.is(matching(equalToTextResource("select-user-meta-path-token.sql",
					TestSqlResources.class, SQL_COMMENT)))
			;
		and.then(result)
			.as("Connection statement returned")
			.isSameAs(stmt)
			;
		// @formatter:on
	}

}
