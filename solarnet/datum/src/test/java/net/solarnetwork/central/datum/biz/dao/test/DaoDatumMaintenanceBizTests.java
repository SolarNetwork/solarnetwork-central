/* ==================================================================
 * DaoDatumMaintenanceBizTests.java - 26/11/2020 10:55:19 am
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

package net.solarnetwork.central.datum.biz.dao.test;

import static java.util.Collections.singleton;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.biz.dao.DaoDatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Test cases for the {@link DaoDatumMaintenanceBiz} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoDatumMaintenanceBizTests {

	private DatumMaintenanceDao datumDao;
	private DatumStreamMetadataDao metaDao;

	private DaoDatumMaintenanceBiz biz;

	private void replayAll() {
		replay(datumDao, metaDao);
	}

	private void verifyAll() {
		verify(datumDao, metaDao);
	}

	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumMaintenanceDao.class);
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);
		biz = new DaoDatumMaintenanceBiz(datumDao, metaDao);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	@Test
	public void findStale_nodeAndSource() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		List<net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum> daoStale = new ArrayList<>();
		StaleAggregateDatumEntity daoDatum = new StaleAggregateDatumEntity(streamId,
				Instant.now().truncatedTo(ChronoUnit.HOURS), Aggregation.Hour, Instant.now());
		daoStale.add(daoDatum);
		Capture<DatumStreamCriteria> filterCaptor = new Capture<>();
		BasicFilterResults<net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum, StreamKindPK> daoResults = new BasicFilterResults<>(
				daoStale);
		expect(datumDao.findStaleAggregateDatum(capture(filterCaptor))).andReturn(daoResults);

		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "America/New_York",
				ObjectDatumKind.Node, 1L, "a", null, null, null);

		Capture<ObjectStreamCriteria> metaFilterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(metaFilterCaptor))).andReturn(singleton(meta));

		// WHEN
		replayAll();
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(1L);
		criteria.setSourceId("a");
		FilterResults<StaleAggregateDatum> results = biz.findStaleAggregateDatum(criteria, null, null,
				null);

		// THEN
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));
		StaleAggregateDatum s = results.iterator().next();
		assertThat("Node ID returned from meta", s.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID returned from meta", s.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp returned from datum", s.getCreated(), equalTo(daoDatum.getTimestamp()));
		assertThat("Kind returned from datum", s.getKind(), equalTo(daoDatum.getKind().getKey()));

		DatumStreamCriteria f = filterCaptor.getValue();
		assertThat("Node IDs copied from criteria", f.getNodeIds(),
				arrayContaining(criteria.getNodeIds()));
		assertThat("Source IDs copied from criteria", f.getSourceIds(),
				arrayContaining(criteria.getSourceIds()));

		assertThat("Same criteria used to find datum and meta", filterCaptor.getValue(),
				sameInstance(metaFilterCaptor.getValue()));
	}

	@Test
	public void findStale_nodeAndSourceAndType() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		List<net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum> daoStale = new ArrayList<>();
		StaleAggregateDatumEntity daoDatum = new StaleAggregateDatumEntity(streamId,
				Instant.now().truncatedTo(ChronoUnit.HOURS), Aggregation.Hour, Instant.now());
		daoStale.add(daoDatum);
		Capture<DatumStreamCriteria> filterCaptor = new Capture<>();
		BasicFilterResults<net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum, StreamKindPK> daoResults = new BasicFilterResults<>(
				daoStale);
		expect(datumDao.findStaleAggregateDatum(capture(filterCaptor))).andReturn(daoResults);

		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "America/New_York",
				ObjectDatumKind.Node, 1L, "a", null, null, null);

		Capture<ObjectStreamCriteria> metaFilterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(metaFilterCaptor))).andReturn(singleton(meta));

		// WHEN
		replayAll();
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(1L);
		criteria.setSourceId("a");
		criteria.setType("m");
		FilterResults<StaleAggregateDatum> results = biz.findStaleAggregateDatum(criteria, null, null,
				null);

		// THEN
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));
		StaleAggregateDatum s = results.iterator().next();
		assertThat("Node ID returned from meta", s.getNodeId(), equalTo(meta.getObjectId()));
		assertThat("Source ID returned from meta", s.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp returned from datum", s.getCreated(), equalTo(daoDatum.getTimestamp()));
		assertThat("Kind returned from datum", s.getKind(), equalTo(daoDatum.getKind().getKey()));

		DatumStreamCriteria f = filterCaptor.getValue();
		assertThat("Node IDs copied from criteria", f.getNodeIds(),
				arrayContaining(criteria.getNodeIds()));
		assertThat("Source IDs copied from criteria", f.getSourceIds(),
				arrayContaining(criteria.getSourceIds()));
		assertThat("'m' kind copied from criteria as Month agg", f.getAggregation(),
				equalTo(Aggregation.Month));

		assertThat("Same criteria used to find datum and meta", filterCaptor.getValue(),
				sameInstance(metaFilterCaptor.getValue()));
	}
}
