/* ==================================================================
 * JdbcDatumAuxiliaryEntityDao_FilterableDaoTests.java - 19/11/2020 5:24:58 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStreamWithAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertDatumAuxiliary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link FilterableDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumAuxiliaryEntityDao_FilterableDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumAuxiliaryEntityDao dao;

	@Before
	public void setup() {
		dao = new JdbcDatumAuxiliaryEntityDao(jdbcTemplate);
	}

	@Test
	public void find_nodesAndSourcesAndType_absoluteDates() throws IOException {
		// GIVEN
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStreamWithAuxiliary(log,
				jdbcTemplate, "test-datum-17.txt", getClass(), "UTC");
		UUID streamId = metas.values().iterator().next().getStreamId();

		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusDays(1).toInstant());
		filter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);

		// WHEN
		FilterResults<DatumAuxiliary, DatumAuxiliaryPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		DatumSamples f = new DatumSamples();
		f.putAccumulatingSampleValue("w", 41);
		DatumSamples s = new DatumSamples();
		s.putAccumulatingSampleValue("w", 100);
		assertDatumAuxiliary("Found match", results.iterator().next(), new DatumAuxiliaryEntity(streamId,
				start.plusHours(13).toInstant(), DatumAuxiliaryType.Reset, null, f, s, null, null));

	}

}
