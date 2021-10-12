/* ==================================================================
 * DaoDatumAuxiliaryBizTests.java - 29/11/2020 9:31:33 am
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
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumAuxiliaryBiz;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.NodeMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Test cases for the {@link DaoDatumAuxiliaryBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoDatumAuxiliaryBizTests {

	private static final Long TEST_NODE_ID = 1L;
	private static final String TEST_SOURCE_ID = "a";

	private DatumAuxiliaryEntityDao datumAuxiliaryDao;
	private DatumStreamMetadataDao metaDao;

	private DatumAuxiliaryBiz biz;

	private void replayAll() {
		replay(datumAuxiliaryDao, metaDao);
	}

	private void verifyAll() {
		verify(datumAuxiliaryDao, metaDao);
	}

	@Before
	public void setup() {
		datumAuxiliaryDao = EasyMock.createMock(DatumAuxiliaryEntityDao.class);
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);
		biz = new DaoDatumAuxiliaryBiz(datumAuxiliaryDao, metaDao);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private GeneralNodeDatumAuxiliary testGenAux() {
		DatumSamples sf = new DatumSamples();
		sf.putAccumulatingSampleValue("foo", 1);

		DatumSamples ss = new DatumSamples();
		ss.putAccumulatingSampleValue("foo", 10);

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "pow");
		//return new DatumAuxiliaryEntity(UUID.randomUUID(), Instant.now().truncatedTo(ChronoUnit.HOURS),
		//		DatumAuxiliaryType.Reset, null, sf, ss, "Note.", meta);

		GeneralNodeDatumAuxiliary genAux = new GeneralNodeDatumAuxiliary();
		genAux.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));
		genAux.setNodeId(TEST_NODE_ID);
		genAux.setSourceId(TEST_SOURCE_ID);
		genAux.setSamplesFinal(sf);
		genAux.setSamplesStart(ss);
		genAux.setMeta(meta);
		return genAux;
	}

	private DatumAuxiliaryEntity testAuxEntity(Instant date, UUID streamId) {
		DatumSamples sf = new DatumSamples();
		sf.putAccumulatingSampleValue("foo", 1);

		DatumSamples ss = new DatumSamples();
		ss.putAccumulatingSampleValue("foo", 10);

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("bim", "pow");
		return new DatumAuxiliaryEntity(streamId, date, DatumAuxiliaryType.Reset, null, sf, ss, "Note.",
				meta);
	}

	private void asssertCriteria(NodeMetadataCriteria metaCriteria) {
		asssertCriteria(metaCriteria, TEST_NODE_ID, TEST_SOURCE_ID);
	}

	private void asssertCriteria(NodeMetadataCriteria metaCriteria, Long nodeId, String sourceId) {
		assertThat("Criteria node ID", metaCriteria.getNodeId(), equalTo(nodeId));
		assertThat("Criteria source ID", metaCriteria.getSourceId(), equalTo(sourceId));
		assertThat("Criteria obj kind", metaCriteria.getObjectKind(), equalTo(ObjectDatumKind.Node));
	}

	private void assertConverted(GeneralNodeDatumAuxiliary genAux, DatumAuxiliaryEntity ent) {
		assertThat("Saved ID", ent.getId(), equalTo(ent.getId()));
		assertThat("Saved final sample", ent.getSamplesFinal(), equalTo(genAux.getSamplesFinal()));
		assertThat("Saved start sample", ent.getSamplesStart(), equalTo(genAux.getSamplesStart()));
		assertThat("Saved metadata", ent.getMetadata(), equalTo(genAux.getMeta()));
	}

	@Test
	public void store() {
		// GIVEN
		GeneralNodeDatumAuxiliary genAux = testGenAux();

		// look up meta based on node+source
		Capture<ObjectStreamCriteria> metaCriteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(metaCriteriaCaptor))).andReturn(singleton(meta));

		// save converted aux
		Capture<DatumAuxiliaryEntity> entCaptor = new Capture<>();
		DatumAuxiliaryPK entId = new DatumAuxiliaryPK(meta.getStreamId(), genAux.getCreated(),
				DatumAuxiliaryType.Reset);
		expect(datumAuxiliaryDao.save(capture(entCaptor))).andReturn(entId);

		// WHEN
		replayAll();
		biz.storeGeneralNodeDatumAuxiliary(genAux);

		// THEN
		asssertCriteria(metaCriteriaCaptor.getValue());
		assertConverted(genAux, entCaptor.getValue());
	}

	@Test
	public void get() {
		// GIVEN
		GeneralNodeDatumAuxiliaryPK id = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, Instant.now(),
				TEST_SOURCE_ID);

		// look up meta based on node+source
		Capture<ObjectStreamCriteria> metaCriteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(metaCriteriaCaptor))).andReturn(singleton(meta));

		// get by stream
		DatumAuxiliaryEntity ent = testAuxEntity(id.getCreated(), meta.getStreamId());
		expect(datumAuxiliaryDao.get(ent.getId())).andReturn(ent);

		// WHEN
		replayAll();
		GeneralNodeDatumAuxiliary genAux = biz.getGeneralNodeDatumAuxiliary(id);

		// THEN
		asssertCriteria(metaCriteriaCaptor.getValue());
		assertConverted(genAux, ent);
	}

	@Test
	public void remove() {
		// GIVEN
		GeneralNodeDatumAuxiliaryPK id = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, Instant.now(),
				TEST_SOURCE_ID);

		// look up meta based on node+source
		Capture<ObjectStreamCriteria> metaCriteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(metaCriteriaCaptor))).andReturn(singleton(meta));

		// get by stream
		DatumAuxiliaryEntity ent = testAuxEntity(id.getCreated(), meta.getStreamId());
		expect(datumAuxiliaryDao.get(ent.getId())).andReturn(ent);

		// delete by stream
		Capture<DatumAuxiliaryEntity> entCaptor = new Capture<>();
		datumAuxiliaryDao.delete(capture(entCaptor));

		// WHEN
		replayAll();
		biz.removeGeneralNodeDatumAuxiliary(id);

		// THEN
		asssertCriteria(metaCriteriaCaptor.getValue());
		assertThat("PK deleted", entCaptor.getValue().getId(), equalTo(
				new DatumAuxiliaryPK(meta.getStreamId(), id.getCreated(), DatumAuxiliaryType.Reset)));
	}

	@Test
	public void move_sameStream() {
		// GIVEN
		GeneralNodeDatumAuxiliary genAux = testGenAux();
		GeneralNodeDatumAuxiliary toGenAux = new GeneralNodeDatumAuxiliary(
				new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID,
						genAux.getCreated().plus(1, ChronoUnit.HOURS), TEST_SOURCE_ID),
				genAux.getSamplesFinal(), genAux.getSamplesStart());

		// look up meta based on node+source
		Capture<ObjectStreamCriteria> metaCriteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(metaCriteriaCaptor))).andReturn(singleton(meta));

		DatumAuxiliaryPK fromEntId = new DatumAuxiliaryPK(meta.getStreamId(), genAux.getCreated(),
				DatumAuxiliaryType.Reset);
		Capture<DatumAuxiliaryEntity> entCaptor = new Capture<>();
		expect(datumAuxiliaryDao.move(eq(fromEntId), capture(entCaptor))).andReturn(true);

		// WHEN
		replayAll();
		biz.moveGeneralNodeDatumAuxiliary(genAux.getId(), toGenAux);

		// THEN
		asssertCriteria(metaCriteriaCaptor.getValue());
		assertConverted(toGenAux, entCaptor.getValue());
	}

	@Test
	public void move_differentStream() {
		// GIVEN
		GeneralNodeDatumAuxiliary genAux = testGenAux();
		GeneralNodeDatumAuxiliary toGenAux = new GeneralNodeDatumAuxiliary(
				new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID,
						genAux.getCreated().plus(1, ChronoUnit.HOURS), "b"),
				genAux.getSamplesFinal(), genAux.getSamplesStart());

		// look up meta based on node+source (for from)
		Capture<ObjectStreamCriteria> metaFromCriteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata metaFrom = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(metaFromCriteriaCaptor)))
				.andReturn(singleton(metaFrom));

		// look up meta based on node+source (for to)
		Capture<ObjectStreamCriteria> metaToCriteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata metaTo = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, "b");
		expect(metaDao.findDatumStreamMetadata(capture(metaToCriteriaCaptor)))
				.andReturn(singleton(metaTo));

		DatumAuxiliaryPK fromEntId = new DatumAuxiliaryPK(metaFrom.getStreamId(), genAux.getCreated(),
				DatumAuxiliaryType.Reset);
		Capture<DatumAuxiliaryEntity> entCaptor = new Capture<>();
		expect(datumAuxiliaryDao.move(eq(fromEntId), capture(entCaptor))).andReturn(true);

		// WHEN
		replayAll();
		biz.moveGeneralNodeDatumAuxiliary(genAux.getId(), toGenAux);

		// THEN
		asssertCriteria(metaFromCriteriaCaptor.getValue(), TEST_NODE_ID, TEST_SOURCE_ID);
		asssertCriteria(metaToCriteriaCaptor.getValue(), TEST_NODE_ID, "b");
		assertConverted(toGenAux, entCaptor.getValue());
	}

	@Test
	public void find() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		DatumAuxiliaryEntity ent = testAuxEntity(Instant.now(), streamId);
		Capture<DatumAuxiliaryCriteria> filterCaptor = new Capture<>();
		BasicFilterResults<DatumAuxiliary, DatumAuxiliaryPK> daoResults = new BasicFilterResults<>(
				singleton(ent));
		expect(datumAuxiliaryDao.findFiltered(capture(filterCaptor))).andReturn(daoResults);

		ObjectDatumStreamMetadata meta = emptyMeta(streamId, "UTC", ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID);

		Capture<ObjectStreamCriteria> metaFilterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(metaFilterCaptor))).andReturn(singleton(meta));

		// WHEN
		replayAll();
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		criteria.setEndDate(criteria.getStartDate().plus(1, ChronoUnit.HOURS));
		FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> results = biz
				.findGeneralNodeDatumAuxiliary(criteria, null, null, null);

		// THEN
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));
		GeneralNodeDatumAuxiliaryFilterMatch s = results.iterator().next();
		assertThat("Datum ID returned from meta", s.getId(),
				equalTo(new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, ent.getTimestamp(), TEST_SOURCE_ID,
						DatumAuxiliaryType.Reset)));

		asssertCriteria(filterCaptor.getValue(), TEST_NODE_ID, TEST_SOURCE_ID);

		BasicDatumCriteria expectedMetaCriteria = ((BasicDatumCriteria) filterCaptor.getValue()).clone();
		expectedMetaCriteria.setStartDate(null);
		expectedMetaCriteria.setEndDate(null);
		assertThat("Same criteria used to find datum, minus date range, used to find meta",
				metaFilterCaptor.getValue(), equalTo(expectedMetaCriteria));
	}

}
