/* ==================================================================
 * MyBatisGeneralNodeDatumDaoTests.java - Nov 14, 2014 6:21:15 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Iterator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for the {@link MyBatisGeneralNodeDatumDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisGeneralNodeDatumDaoTests extends MyBatisGeneralNodeDatumDaoTestSupport {

	@Test
	public void storeNew() {
		GeneralNodeDatum datum = getTestInstance();
		GeneralNodeDatumPK id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	@Test
	public void findDatumAtLocalNoDifference() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts", m.getSampleData().get("watts"), equalTo((Object) 231));
		assertThat("Watt hours", m.getSampleData().get("watt_hours"), equalTo((Object) 4123));
	}

	@Test
	public void findDatumAtLocalExactDateStart() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts, TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts", m.getSampleData().get("watts"), equalTo((Object) 231));
		assertThat("Watt hours", m.getSampleData().get("watt_hours"), equalTo((Object) 4123));
	}

	@Test
	public void findDatumAtLocalExactDateEnd() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts, TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts", m.getSampleData().get("watts"), equalTo((Object) 345));
		assertThat("Watt hours", m.getSampleData().get("watt_hours"), equalTo((Object) 4445));
	}

	@Test
	public void findDatumAtLocalNoDataInRange() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusDays(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusDays(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumAtLocalOnlyDataBefore() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusSeconds(90), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.minusSeconds(60), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumAtLocalOnlyDataAfter() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.plusSeconds(60), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusSeconds(90), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumAtLocalEvenDifference() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 4284));
	}

	@Test
	public void findDatumAtLocalSixtyFortyDifference() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusSeconds(40), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusSeconds(20), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", (BigDecimal) m.getSampleData().get("watt_hours"),
				closeTo(new BigDecimal("4337.666"), new BigDecimal("0.001")));
	}

	@Test
	public void findDatumAtSixtyFortyDifference() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusSeconds(40), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusSeconds(20), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter, ts,
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", (BigDecimal) m.getSampleData().get("watt_hours"),
				closeTo(new BigDecimal("4337.666"), new BigDecimal("0.001")));
	}

	@Test
	public void findDatumAtLocalMultipleNodesDifferentTimeZones() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusSeconds(40), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusSeconds(20), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		final DateTimeZone tz2 = DateTimeZone.forID("America/Los_Angeles");
		final Long nodeId2 = -19889L;
		setupTestLocation(-9889L, tz2.getID());
		setupTestNode(nodeId2, -9889L);

		DateTime ts2 = new DateTime(2018, 8, 1, 0, 0, 0, tz2);
		GeneralNodeDatum d3 = getTestInstance(ts2.minusSeconds(20), nodeId2, TEST_SOURCE_ID);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusSeconds(40), nodeId2, TEST_SOURCE_ID);
		d4.getSamples().putInstantaneousSampleValue("watts", 482);
		d4.getSamples().putAccumulatingSampleValue("watt_hours", 8344);
		dao.store(d3);
		dao.store(d4);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, nodeId2 });
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts2.getZone()), equalTo(ts2));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(nodeId2));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"),
				equalTo((Object) new BigDecimal("356.5")));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 5530));

		m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", (BigDecimal) m.getSampleData().get("watt_hours"),
				closeTo(new BigDecimal("4337.666"), new BigDecimal("0.001")));
	}

	@Test
	public void findDatumBetweenLocal() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d3.getSamples().putInstantaneousSampleValue("watts", 462);
		d3.getSamples().putAccumulatingSampleValue("watt_hours", 8044);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d4.getSamples().putInstantaneousSampleValue("watts", 482);
		d4.getSamples().putAccumulatingSampleValue("watt_hours", 8344);
		dao.store(d3);
		dao.store(d4);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 380));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 3910));
	}

	@Test
	public void findDatumBetween() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d3.getSamples().putInstantaneousSampleValue("watts", 462);
		d3.getSamples().putAccumulatingSampleValue("watt_hours", 8044);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d4.getSamples().putInstantaneousSampleValue("watts", 482);
		d4.getSamples().putAccumulatingSampleValue("watt_hours", 8344);
		dao.store(d3);
		dao.store(d4);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter, ts, ts2,
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 380));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 3910));
	}

	@Test
	public void findDatumBetweenLocalNoData() {
		// given

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumBetweenLocalOnlyStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 0));
	}

	@Test
	public void findDatumBetweenLocalOnlyEnd() {
		// given
		DateTime ts = new DateTime(2018, 9, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 0));
	}
}
