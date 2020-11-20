/* ==================================================================
 * SelectAccumulativeAuditDatumTests.java - 20/11/2020 4:20:29 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectAccumulativeAuditDatum;

/**
 * Test cases for the {@link SelectAccumulativeAuditDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectAccumulativeAuditDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_day_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectAccumulativeAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-acc-datum-day-users.sql", TestSqlResources.class));
	}

	@Test
	public void sql_mostRecent_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(1L);
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectAccumulativeAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-audit-acc-datum-mostRecent-users.sql",
				TestSqlResources.class));
	}

}
