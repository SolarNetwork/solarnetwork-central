/* ==================================================================
 * SelectAuditDatumTests.java - 20/11/2020 12:05:56 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectAuditDatum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link SelectAuditDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectAuditDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static DatumProperties testProps() {
		return propertiesOf(decimalArray("1.1", "1.2", "1.3", "1.4"),
				decimalArray("2.1", "2.2", "2.3", "2.4"), new String[] { "a", "b" },
				new String[] { "c" });
	}

	@Test
	public void sql_hour_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-datum-hour-users.sql", TestSqlResources.class));
	}

	@Test
	public void sql_day_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-datum-day-users.sql", TestSqlResources.class));
	}

	@Test
	public void sql_month_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-datum-month-users.sql", TestSqlResources.class));
	}
}
