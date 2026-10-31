/* ==================================================================
 * CommonSqlUtilsTests.java - 1/11/2026 8:03:27 am
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;

/**
 * Test cases for the {@link CommonSqlUtils} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CommonSqlUtilsTests {

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array array;

	@Test
	public void prepareOptimizedArrayParameter_null() throws SQLException {
		// WHEN
		final int result = prepareOptimizedArrayParameter(con, stmt, 1, (Long[]) null);

		// THEN
		then(stmt).shouldHaveNoInteractions();
		and.then(result).as("Parameter offset unchanged").isEqualTo(1);
	}

	@Test
	public void prepareOptimizedArrayParameter_empty() throws SQLException {
		// GIVEN
		final Long[] value = new Long[0];
		final StringBuilder where = new StringBuilder();

		// WHEN
		final int whereCount = whereOptimizedArrayContains(value, "foo", where);
		final int result = prepareOptimizedArrayParameter(con, stmt, 1, value);

		// THEN
		then(stmt).shouldHaveNoInteractions();
		// @formatter:off
		and.then(whereCount)
			.as("No SQL parameter generated")
			.isZero()
			;
		and.then(result)
			.as("Parameter offset unchanged, to match generated SQL")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareOptimizedArrayParameter_empty_setNull() throws SQLException {
		// WHEN
		final int result = prepareOptimizedArrayParameter(con, stmt, 1, "bigint", new Long[0], true);

		// THEN
		then(stmt).should().setNull(2, Types.ARRAY);
		and.then(result).as("Parameter offset incremented").isEqualTo(2);
	}

	@Test
	public void prepareOptimizedArrayParameter_single() throws SQLException {
		// GIVEN
		final Long value = randomLong();

		// WHEN
		final int result = prepareOptimizedArrayParameter(con, stmt, 1, new Long[] { value });

		// THEN
		then(stmt).should().setObject(2, value);
		and.then(result).as("Parameter offset incremented").isEqualTo(2);
	}

	@Test
	public void prepareOptimizedArrayParameter_multiple() throws SQLException {
		// GIVEN
		final Long[] value = new Long[] { randomLong(), randomLong() };
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(array);

		// WHEN
		final int result = prepareOptimizedArrayParameter(con, stmt, 1, value);

		// THEN
		then(stmt).should().setArray(2, array);
		then(array).should().free();
		and.then(result).as("Parameter offset incremented").isEqualTo(2);
	}

}
