/* ==================================================================
 * SelectAppSettingTests.java - 10/11/2021 10:04:24 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import net.solarnetwork.central.common.dao.jdbc.sql.SelectAppSetting;

/**
 * Test cases for the {@link SelectAppSetting} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectAppSettingTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array keysArray;

	@Mock
	private Array typesArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenSetStringArrayParameter(int p, String[] value, Array array) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(array);
		stmt.setArray(p, array);
		array.free();

	}

	@Test
	public void all_sql() {
		// WHEN
		String sql = new SelectAppSetting(null, null).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-app-setting-all.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void all_prep() throws SQLException {
		// GIVEN
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectAppSetting(null, null).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-app-setting-all.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void key_sql() {
		// WHEN
		String sql = SelectAppSetting.selectForKey("foo").getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-app-setting-key.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void key_prep() throws SQLException {
		// GIVEN
		final String key = "foo";
		givenPrepStatement();
		stmt.setObject(1, key);

		// WHEN
		PreparedStatement result = SelectAppSetting.selectForKey(key).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-app-setting-key.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void keyAndType_sql() {
		// WHEN
		String sql = SelectAppSetting.selectForKeyType("foo", "bar").getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-app-setting-key-and-type.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void keyAndType_prep() throws SQLException {
		// GIVEN
		final String key = "foo";
		final String type = "bar";
		givenPrepStatement();
		stmt.setObject(1, key);
		stmt.setObject(2, type);

		// WHEN
		PreparedStatement result = SelectAppSetting.selectForKeyType(key, type)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-app-setting-key-and-type.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void keysAndTypes_sql() {
		// WHEN
		String sql = new SelectAppSetting(new String[] { "k1", "k2" }, new String[] { "t1", "t2" })
				.getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-app-setting-keys-and-types.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void keysAndTypes_prep() throws SQLException {
		// GIVEN
		final String[] keys = new String[] { "k1", "k2" };
		final String[] types = new String[] { "t1", "t2" };
		givenPrepStatement();
		givenSetStringArrayParameter(1, keys, keysArray);
		givenSetStringArrayParameter(2, types, typesArray);

		// WHEN
		PreparedStatement result = new SelectAppSetting(keys, types).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-app-setting-keys-and-types.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

}
