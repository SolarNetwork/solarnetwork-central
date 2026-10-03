/* ==================================================================
 * CommonJdbcUtilsTests.java - 3/10/2026 3:14:11 pm
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

import static org.assertj.core.api.BDDAssertions.then;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;

/**
 * Test cases for the {@link CommonJdbcUtils} class.
 *
 * @author matt
 * @version 1.0
 */
public class CommonJdbcUtilsTests {

	// @formatter:off
	@ParameterizedTest
	@CsvSource({
		"08006, true",  // connection failure
		"40001, true",  // serialization failure
		"40P01, true",  // deadlock detected
		"53300, true",  // too many connections
		"55P03, true",  // lock not available
		"57014, true",  // query canceled, such as by a statement timeout
		"22003, false", // numeric value out of range
		"23505, false", // unique violation
		"42501, false", // insufficient privilege
		"55000, false", // object not in prerequisite state
	})
	// @formatter:on
	public void isTransientException_sqlState(String sqlState, boolean expected) {
		// GIVEN
		SQLException e = new SQLException("Test", sqlState);

		// WHEN
		boolean result = CommonJdbcUtils.isTransientException(e);

		// THEN
		// @formatter:off
		then(result)
			.as("Transient for SQL state %s", sqlState)
			.isEqualTo(expected)
			;
		// @formatter:on
	}

	@Test
	public void isTransientException_transientType() {
		// GIVEN
		SQLException e = new SQLTimeoutException("Test");

		// WHEN
		boolean result = CommonJdbcUtils.isTransientException(e);

		// THEN
		// @formatter:off
		then(result)
			.as("A SQLTransientException subclass is transient")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void isTransientException_recoverableType() {
		// GIVEN
		SQLException e = new SQLRecoverableException("Test");

		// WHEN
		boolean result = CommonJdbcUtils.isTransientException(e);

		// THEN
		// @formatter:off
		then(result)
			.as("A SQLRecoverableException is transient")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void isTransientException_noSqlState() {
		// GIVEN
		SQLException e = new SQLException("Test");

		// WHEN
		boolean result = CommonJdbcUtils.isTransientException(e);

		// THEN
		// @formatter:off
		then(result)
			.as("Not transient without a SQL state")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void isTransientException_notSqlException() {
		// GIVEN
		RuntimeException e = new IllegalArgumentException("Test");

		// WHEN
		boolean result = CommonJdbcUtils.isTransientException(e);

		// THEN
		// @formatter:off
		then(result)
			.as("Not transient when not a SQLException")
			.isFalse()
			;
		// @formatter:on
	}

}
