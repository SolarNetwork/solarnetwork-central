/* ==================================================================
 * MyBatisGeneralNodeDatumAuxiliaryDaoTests.java - 4/02/2019 10:33:46 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumAuxiliaryDao;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Test cases for the {@link MyBatisGeneralNodeDatumAuxiliaryDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumAuxiliaryDaoTests extends MyBatisGeneralNodeDatumDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";

	private MyBatisGeneralNodeDatumAuxiliaryDao auxDao;

	private GeneralNodeDatumAuxiliary lastDatum;

	@Override
	@Before
	public void setup() {
		super.setup();
		auxDao = new MyBatisGeneralNodeDatumAuxiliaryDao();
		auxDao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private GeneralNodeDatumAuxiliary getTestAuxInstance() {
		return getTestAuxInstance(new DateTime(), TEST_NODE_ID, TEST_SOURCE_ID);
	}

	private GeneralNodeDatumAuxiliary getTestAuxInstance(DateTime created, Long nodeId,
			String sourceId) {
		GeneralNodeDatumAuxiliary datum = new GeneralNodeDatumAuxiliary();
		datum.setCreated(created);
		datum.setNodeId(nodeId);
		datum.setSourceId(sourceId);
		datum.setType(DatumAuxiliaryType.Reset);

		GeneralNodeDatumSamples samplesFinal = new GeneralNodeDatumSamples();
		samplesFinal.putAccumulatingSampleValue("watt_hours", 4123);
		datum.setSamplesFinal(samplesFinal);

		GeneralNodeDatumSamples samplesStart = new GeneralNodeDatumSamples();
		samplesStart.putAccumulatingSampleValue("watt_hours", 4321);
		datum.setSamplesStart(samplesStart);

		return datum;
	}

	@Test
	public void storeNew() {
		GeneralNodeDatumAuxiliary datum = getTestAuxInstance();
		GeneralNodeDatumPK id = auxDao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	@Test
	public void storeNewWithMetadata() {
		GeneralNodeDatumAuxiliary datum = getTestAuxInstance();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		datum.setMeta(meta);

		GeneralNodeDatumPK id = auxDao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	protected void verifyStaleDatumRow(String msg, Map<String, Object> s, DateTime date, Long nodeId,
			String sourceId) {
		assertThat(msg + " date", s.get("ts_start"), equalTo(new Timestamp(date.getMillis())));
		assertThat(msg + " node ID", s.get("node_id"), equalTo(nodeId));
		assertThat(msg + " source ID", s.get("source_id"), equalTo(sourceId));
	}

	@Test
	public void storeNewPopulatesStaleDatumRow() {
		// when
		storeNew();

		// then
		List<Map<String, Object>> stale = getStaleDatum(Aggregation.Hour);
		assertThat("Stale hourly records", stale, hasSize(1));
		verifyStaleDatumRow("stale", stale.get(0), lastDatum.getCreated().hourOfDay().roundFloorCopy(),
				TEST_NODE_ID, TEST_SOURCE_ID);
	}

	@Test
	public void movePopulatesStaleDatumRows() {
		// given
		storeNew();
		deleteStaleDatum();

		GeneralNodeDatumAuxiliary to = new GeneralNodeDatumAuxiliary(
				new GeneralNodeDatumAuxiliaryPK(lastDatum.getNodeId(),
						lastDatum.getCreated().plusHours(1), lastDatum.getSourceId()),
				lastDatum.getSamplesFinal(), lastDatum.getSamplesStart());

		// when
		boolean moved = auxDao.move(lastDatum.getId(), to);

		// then
		assertThat("Was moved", moved, equalTo(true));
		GeneralNodeDatumAuxiliary aux = auxDao.get(lastDatum.getId());
		assertThat("Datum was moved", aux, nullValue());

		List<Map<String, Object>> stale = getStaleDatum(Aggregation.Hour);
		assertThat("Stale hourly records", stale, hasSize(2));
		verifyStaleDatumRow("stale old", stale.get(0),
				lastDatum.getCreated().hourOfDay().roundFloorCopy(), TEST_NODE_ID, TEST_SOURCE_ID);
		verifyStaleDatumRow("stale new", stale.get(1), to.getCreated().hourOfDay().roundFloorCopy(),
				TEST_NODE_ID, TEST_SOURCE_ID);
	}

	@Test
	public void storeNewWithImmediateHistoryPopulatesTwoStaleDatumRows() {
		// given
		DateTime date = new DateTime(2019, 2, 12, 7, 20, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d = getTestInstance(date.minusMinutes(30), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d);
		deleteStaleDatum();

		// when
		GeneralNodeDatumAuxiliary aux = getTestAuxInstance(date, TEST_NODE_ID, TEST_SOURCE_ID);
		auxDao.store(aux);

		// then
		List<Map<String, Object>> stale = getStaleDatum(Aggregation.Hour);
		assertThat("Two stale hourly records for reset with immediate history", stale, hasSize(2));
		verifyStaleDatumRow("stale", stale.get(0), d.getCreated().hourOfDay().roundFloorCopy(),
				TEST_NODE_ID, TEST_SOURCE_ID);
		verifyStaleDatumRow("stale", stale.get(1), aux.getCreated().hourOfDay().roundFloorCopy(),
				TEST_NODE_ID, TEST_SOURCE_ID);
	}

	@Test
	public void storeNewWithDistantHistoryPopulatesStaleDatumRow() {
		// given
		GeneralNodeDatum d = getTestInstance(new DateTime().minusDays(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d);
		deleteStaleDatum();

		// when
		storeNew();

		// then
		List<Map<String, Object>> stale = getStaleDatum(Aggregation.Hour);
		assertThat("One stale hourly record for reset with distant history", stale, hasSize(1));
		verifyStaleDatumRow("stale", stale.get(0), lastDatum.getCreated().hourOfDay().roundFloorCopy(),
				TEST_NODE_ID, TEST_SOURCE_ID);
	}

	@Test
	public void storeNewInPastPopulatesTwoStaleDatumRows() {
		// given
		GeneralNodeDatum d = getTestInstance(new DateTime().plusMinutes(61), TEST_NODE_ID,
				TEST_SOURCE_ID);
		dao.store(d);
		deleteStaleDatum();

		// when
		storeNew();

		// then
		List<Map<String, Object>> stale = getStaleDatum(Aggregation.Hour);
		assertThat("Two stale hourly records for reset and future datum records", stale, hasSize(2));
		verifyStaleDatumRow("stale for reset", stale.get(0),
				lastDatum.getCreated().hourOfDay().roundFloorCopy(), TEST_NODE_ID, TEST_SOURCE_ID);
		verifyStaleDatumRow("stale for datum", stale.get(1), d.getCreated().hourOfDay().roundFloorCopy(),
				TEST_NODE_ID, TEST_SOURCE_ID);
	}

	private void validate(GeneralNodeDatumAuxiliary src, GeneralNodeDatumAuxiliary entity) {
		assertThat("GeneralNodeDatumAuxiliary should exist", entity, notNullValue());
		assertThat("Different instance", entity, not(sameInstance(src)));
		assertThat("Node ID", entity.getNodeId(), equalTo(src.getNodeId()));
		assertThat("Source ID", entity.getSourceId(), equalTo(src.getSourceId()));
		assertThat("Type", entity.getType(), equalTo(src.getType()));
		assertThat("Final samples accumulating", entity.getSamplesFinal().getA(),
				equalTo(src.getSamplesFinal().getA()));
		assertThat("Final samples tags", entity.getSamplesFinal().getT(),
				containsInAnyOrder("Reset", "final"));
		assertThat("Start samples accumulating", entity.getSamplesStart().getA(),
				equalTo(src.getSamplesStart().getA()));
		assertThat("Start samples tags", entity.getSamplesStart().getT(),
				containsInAnyOrder("Reset", "start"));
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GeneralNodeDatumAuxiliary datum = auxDao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void getByPrimaryKeyWithMetadata() {
		storeNewWithMetadata();
		GeneralNodeDatumAuxiliary datum = auxDao.get(lastDatum.getId());
		validate(lastDatum, datum);
		assertThat("Metadata exists", datum.getMeta(), notNullValue());
		assertThat("Metadata", datum.getMeta().getInfo(), Matchers.hasEntry("foo", "bar"));
	}

	@Test
	public void move() {
		storeNew();

		GeneralNodeDatumAuxiliary to = new GeneralNodeDatumAuxiliary(
				new GeneralNodeDatumAuxiliaryPK(lastDatum.getNodeId(),
						lastDatum.getCreated().minusHours(1), lastDatum.getSourceId()),
				lastDatum.getSamplesFinal(), lastDatum.getSamplesStart());

		boolean moved = auxDao.move(lastDatum.getId(), to);
		assertThat("Was moved", moved, equalTo(true));

		GeneralNodeDatumAuxiliary aux = auxDao.get(lastDatum.getId());
		assertThat("Datum was moved", aux, nullValue());

		aux = auxDao.get(to.getId());
		validate(lastDatum, aux);
	}

	@Test
	public void moveNothing() {
		storeNew();

		GeneralNodeDatumAuxiliary to = new GeneralNodeDatumAuxiliary(
				new GeneralNodeDatumAuxiliaryPK(lastDatum.getNodeId(),
						lastDatum.getCreated().minusHours(1), lastDatum.getSourceId()),
				lastDatum.getSamplesFinal(), lastDatum.getSamplesStart());

		GeneralNodeDatumAuxiliaryPK notOld = new GeneralNodeDatumAuxiliaryPK(lastDatum.getNodeId(),
				lastDatum.getCreated().minusHours(1), lastDatum.getSourceId());
		boolean moved = auxDao.move(notOld, to);
		assertThat("Was moved", moved, equalTo(false));

		GeneralNodeDatumAuxiliary aux = auxDao.get(lastDatum.getId());
		assertThat("Datum was not moved", aux, notNullValue());
		validate(lastDatum, aux);
	}

	@Test
	public void findFilteredDateRange() {
		DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		List<GeneralNodeDatumAuxiliaryPK> pks = new ArrayList<>(10);
		List<Integer> finalReadings = new ArrayList<>(10);
		List<Integer> startingReadings = new ArrayList<>(10);
		for ( int i = 0; i < 10; i++ ) {
			GeneralNodeDatumAuxiliary d = new GeneralNodeDatumAuxiliary();
			d.setCreated(start.plusMinutes(10 * i));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setType(DatumAuxiliaryType.Reset);

			GeneralNodeDatumSamples sf = new GeneralNodeDatumSamples();
			sf.putAccumulatingSampleValue("watt_hours", 100 * i);
			finalReadings.add(sf.getAccumulatingSampleInteger("watt_hours"));
			d.setSamplesFinal(sf);

			GeneralNodeDatumSamples ss = new GeneralNodeDatumSamples();
			ss.putAccumulatingSampleValue("watt_hours", 100 * i + 5);
			startingReadings.add(ss.getAccumulatingSampleInteger("watt_hours"));
			d.setSamplesStart(ss);

			GeneralNodeDatumAuxiliaryPK pk = auxDao.store(d);
			pks.add(pk);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(start.plusMinutes(60));

		FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> results = auxDao.findFiltered(criteria, null,
				null, null);

		final int expectedCount = 6;

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo((long) expectedCount));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(expectedCount));

		List<GeneralNodeDatumAuxiliaryFilterMatch> matches = StreamSupport
				.stream(results.spliterator(), false).collect(toList());
		assertThat("Matches count", matches, hasSize(expectedCount));

		assertThat("Matched IDs", matches.stream().map(m -> m.getId()).collect(toList()),
				contains(pks.stream().limit(expectedCount).toArray()));

		assertThat("Matched final readings",
				matches.stream().map(m -> m.getSampleDataFinal().get("watt_hours")).collect(toList()),
				contains(finalReadings.stream().limit(expectedCount).toArray()));

		assertThat("Matched starting readings",
				matches.stream().map(m -> m.getSampleDataStart().get("watt_hours")).collect(toList()),
				contains(startingReadings.stream().limit(expectedCount).toArray()));
	}
}
