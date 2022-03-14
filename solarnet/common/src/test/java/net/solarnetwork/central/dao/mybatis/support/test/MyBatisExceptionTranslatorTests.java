/* ==================================================================
 * MyBatisExceptionTranslatorTests.java - 29/06/2020 12:22:57 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.Test;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import net.solarnetwork.central.dao.mybatis.support.MyBatisExceptionTranslator;

/**
 * Test cases for the {@link MyBatisExceptionTranslator} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisExceptionTranslatorTests {

	private DataSource dataSource;

	@Test
	public void translateConflictWithRecovery() {
		// GIVEN
		MyBatisExceptionTranslator et = new MyBatisExceptionTranslator(dataSource, true);

		// WHEN
		SQLException sqlEx = new SQLException("Error: canceling statement due to conflict with recovery",
				"XX000");
		DataAccessException dae = et.translateExceptionIfPossible(new RuntimeException(sqlEx));

		// THEN
		assertThat("ConcurrencyFailureException created", dae,
				instanceOf(ConcurrencyFailureException.class));
	}

}
