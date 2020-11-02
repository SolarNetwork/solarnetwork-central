/* ==================================================================
 * DbDatumIngestSideEffectTests.java - 3/11/2020 10:52:40 am
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.ingestDatumStream;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.loadJsonDatumResource;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;

/**
 * Test DB functions for datum ingest, that populate "stale" records and update
 * audit records.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumIngestSideEffectTests extends BaseDatumJdbcTestSupport {

	@Test
	public void firstDatum() throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		log.debug("Got test data: {}", datums);
		ingestDatumStream(log, jdbcTemplate, singleton(datums.get(0)));

		// should have inserted "stale" agg row for datum hour
		List<Map<String, Object>> stale = DatumTestUtils.staleAggregateDatumStreams(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", stale, hasSize(1));
	}

}
