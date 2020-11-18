/* ==================================================================
 * JdbcReadingDatumEntityDaoTests.java - 17/11/2020 7:28:08 pm
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcReadingDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcReadingDatumEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcReadingDatumEntityDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcReadingDatumEntityDao dao;

	private Map<NodeSourcePK, NodeDatumStreamMetadata> loadStreamWithAuxiliary(String resource) {
		List<?> data;
		try {
			data = DatumTestUtils.loadJsonDatumAndAuxiliaryResource(resource, getClass());
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				DatumTestUtils.insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
		}
		return meta;
	}

	private FilterResults<ReadingDatum, DatumPK> execute(ReadingDatumCriteria filter) {
		FilterResults<ReadingDatum, DatumPK> results = dao.findDatumReadingFiltered(filter);
		if ( results.getReturnedResultCount() > 0 ) {
			log.debug("Got {} ReadingDatum results:\n{}", results.getReturnedResultCount(),
					stream(results.spliterator(), false).map(Object::toString).collect(joining("\n")));
		}
		return results;
	}

	private static void assertReading(String prefix, ReadingDatum result, ReadingDatum expected) {
		DatumTestUtils.assertAggregateDatum(prefix, result, expected);
		assertThat(prefix + " end timestamp", result.getEndTimestamp(),
				equalTo(expected.getEndTimestamp()));
	}

	@Before
	public void setup() {
		dao = new JdbcReadingDatumEntityDao(jdbcTemplate);
	}

	@Test
	public void diff_nodeAndSource_empty() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		FilterResults<ReadingDatum, DatumPK> results = dao.findDatumReadingFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(0L));
	}

	@Test
	public void diff_nodeAndSource() {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-02.txt").values().iterator().next()
				.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("Node and source", d,
				new ReadingDatumEntity(streamId, start.minusMinutes(1).toInstant(), null,
						start.plusHours(1).minusMinutes(1).toInstant(),
						propertiesOf(null, decimalArray("30"), null, null),
						statisticsOf(null, new BigDecimal[][] { decimalArray("30", "100", "130") })));
	}

}
