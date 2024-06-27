/* ==================================================================
 * DeleteForIdTests.java - 25/06/2024 10:54:48 am
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

package net.solarnetwork.central.common.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.HamcrestCondition.matching;
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
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForId;

/**
 * Test cases for the {@link DeleteForId} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DeleteForIdTests {

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

	private void thenPrepStatement(PreparedStatement stmt, Object pk) throws SQLException {
		then(stmt).should().setObject(1, pk);
	}

	@Test
	public void keyRequired() {
		assertThatExceptionOfType(IllegalArgumentException.class).as("A key instance is required")
				.isThrownBy(() -> {
					new DeleteForId(null, "", "");
				});
	}

	@Test
	public void tableNameRequired() {
		assertThatExceptionOfType(IllegalArgumentException.class).as("A table name is required")
				.isThrownBy(() -> {
					new DeleteForId(1L, null, "");
				});
	}

	@Test
	public void columnNamesRequired() {
		assertThatExceptionOfType(IllegalArgumentException.class).as("Column names are required")
				.isThrownBy(() -> {
					new DeleteForId(1L, "t", null);
				});
	}

	@Test
	public void delete() throws Exception {
		// GIVEN
		Long pk = 1L;
		givenPrepStatement();

		// WHEN
		String tableName = "t";
		String columnName = "a";
		DeleteForId sql = new DeleteForId(pk, tableName, columnName);
		PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		thenPrepStatement(result, pk);
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		and.then(sql.getSql()).as("SQL generated").is(
				matching(equalToTextResource("delete-for-id.sql", TestSqlResources.class, SQL_COMMENT)));
		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
