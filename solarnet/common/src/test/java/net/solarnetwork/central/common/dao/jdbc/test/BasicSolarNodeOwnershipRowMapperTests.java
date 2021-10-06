/* ==================================================================
 * BasicSolarNodeOwnershipRowMapperTests.java - 6/10/2021 11:39:09 AM
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.dao.jdbc.BasicSolarNodeOwnershipRowMapper;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * Test cases for the [@link BasicSolarNodeOwnershipRowMapper} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class BasicSolarNodeOwnershipRowMapperTests {

	@Mock
	private ResultSet resultSet;

	@Mock
	private ResultSetMetaData resultSetMeta;

	private void givenRowMap(int i, SolarNodeOwnership expected, int colCount) throws SQLException {
		given(resultSet.getMetaData()).willReturn(resultSetMeta);
		given(resultSetMeta.getColumnCount()).willReturn(colCount);
		given(resultSet.getObject(1)).willReturn(expected.getNodeId());
		given(resultSet.getObject(2)).willReturn(expected.getUserId());
		if ( colCount >= 3 ) {
			given(resultSet.getBoolean(3)).willReturn(expected.isRequiresAuthorization());
		}
		if ( colCount >= 4 ) {
			given(resultSet.getBoolean(4)).willReturn(expected.isArchived());
		}
	}

	@Test
	public void fullRow() throws SQLException {
		// GIVEN
		final BasicSolarNodeOwnership expected = new BasicSolarNodeOwnership(123L, 321L, true, true);
		givenRowMap(0, expected, 4);

		// WHEN
		SolarNodeOwnership result = BasicSolarNodeOwnershipRowMapper.INSTANCE.mapRow(resultSet, 0);

		// THEN
		assertThat("Result has same values", expected.isSameAs(result), is(true));
	}

	@Test
	public void shortRow() throws SQLException {
		// GIVEN
		final BasicSolarNodeOwnership expected = new BasicSolarNodeOwnership(123L, 321L, false, false);
		givenRowMap(0, expected, 2);

		// WHEN
		SolarNodeOwnership result = BasicSolarNodeOwnershipRowMapper.INSTANCE.mapRow(resultSet, 0);

		// THEN
		assertThat("Result has same values", expected.isSameAs(result), is(true));
	}

}
