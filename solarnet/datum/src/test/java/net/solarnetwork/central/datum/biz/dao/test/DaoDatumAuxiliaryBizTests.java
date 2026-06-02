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

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumAuxiliaryBiz;
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
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link DaoDatumAuxiliaryBiz} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoDatumAuxiliaryBizTests {

	private static final Long TEST_NODE_ID = 1L;
	private static final String TEST_SOURCE_ID = "a";

	@Mock
	private DatumAuxiliaryEntityDao datumAuxiliaryDao;

	@Mock
	private DatumStreamMetadataDao metaDao;

	@Captor
	private ArgumentCaptor<ObjectStreamCriteria> metaCriteriaCaptor;

	@Captor
	private ArgumentCaptor<DatumAuxiliaryEntity> entCaptor;

	@Captor
	private ArgumentCaptor<DatumAuxiliaryCriteria> metaFilterCaptor;

	private DatumAuxiliaryBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoDatumAuxiliaryBiz(datumAuxiliaryDao, metaDao);
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

		GeneralNodeDatumAuxiliary genAux = new GeneralNodeDatumAuxiliary(new GeneralNodeDatumAuxiliaryPK(
				TEST_NODE_ID, Instant.now().truncatedTo(ChronoUnit.HOURS), TEST_SOURCE_ID));
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
		return new DatumAuxiliaryEntity(streamId, date, DatumAuxiliaryType.Reset, now(), sf, ss, "Note.",
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
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		given(metaDao.findDatumStreamMetadata(any())).willReturn(singleton(meta));

		// save converted aux
		DatumAuxiliaryPK entId = new DatumAuxiliaryPK(meta.getStreamId(), genAux.getCreated(),
				DatumAuxiliaryType.Reset);
		given(datumAuxiliaryDao.save(any())).willReturn(entId);

		// WHEN
		biz.storeGeneralNodeDatumAuxiliary(genAux);

		// THEN
		then(metaDao).should().findDatumStreamMetadata(metaCriteriaCaptor.capture());
		asssertCriteria(metaCriteriaCaptor.getValue());
		then(datumAuxiliaryDao).should().save(entCaptor.capture());
		assertConverted(genAux, entCaptor.getValue());
	}

	@Test
	public void get() {
		// GIVEN
		GeneralNodeDatumAuxiliaryPK id = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, Instant.now(),
				TEST_SOURCE_ID);

		// look up meta based on node+source
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		given(metaDao.findDatumStreamMetadata(any())).willReturn(singleton(meta));

		// get by stream
		DatumAuxiliaryEntity ent = testAuxEntity(id.getCreated(), meta.getStreamId());
		given(datumAuxiliaryDao.get(ent.getId())).willReturn(ent);

		// WHEN
		GeneralNodeDatumAuxiliary genAux = biz.getGeneralNodeDatumAuxiliary(id);

		// THEN
		then(metaDao).should().findDatumStreamMetadata(metaCriteriaCaptor.capture());
		asssertCriteria(metaCriteriaCaptor.getValue());
		assertConverted(genAux, ent);
	}

	@Test
	public void remove() {
		// GIVEN
		GeneralNodeDatumAuxiliaryPK id = new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, Instant.now(),
				TEST_SOURCE_ID);

		// look up meta based on node+source
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		given(metaDao.findDatumStreamMetadata(any())).willReturn(singleton(meta));

		// get by stream
		DatumAuxiliaryEntity ent = testAuxEntity(id.getCreated(), meta.getStreamId());
		given(datumAuxiliaryDao.get(ent.getId())).willReturn(ent);

		// WHEN
		biz.removeGeneralNodeDatumAuxiliary(id);

		// THEN
		then(metaDao).should().findDatumStreamMetadata(metaCriteriaCaptor.capture());
		asssertCriteria(metaCriteriaCaptor.getValue());
		then(datumAuxiliaryDao).should().delete(entCaptor.capture());
		// @formatter:off
		and.then(entCaptor.getValue())
			.as("PK deleted")
			.returns(new DatumAuxiliaryPK(meta.getStreamId(), id.getCreated(), DatumAuxiliaryType.Reset), from(DatumAuxiliaryEntity::getId))
			;
		// @formatter:on
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
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		given(metaDao.findDatumStreamMetadata(any())).willReturn(singleton(meta));

		DatumAuxiliaryPK fromEntId = new DatumAuxiliaryPK(meta.getStreamId(), genAux.getCreated(),
				DatumAuxiliaryType.Reset);
		given(datumAuxiliaryDao.move(eq(fromEntId), any())).willReturn(true);

		// WHEN
		biz.moveGeneralNodeDatumAuxiliary(genAux.getId(), toGenAux);

		// THEN
		then(metaDao).should().findDatumStreamMetadata(metaCriteriaCaptor.capture());
		asssertCriteria(metaCriteriaCaptor.getValue());
		then(datumAuxiliaryDao).should().move(eq(fromEntId), entCaptor.capture());
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
		ObjectDatumStreamMetadata metaFrom = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);

		// look up meta based on node+source (for to)
		ObjectDatumStreamMetadata metaTo = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, "b");

		given(metaDao.findDatumStreamMetadata(any())).willReturn(singleton(metaFrom))
				.willReturn(singleton(metaTo));

		DatumAuxiliaryPK fromEntId = new DatumAuxiliaryPK(metaFrom.getStreamId(), genAux.getCreated(),
				DatumAuxiliaryType.Reset);
		given(datumAuxiliaryDao.move(eq(fromEntId), any())).willReturn(true);

		// WHEN
		biz.moveGeneralNodeDatumAuxiliary(genAux.getId(), toGenAux);

		// THEN
		then(metaDao).should(times(2)).findDatumStreamMetadata(metaCriteriaCaptor.capture());
		asssertCriteria(metaCriteriaCaptor.getAllValues().get(0), TEST_NODE_ID, TEST_SOURCE_ID);
		asssertCriteria(metaCriteriaCaptor.getAllValues().get(1), TEST_NODE_ID, "b");
		then(datumAuxiliaryDao).should().move(eq(fromEntId), entCaptor.capture());
		assertConverted(toGenAux, entCaptor.getValue());
	}

	@Test
	public void find() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		DatumAuxiliaryEntity ent = testAuxEntity(Instant.now(), streamId);
		BasicFilterResults<DatumAuxiliary, DatumAuxiliaryPK> daoResults = new BasicFilterResults<>(
				singleton(ent));
		given(datumAuxiliaryDao.findFiltered(any())).willReturn(daoResults);

		ObjectDatumStreamMetadata meta = emptyMeta(streamId, "UTC", ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID);

		given(metaDao.findDatumStreamMetadata(any())).willReturn(singleton(meta));

		// WHEN
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		criteria.setEndDate(criteria.getStartDate().plus(1, ChronoUnit.HOURS));
		FilterResults<GeneralNodeDatumAuxiliaryFilterMatch, GeneralNodeDatumAuxiliaryPK> results = biz
				.findGeneralNodeDatumAuxiliary(criteria, null, null, null);

		// THEN
		// @formatter:off
		and.then(results)
			.hasSize(1)
			.element(0, InstanceOfAssertFactories.type(GeneralNodeDatumAuxiliaryFilterMatch.class))
			.returns(new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, ent.getTimestamp(), TEST_SOURCE_ID,
					DatumAuxiliaryType.Reset), from(GeneralNodeDatumAuxiliaryFilterMatch::getId))
			;


		then(metaDao).should().findDatumStreamMetadata(metaFilterCaptor.capture());
		asssertCriteria(metaFilterCaptor.getValue(), TEST_NODE_ID, TEST_SOURCE_ID);

		BasicDatumCriteria expectedMetaCriteria = ((BasicDatumCriteria) metaFilterCaptor.getValue())
				.clone();
		expectedMetaCriteria.setStartDate(null);
		expectedMetaCriteria.setEndDate(null);
		// @formatter:off
		and.then(metaFilterCaptor.getValue())
			.as("Same criteria used to find datum, minus date range, used to find meta")
			.isEqualTo(expectedMetaCriteria)
			;
		// @formatter:on
	}

	@Test
	public void find_markType() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		DatumAuxiliaryEntity ent = testAuxEntity(Instant.now(), streamId);
		BasicFilterResults<DatumAuxiliary, DatumAuxiliaryPK> daoResults = new BasicFilterResults<>(
				singleton(ent));
		given(datumAuxiliaryDao.findFiltered(any())).willReturn(daoResults);

		ObjectDatumStreamMetadata meta = emptyMeta(streamId, "UTC", ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID);

		given(metaDao.findDatumStreamMetadata(any())).willReturn(singleton(meta));

		// WHEN
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		criteria.setEndDate(criteria.getStartDate().plus(1, ChronoUnit.HOURS));
		criteria.setDatumAuxiliaryType(DatumAuxiliaryType.Mark);
		FilterResults<GeneralNodeDatumAuxiliaryFilterMatch, GeneralNodeDatumAuxiliaryPK> results = biz
				.findGeneralNodeDatumAuxiliary(criteria, null, null, null);

		// THEN
		// @formatter:off
		and.then(results)
			.hasSize(1)
			.element(0, InstanceOfAssertFactories.type(GeneralNodeDatumAuxiliaryFilterMatch.class))
			.returns(new GeneralNodeDatumAuxiliaryPK(TEST_NODE_ID, ent.getTimestamp(), TEST_SOURCE_ID,
					DatumAuxiliaryType.Reset), from(GeneralNodeDatumAuxiliaryFilterMatch::getId))
			;

		then(metaDao).should().findDatumStreamMetadata(metaFilterCaptor.capture());
		asssertCriteria(metaFilterCaptor.getValue(), TEST_NODE_ID, TEST_SOURCE_ID);
		and.then(metaFilterCaptor.getValue())
			.returns(criteria.getDatumAuxiliaryType(), from(DatumAuxiliaryCriteria::getDatumAuxiliaryType))
			;

		BasicDatumCriteria expectedMetaCriteria = ((BasicDatumCriteria) metaFilterCaptor.getValue())
				.clone();
		expectedMetaCriteria.setStartDate(null);
		expectedMetaCriteria.setEndDate(null);
		and.then(metaFilterCaptor.getValue())
			.as("Same criteria used to find datum, minus date range, used to find meta")
			.isEqualTo(expectedMetaCriteria)
			;
		// @formatter:on
	}

}
