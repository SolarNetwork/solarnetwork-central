/* ==================================================================
 * DeleteForCompositeKeyTests.java - 6/08/2023 7:21:35 am
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

package net.solarnetwork.central.common.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Test cases for the {@link DeleteForCompositeKey} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DeleteForCompositeKeyTests {

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

	private void verifyPrepStatement(PreparedStatement stmt, CompositeKey pk) throws SQLException {
		int p = 0;
		for ( int i = 0; i < pk.keyComponentLength(); i++ ) {
			if ( pk.keyComponentIsAssigned(i) ) {
				verify(stmt).setObject(++p, pk.keyComponent(i));
			}
		}
	}

	@Test
	public void keyRequired() {
		assertThatExceptionOfType(IllegalArgumentException.class).as("A key instance is required")
				.isThrownBy(() -> {
					new DeleteForCompositeKey(null, "", new String[0]);
				});
	}

	@Test
	public void tableNameRequired() {
		assertThatExceptionOfType(IllegalArgumentException.class).as("A table name is required")
				.isThrownBy(() -> {
					new DeleteForCompositeKey(new UserLongCompositePK(1L, 2L), null, new String[0]);
				});
	}

	@Test
	public void columnNamesRequired() {
		assertThatExceptionOfType(IllegalArgumentException.class).as("Column names are required")
				.isThrownBy(() -> {
					new DeleteForCompositeKey(new UserLongCompositePK(1L, 2L), "t", null);
				});
		assertThatExceptionOfType(IllegalArgumentException.class).as("Column names must not be empty")
				.isThrownBy(() -> {
					new DeleteForCompositeKey(new UserLongCompositePK(1L, 2L), "t", new String[0]);
				});
	}

	@Test
	public void missingColumnName() {
		assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
				.as("Insufficient column names triggers bounds exception").isThrownBy(() -> {
					DeleteForCompositeKey sql = new DeleteForCompositeKey(
							new UserLongCompositePK(1L, 2L), "t", new String[] { "a" });
					sql.getSql();
				});
	}

	@Test
	public void delete() throws Exception {
		// GIVEN
		UserLongCompositePK pk = new UserLongCompositePK(1L, 2L);
		givenPrepStatement();

		// WHEN
		String tableName = "t";
		String[] columnNames = new String[] { "a", "b" };
		DeleteForCompositeKey sql = new DeleteForCompositeKey(pk, tableName, columnNames);
		PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sql.getSql()).as("SQL generated")
				.is(matching(equalToTextResource("delete-for-composite-key-unique.sql",
						TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, pk);
	}

	@Test
	public void delete_firstComponentOnly() throws Exception {
		// GIVEN
		UserLongCompositePK pk = UserLongCompositePK.unassignedEntityIdKey(1L);
		givenPrepStatement();

		// WHEN
		String tableName = "t";
		String[] columnNames = new String[] { "a" };
		DeleteForCompositeKey sql = new DeleteForCompositeKey(pk, tableName, columnNames);
		PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sql.getSql()).as("SQL generated")
				.is(matching(equalToTextResource("delete-for-composite-key-group.sql",
						TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, pk);
	}

}
