/* ==================================================================
 * AggTestSupport.java - 5/07/2018 9:52:13 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.test.AbstractCentralTest;

/**
 * Test support for aggregate test cases.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration("classpath:/net/solarnetwork/central/test/test-tx-context.xml")
public abstract class AggTestSupport extends AbstractCentralTest {

	@Resource
	protected DataSource dataSource;

	@Resource
	protected PlatformTransactionManager txManager;

	protected JdbcTemplate jdbcTemplate;
	protected TransactionTemplate txTemplate;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected void setup() {
		assertThat("DataSource", dataSource, notNullValue());

		jdbcTemplate = new JdbcTemplate(dataSource);
		txTemplate = new TransactionTemplate(txManager);

		cleanupDatabase();
	}

	@After
	public void cleanupDatabase() {
		if ( jdbcTemplate == null ) {
			return;
		}
		jdbcTemplate.update("DELETE FROM solardatum.da_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_datum_hourly");
		jdbcTemplate.update("DELETE FROM solaragg.agg_datum_daily");
		jdbcTemplate.update("DELETE FROM solaragg.agg_datum_monthly");
		jdbcTemplate.update("DELETE FROM solaragg.aud_acc_datum_daily");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_hourly");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_daily");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_monthly");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_daily_stale");
		jdbcTemplate.update("DELETE FROM solarnet.sn_node WHERE node_id = ?", TEST_NODE_ID);
		jdbcTemplate.update("DELETE FROM solarnet.sn_loc WHERE id = ?", TEST_LOC_ID);
		jdbcTemplate.update("DELETE FROM solardatum.da_loc_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_loc_datum_hourly");
		jdbcTemplate.update("DELETE FROM solaragg.agg_loc_datum_daily");
		jdbcTemplate.update("DELETE FROM solaragg.agg_loc_datum_monthly");
	}

	protected void setupTestNode(Long nodeId, Long locationId) {
		jdbcTemplate.update("insert into solarnet.sn_node (node_id, loc_id) values (?,?)", nodeId,
				locationId);
	}

	protected void setupTestLocation(Long id, String timeZoneId) {
		jdbcTemplate.update(
				"insert into solarnet.sn_loc (id,country,region,postal_code,time_zone) values (?,?,?,?,?)",
				id, TEST_LOC_COUNTRY, TEST_LOC_REGION, TEST_LOC_POSTAL_CODE, timeZoneId);
	}

}
