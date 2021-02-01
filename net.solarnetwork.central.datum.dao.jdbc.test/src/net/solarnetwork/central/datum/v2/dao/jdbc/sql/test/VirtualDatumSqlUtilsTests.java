/* ==================================================================
 * VirtualDatumSqlUtilsTests.java - 8/12/2020 5:18:16 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.VirtualDatumSqlUtils;
import net.solarnetwork.util.ClassUtils;

/**
 * Test cases for the {@link VirtualDatumSqlUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class VirtualDatumSqlUtilsTests {

	@Test
	public void combiningClause_average() {
		assertThat("Combining SQL",
				VirtualDatumSqlUtils.combineCalculationSql(CombiningType.Average, "foo"),
				equalTo("AVG(foo)"));
	}

	@Test
	public void combiningClause_difference() {
		assertThat("Combining SQL",
				VirtualDatumSqlUtils.combineCalculationSql(CombiningType.Difference, "foo"),
				equalTo("SUM(CASE prank WHEN 1 THEN foo ELSE -foo END ORDER BY prank)"));
	}

	@Test
	public void combiningClause_difference_customOrder() {
		assertThat("Combining SQL",
				VirtualDatumSqlUtils.combineCalculationSql(CombiningType.Difference, "foo", "bar"),
				equalTo("SUM(CASE bar WHEN 1 THEN foo ELSE -foo END ORDER BY bar)"));
	}

	@Test
	public void combiningClause_sum() {
		assertThat("Combining SQL", VirtualDatumSqlUtils.combineCalculationSql(CombiningType.Sum, "foo"),
				equalTo("SUM(foo)"));
	}

	@Test
	public void combiningCteSql_sum() {
		// GIVEN
		String sql = VirtualDatumSqlUtils.combineCteSql(CombiningType.Sum);

		// THEN
		assertThat("SQL templates resolved", sql,
				equalTo(ClassUtils.getResourceAsString("datum-combine-cte-sum.sql", getClass(),
						DatumSqlUtils.SQL_COMMENT)));
	}

}
