/* ==================================================================
 * AuditUserServiceValueRowMapperTests.java - 29/05/2024 4:06:05 pm
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.dao.jdbc.AuditUserServiceValueRowMapper;
import net.solarnetwork.central.dao.AuditUserServiceEntity;
import net.solarnetwork.central.domain.AuditUserServiceValue;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumId;

/**
 * Test cases for the {@link AuditUserServiceValueRowMapper} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class AuditUserServiceValueRowMapperTests {

	@Mock
	private ResultSet resultSet;

	private void givenRowMap(int i, AuditUserServiceValue expected) throws SQLException {
		given(resultSet.getTimestamp(1)).willReturn(Timestamp.from(expected.getTimestamp()));
		given(resultSet.getObject(2)).willReturn(expected.getUserId());
		given(resultSet.getString(3)).willReturn(expected.getService());
		given(resultSet.getString(4)).willReturn(expected.getAggregation().getKey());
		given(resultSet.getLong(5)).willReturn(expected.getCount());
	}

	@Test
	public void mapRow() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final String service = UUID.randomUUID().toString().substring(0, 4);
		final Instant ts = Instant.now();
		final long count = UUID.randomUUID().getMostSignificantBits();
		final AuditUserServiceEntity expected = new AuditUserServiceEntity(
				DatumId.nodeId(userId, service, ts), Aggregation.Hour, count);
		givenRowMap(0, expected);

		// WHEN
		AuditUserServiceValue result = AuditUserServiceValueRowMapper.INSTANCE.mapRow(resultSet, 0);

		// THEN
		assertThat("Result has same values", expected.isSameAs(result), is(true));
	}

}
