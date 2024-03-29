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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.BDDMockito.given;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.sql.InsertAppSetting;
import net.solarnetwork.central.domain.AppSetting;

/**
 * Test cases for the {@link InsertAppSetting} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class InsertAppSettingTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
	}

	@Test
	public void new_sql() {
		// GIVEN
		AppSetting setting = new AppSetting("foo", "bar", null, Instant.now(), "test");

		// WHEN
		String sql = new InsertAppSetting(setting, false).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("insert-app-setting-new.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void new_prep() throws SQLException {
		// GIVEN
		AppSetting setting = new AppSetting("foo", "bar", null, Instant.now(), "test");

		// GIVEN
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new InsertAppSetting(setting, false).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("insert-app-setting-new.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void new_upsert_sql() {
		// GIVEN
		AppSetting setting = new AppSetting("foo", "bar", null, Instant.now(), "test");

		// WHEN
		String sql = new InsertAppSetting(setting, true).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("insert-app-setting-new-upsert.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void new_upsert_prep() throws SQLException {
		// GIVEN
		AppSetting setting = new AppSetting("foo", "bar", null, Instant.now(), "test");

		// GIVEN
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new InsertAppSetting(setting, true).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"insert-app-setting-new-upsert.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void new_upsert_withoutModified_sql() {
		// GIVEN
		AppSetting setting = AppSetting.appSetting("foo", "bar", "test");

		// WHEN
		String sql = new InsertAppSetting(setting, true).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("insert-app-setting-new-upsert-nomod.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void new_upsert_withoutModified_prep() throws SQLException {
		// GIVEN
		AppSetting setting = AppSetting.appSetting("foo", "bar", "test");

		// GIVEN
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new InsertAppSetting(setting, true).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"insert-app-setting-new-upsert-nomod.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

}
