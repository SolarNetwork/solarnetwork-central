/* ==================================================================
 * DaoQueryBizTests.java - Jul 12, 2012 6:22:33 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.biz.dao.test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Import;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.biz.dao.DaoQueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.support.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Unit test for the {@link DaoQueryBiz} class.
 * 
 * @author matt
 * @version 4.0
 */
@Import({ net.solarnetwork.central.common.config.NetworkIdentityConfig.class,
		net.solarnetwork.central.query.config.CacheConfig.class,
		net.solarnetwork.central.query.config.NodeOwnershipCacheConfig.class })
public class DaoQueryBizTests extends AbstractQueryBizDaoTestSupport {

	private final String TEST_SOURCE_ID = "test.source";
	private final String TEST_SOURCE_ID2 = "test.source.2";
	private DatumEntityDao datumDao;
	private DatumStreamMetadataDao metaDao;
	private ReadingDatumDao readingDao;
	private SolarNodeOwnershipDao nodeOwnershipDao;

	private DaoQueryBiz biz;

	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);
		readingDao = EasyMock.createMock(ReadingDatumDao.class);
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		biz = new DaoQueryBiz(datumDao, metaDao, readingDao, nodeOwnershipDao);
		setupTestNode();
	}

	private void replayAll() {
		EasyMock.replay(datumDao, metaDao, readingDao, nodeOwnershipDao);
	}

	@After
	public void tearndown() {
		EasyMock.verify(datumDao, metaDao, readingDao, nodeOwnershipDao);
	}

	private static void assertConverted(String prefix, ReportableInterval result,
			DatumDateInterval expected) {
		assertThat(prefix + " start", result.getStartDate(),
				equalTo(expected.getStart().atZone(expected.getZone()).toLocalDateTime()));
		assertThat(prefix + " end", result.getEndDate(),
				equalTo(expected.getEnd().atZone(expected.getZone()).toLocalDateTime()));
		assertThat(prefix + " time zone", result.getTimeZone(), equalTo(expected.getZone()));
	}

	@Test
	public void getReportableInterval_node_noData() {
		// GIVEN
		Capture<DatumStreamCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findAvailableInterval(capture(filterCaptor))).andReturn(emptyList());

		// WHEN
		replayAll();
		ReportableInterval result = biz.getReportableInterval(TEST_NODE_ID, (String) null);

		// THEN
		assertThat("No result", result, nullValue());
		DatumStreamCriteria c = filterCaptor.getValue();
		assertThat("Query for node IDs", c.getNodeIds(), arrayContaining(TEST_NODE_ID));
		assertThat("Query for node kind", c.getObjectKind(), equalTo(ObjectDatumKind.Node));
	}

	@Test
	public void getReportableInterval_node_one() {
		// GIVEN
		DatumDateInterval range = DatumDateInterval.streamInterval(
				Instant.now().truncatedTo(ChronoUnit.HOURS), Instant.now(), "UTC", ObjectDatumKind.Node,
				UUID.randomUUID(), TEST_NODE_ID, TEST_SOURCE_ID);

		Capture<DatumStreamCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findAvailableInterval(capture(filterCaptor))).andReturn(singleton(range));

		// WHEN
		replayAll();
		ReportableInterval result = biz.getReportableInterval(TEST_NODE_ID, (String) null);

		// THEN
		assertThat("Result available", result, notNullValue());
		assertConverted("Range", result, range);

		DatumStreamCriteria c = filterCaptor.getValue();
		assertThat("Query for node IDs", c.getNodeIds(), arrayContaining(TEST_NODE_ID));
		assertThat("Query for node kind", c.getObjectKind(), equalTo(ObjectDatumKind.Node));
	}

	@Test
	public void getAvailableSources_node() {
		// GIVEN
		ObjectDatumStreamMetadata meta1 = emptyMeta(randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		ObjectDatumStreamMetadata meta2 = emptyMeta(randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID2);

		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(filterCaptor))).andReturn(asList(meta1, meta2));

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		Set<String> result = biz.getAvailableSources(filter);

		// THEN
		assertThat("Results", result, contains(TEST_SOURCE_ID, TEST_SOURCE_ID2));
		assertThat("Filtered on node ID", filterCaptor.getValue().getNodeIds(),
				arrayContaining(filter.getNodeIds()));
	}

	@Test
	public void getAvailableSources_nodeAndDateRange() {
		// GIVEN
		ObjectDatumStreamMetadata meta = emptyMeta(randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);

		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(filterCaptor))).andReturn(singleton(meta));

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		Set<String> result = biz.getAvailableSources(filter);

		// THEN
		assertThat("Results", result, contains(TEST_SOURCE_ID));
		assertThat("Filtered on node ID", filterCaptor.getValue().getNodeIds(),
				arrayContaining(filter.getNodeIds()));
		assertThat("Filtered on start date", filterCaptor.getValue().getStartDate(),
				equalTo(filter.getStartDate()));
		assertThat("Filtered on end date", filterCaptor.getValue().getEndDate(),
				equalTo(filter.getEndDate()));
	}

	@Test
	public void getReportableInterval_loc_noData() {
		// GIVEN
		Capture<DatumStreamCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findAvailableInterval(capture(filterCaptor))).andReturn(emptyList());

		// WHEN
		replayAll();
		ReportableInterval result = biz.getLocationReportableInterval(TEST_LOC_ID, null);

		// THEN
		assertThat("No result", result, nullValue());
		DatumStreamCriteria c = filterCaptor.getValue();
		assertThat("Query for loc IDs", c.getLocationIds(), arrayContaining(TEST_LOC_ID));
		assertThat("Query for loc kind", c.getObjectKind(), equalTo(ObjectDatumKind.Location));
	}

	@Test
	public void getReportableInterval_loc_one() {
		DatumDateInterval range = DatumDateInterval.streamInterval(
				Instant.now().truncatedTo(ChronoUnit.HOURS), Instant.now(), "UTC",
				ObjectDatumKind.Location, UUID.randomUUID(), TEST_LOC_ID, TEST_SOURCE_ID);

		// GIVEN
		Capture<DatumStreamCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findAvailableInterval(capture(filterCaptor))).andReturn(singleton(range));

		// WHEN
		replayAll();
		ReportableInterval result = biz.getLocationReportableInterval(TEST_LOC_ID, (String) null);

		// THEN
		assertThat("Result available", result, notNullValue());
		assertConverted("Range", result, range);

		DatumStreamCriteria c = filterCaptor.getValue();
		assertThat("Query for loc IDs", c.getLocationIds(), arrayContaining(TEST_LOC_ID));
		assertThat("Query for loc kind", c.getObjectKind(), equalTo(ObjectDatumKind.Location));
	}

	@Test
	public void getAvailableSources_loc() {
		// GIVEN
		ObjectDatumStreamMetadata meta1 = BasicObjectDatumStreamMetadata.emptyMeta(randomUUID(), "UTC",
				ObjectDatumKind.Node, TEST_LOC_ID, TEST_SOURCE_ID);
		ObjectDatumStreamMetadata meta2 = BasicObjectDatumStreamMetadata.emptyMeta(randomUUID(), "UTC",
				ObjectDatumKind.Node, TEST_LOC_ID, TEST_SOURCE_ID2);

		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(filterCaptor))).andReturn(asList(meta1, meta2));

		// WHEN
		replayAll();
		Set<String> result = biz.getLocationAvailableSources(TEST_LOC_ID, null, null);

		// THEN
		assertThat("Results", result, contains(TEST_SOURCE_ID, TEST_SOURCE_ID2));
		assertThat("Filtered on location ID", filterCaptor.getValue().getLocationIds(),
				arrayContaining(TEST_LOC_ID));
	}

	@Test
	public void getAvailableSources_locAndDateRange() {
		// GIVEN
		ObjectDatumStreamMetadata meta1 = BasicObjectDatumStreamMetadata.emptyMeta(randomUUID(), "UTC",
				ObjectDatumKind.Location, TEST_LOC_ID, TEST_SOURCE_ID);
		ObjectDatumStreamMetadata meta2 = BasicObjectDatumStreamMetadata.emptyMeta(randomUUID(), "UTC",
				ObjectDatumKind.Location, TEST_LOC_ID, TEST_SOURCE_ID2);

		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(filterCaptor))).andReturn(asList(meta1, meta2));

		// WHEN
		replayAll();
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		Instant end = start.plus(1, ChronoUnit.HOURS);
		Set<String> result = biz.getLocationAvailableSources(TEST_LOC_ID, start, end);

		// THEN
		assertThat("Results", result, contains(TEST_SOURCE_ID, TEST_SOURCE_ID2));
		assertThat("Filtered on location ID", filterCaptor.getValue().getLocationIds(),
				arrayContaining(TEST_LOC_ID));
		assertThat("Filtered on start date", filterCaptor.getValue().getStartDate(), equalTo(start));
		assertThat("Filtered on end date", filterCaptor.getValue().getEndDate(), equalTo(end));
	}

	@Test
	public void findNodes_dataToken() {
		// GIVEN
		Long userId = storeNewUser(TEST_USER_EMAIL);
		SecurityToken actor = becomeAuthenticatedReadNodeDataToken(userId, null);

		expect(nodeOwnershipDao.nonArchivedNodeIdsForToken(actor.getToken()))
				.andReturn(new Long[] { 1L, 2L });

		// WHEN
		replayAll();
		Set<Long> results = biz.findAvailableNodes(actor);

		// THEN
		assertThat("Results", results, contains(1L, 2L));
	}

	@Test
	public void findSources_dataToken() {
		// GIVEN
		Long userId = storeNewUser(TEST_USER_EMAIL);
		Capture<ObjectStreamCriteria> filterCaptor = new Capture<>();
		ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), "UTC", ObjectDatumKind.Node,
				TEST_NODE_ID, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(filterCaptor))).andReturn(singleton(meta));

		// WHEN
		replayAll();
		SecurityToken actor = becomeAuthenticatedReadNodeDataToken(userId, null);
		Set<NodeSourcePK> results = biz.findAvailableSources(actor, null);

		// THEN
		assertThat("Results", results, contains(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));
		assertThat("Filtered on token", filterCaptor.getValue().getTokenIds(),
				arrayContaining(actor.getToken()));
	}

	private DatumProperties testProps() {
		return propertiesOf(decimalArray("1.1", "1.2"), decimalArray("2.1"), null, null);
	}

	private DatumPropertiesStatistics testStats() {
		return statisticsOf(
				new BigDecimal[][] { decimalArray("60", "1.0", "2.0"),
						decimalArray("61", "2.0", "3.0") },
				new BigDecimal[][] { decimalArray("10", "0", "10") });
	}

	@Test
	public void findFilteredGeneralNodeDatum() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "i1", "i2" },
				new String[] { "a1" }, null, null);
		DatumProperties props = testProps();
		DatumEntity d = new DatumEntity(meta.getStreamId(), Instant.now(), Instant.now(), props);
		BasicObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = new BasicObjectDatumStreamFilterResults<>(
				singletonMap(meta.getStreamId(), meta), singleton(d));

		Capture<DatumCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findFiltered(capture(filterCaptor))).andReturn(daoResults);

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(1L);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		List<SortDescriptor> sortDescriptors = Arrays.asList(new SimpleSortDescriptor("created", true));
		FilterResults<GeneralNodeDatumFilterMatch> results = biz.findFilteredGeneralNodeDatum(filter,
				sortDescriptors, 1, 2);

		// THEN
		assertThat("Query kind is node", filterCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
		assertThat("Query node IDs", filterCaptor.getValue().getNodeIds(),
				arrayContaining(filter.getNodeIds()));
		assertThat("Query start date", filterCaptor.getValue().getStartDate(),
				equalTo(filter.getStartDate()));
		assertThat("Query end date", filterCaptor.getValue().getEndDate(), equalTo(filter.getEndDate()));
		assertThat("Query sorts", filterCaptor.getValue().getSorts(),
				contains(new net.solarnetwork.domain.SimpleSortDescriptor("created", true)));
		assertThat("Query offset", filterCaptor.getValue().getOffset(), equalTo(1));
		assertThat("Query max", filterCaptor.getValue().getMax(), equalTo(2));

		assertThat("Results returned", results, notNullValue());
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));

		GeneralNodeDatumFilterMatch match = results.iterator().next();
		assertThat("Match node from meta", match.getId(),
				equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, d.getTimestamp(), TEST_SOURCE_ID)));
		assertThat("Match local date from meta", match.getLocalDate(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalDate()));
		assertThat("Match local time from meta", match.getLocalTime(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalTime()));
		// @formatter:off
		assertThat("Converted datum props", match.getSampleData(), allOf(
				hasEntry("i1", props.getInstantaneous()[0]),
				hasEntry("i2", props.getInstantaneous()[1]),
				hasEntry("a1", props.getAccumulating()[0])
		));
		// @formatter:on
	}

	@Test
	public void findFilteredAggregateGeneralNodeDatum() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "i1", "i2" },
				new String[] { "a1" }, null, null);
		DatumProperties props = testProps();
		DatumPropertiesStatistics stats = testStats();
		AggregateDatumEntity d = new AggregateDatumEntity(meta.getStreamId(), Instant.now(),
				Aggregation.Day, props, stats);
		BasicObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = new BasicObjectDatumStreamFilterResults<>(
				singletonMap(meta.getStreamId(), meta), singleton(d));

		Capture<DatumCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findFiltered(capture(filterCaptor))).andReturn(daoResults);

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setAggregate(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		List<SortDescriptor> sortDescriptors = Arrays.asList(new SimpleSortDescriptor("created", true));
		FilterResults<ReportingGeneralNodeDatumMatch> results = biz
				.findFilteredAggregateGeneralNodeDatum(filter, sortDescriptors, 1, 2);

		// THEN
		assertThat("Query kind is node", filterCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
		assertThat("Query aggregation", filterCaptor.getValue().getAggregation(),
				equalTo(filter.getAggregation()));
		assertThat("Query node IDs", filterCaptor.getValue().getNodeIds(),
				arrayContaining(filter.getNodeIds()));
		assertThat("Query start date", filterCaptor.getValue().getStartDate(),
				equalTo(filter.getStartDate()));
		assertThat("Query end date", filterCaptor.getValue().getEndDate(), equalTo(filter.getEndDate()));
		assertThat("Query sorts", filterCaptor.getValue().getSorts(),
				contains(new net.solarnetwork.domain.SimpleSortDescriptor("created", true)));
		assertThat("Query offset", filterCaptor.getValue().getOffset(), equalTo(1));
		assertThat("Query max", filterCaptor.getValue().getMax(), equalTo(2));

		assertThat("Results returned", results, notNullValue());
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch match = results.iterator().next();
		assertThat("Match node from meta", match.getId(),
				equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, d.getTimestamp(), TEST_SOURCE_ID)));
		assertThat("Match local date from meta", match.getLocalDate(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalDate()));
		assertThat("Match local time from meta", match.getLocalTime(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalTime()));
		// @formatter:off
		assertThat("Converted datum props", match.getSampleData(), allOf(
				hasEntry("i1", props.getInstantaneous()[0]),
				hasEntry("i1_min", stats.getInstantaneous()[0][1]),
				hasEntry("i1_max", stats.getInstantaneous()[0][2]),
				hasEntry("i2", props.getInstantaneous()[1]),
				hasEntry("i2_min", stats.getInstantaneous()[1][1]),
				hasEntry("i2_max", stats.getInstantaneous()[1][2]),
				hasEntry("a1", props.getAccumulating()[0])
		));
		// @formatter:on
	}

	@Test
	public void findGeneralLocationDatum() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Location, TEST_LOC_ID, TEST_SOURCE_ID, new String[] { "i1", "i2" },
				new String[] { "a1" }, null, null);
		DatumProperties props = testProps();
		DatumEntity d = new DatumEntity(meta.getStreamId(), Instant.now(), Instant.now(), props);
		BasicObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = new BasicObjectDatumStreamFilterResults<>(
				singletonMap(meta.getStreamId(), meta), singleton(d));

		Capture<DatumCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findFiltered(capture(filterCaptor))).andReturn(daoResults);

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocationId(TEST_LOC_ID);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		List<SortDescriptor> sortDescriptors = Arrays.asList(new SimpleSortDescriptor("created", true));
		FilterResults<GeneralLocationDatumFilterMatch> results = biz.findGeneralLocationDatum(filter,
				sortDescriptors, 1, 2);

		// THEN
		assertThat("Query kind is location", filterCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Location));
		assertThat("Query location IDs", filterCaptor.getValue().getLocationIds(),
				arrayContaining(filter.getLocationIds()));
		assertThat("Query start date", filterCaptor.getValue().getStartDate(),
				equalTo(filter.getStartDate()));
		assertThat("Query end date", filterCaptor.getValue().getEndDate(), equalTo(filter.getEndDate()));
		assertThat("Query sorts", filterCaptor.getValue().getSorts(),
				contains(new net.solarnetwork.domain.SimpleSortDescriptor("created", true)));
		assertThat("Query offset", filterCaptor.getValue().getOffset(), equalTo(1));
		assertThat("Query max", filterCaptor.getValue().getMax(), equalTo(2));

		assertThat("Results returned", results, notNullValue());
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));

		GeneralLocationDatumFilterMatch match = results.iterator().next();
		assertThat("Match loc from meta", match.getId(),
				equalTo(new GeneralLocationDatumPK(TEST_LOC_ID, d.getTimestamp(), TEST_SOURCE_ID)));
		assertThat("Match actually implements reporting", match,
				instanceOf(ReportingGeneralLocationDatumMatch.class));
		assertThat("Converted datum props", ((ReportingGeneralLocationDatumMatch) match).getSampleData(),
				allOf(
				// @formatter:off
				hasEntry("i1", props.getInstantaneous()[0]),
				hasEntry("i2", props.getInstantaneous()[1]),
				hasEntry("a1", props.getAccumulating()[0])
				// @formatter:on
				));
	}

	@Test
	public void findAggregateGeneralLocationDatum() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Location, TEST_LOC_ID, TEST_SOURCE_ID, new String[] { "i1", "i2" },
				new String[] { "a1" }, null, null);
		DatumProperties props = testProps();
		DatumPropertiesStatistics stats = testStats();
		AggregateDatumEntity d = new AggregateDatumEntity(meta.getStreamId(), Instant.now(),
				Aggregation.Day, props, stats);
		BasicObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = new BasicObjectDatumStreamFilterResults<>(
				singletonMap(meta.getStreamId(), meta), singleton(d));

		Capture<DatumCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findFiltered(capture(filterCaptor))).andReturn(daoResults);

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setAggregate(Aggregation.Day);
		filter.setLocationId(TEST_LOC_ID);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		List<SortDescriptor> sortDescriptors = Arrays.asList(new SimpleSortDescriptor("created", true));
		FilterResults<ReportingGeneralLocationDatumMatch> results = biz
				.findAggregateGeneralLocationDatum(filter, sortDescriptors, 1, 2);

		// THEN
		assertThat("Query kind is location", filterCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Location));
		assertThat("Query aggregation", filterCaptor.getValue().getAggregation(),
				equalTo(filter.getAggregation()));
		assertThat("Query location IDs", filterCaptor.getValue().getLocationIds(),
				arrayContaining(filter.getLocationIds()));
		assertThat("Query start date", filterCaptor.getValue().getStartDate(),
				equalTo(filter.getStartDate()));
		assertThat("Query end date", filterCaptor.getValue().getEndDate(), equalTo(filter.getEndDate()));
		assertThat("Query sorts", filterCaptor.getValue().getSorts(),
				contains(new net.solarnetwork.domain.SimpleSortDescriptor("created", true)));
		assertThat("Query offset", filterCaptor.getValue().getOffset(), equalTo(1));
		assertThat("Query max", filterCaptor.getValue().getMax(), equalTo(2));

		assertThat("Results returned", results, notNullValue());
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralLocationDatumMatch match = results.iterator().next();
		assertThat("Match loc from meta", match.getId(),
				equalTo(new GeneralLocationDatumPK(TEST_LOC_ID, d.getTimestamp(), TEST_SOURCE_ID)));
		assertThat("Match local date from meta", match.getLocalDate(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalDate()));
		assertThat("Match local time from meta", match.getLocalTime(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalTime()));
		// @formatter:off
		assertThat("Converted datum props", match.getSampleData(), allOf(
				hasEntry("i1", props.getInstantaneous()[0]),
				hasEntry("i1_min", stats.getInstantaneous()[0][1]),
				hasEntry("i1_max", stats.getInstantaneous()[0][2]),
				hasEntry("i2", props.getInstantaneous()[1]),
				hasEntry("i2_min", stats.getInstantaneous()[1][1]),
				hasEntry("i2_max", stats.getInstantaneous()[1][2]),
				hasEntry("a1", props.getAccumulating()[0])
		));
		// @formatter:on
	}

	@Test
	public void findFilteredAggregateReading_invalidReadingType() {
		replayAll();
		// only the Difference type is supported
		for ( DatumReadingType type : EnumSet.complementOf(EnumSet.of(DatumReadingType.Difference)) ) {
			try {
				biz.findFilteredAggregateReading(new DatumFilterCommand(),
						DatumReadingType.DifferenceWithin, null, null, null, null);
				Assert.fail("Should have thrown IllegalArgumentException for DatumReadingType " + type);
			} catch ( IllegalArgumentException e ) {
				// expected
			}
		}
	}

	@Test
	public void findFilteredAggregateReading() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "i1", "i2" },
				new String[] { "a1" }, null, null);
		DatumProperties props = testProps();
		DatumPropertiesStatistics stats = testStats();
		ReadingDatumEntity d = new ReadingDatumEntity(meta.getStreamId(), Instant.now(), Aggregation.Day,
				Instant.now(), props, stats);
		BasicObjectDatumStreamFilterResults<Datum, DatumPK> daoResults = new BasicObjectDatumStreamFilterResults<>(
				singletonMap(meta.getStreamId(), meta), singleton(d));

		Capture<DatumCriteria> filterCaptor = new Capture<>();
		expect(datumDao.findFiltered(capture(filterCaptor))).andReturn(daoResults);

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setAggregate(Aggregation.Day);
		filter.setNodeId(TEST_NODE_ID);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		List<SortDescriptor> sortDescriptors = Arrays.asList(new SimpleSortDescriptor("created", true));
		FilterResults<ReportingGeneralNodeDatumMatch> results = biz.findFilteredAggregateReading(filter,
				DatumReadingType.Difference, Period.ofMonths(1), sortDescriptors, 1, 2);

		// THEN
		assertThat("Query reading type", filterCaptor.getValue().getReadingType(),
				equalTo(DatumReadingType.Difference));
		assertThat("Query kind is node", filterCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
		assertThat("Query aggregation", filterCaptor.getValue().getAggregation(),
				equalTo(filter.getAggregation()));
		assertThat("Query node IDs", filterCaptor.getValue().getNodeIds(),
				arrayContaining(filter.getNodeIds()));
		assertThat("Query start date", filterCaptor.getValue().getStartDate(),
				equalTo(filter.getStartDate()));
		assertThat("Query end date", filterCaptor.getValue().getEndDate(), equalTo(filter.getEndDate()));
		assertThat("Query sorts", filterCaptor.getValue().getSorts(),
				contains(new net.solarnetwork.domain.SimpleSortDescriptor("created", true)));
		assertThat("Query offset", filterCaptor.getValue().getOffset(), equalTo(1));
		assertThat("Query max", filterCaptor.getValue().getMax(), equalTo(2));

		assertThat("Results returned", results, notNullValue());
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch match = results.iterator().next();
		assertThat("Match node from meta", match.getId(),
				equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, d.getTimestamp(), TEST_SOURCE_ID)));
		assertThat("Match local date from meta", match.getLocalDate(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalDate()));
		assertThat("Match local time from meta", match.getLocalTime(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalTime()));
		// @formatter:off
		assertThat("Converted datum props", match.getSampleData(), allOf(
				hasEntry("i1", props.getInstantaneous()[0]),
				hasEntry("i1_min", stats.getInstantaneous()[0][1]),
				hasEntry("i1_max", stats.getInstantaneous()[0][2]),
				hasEntry("i2", props.getInstantaneous()[1]),
				hasEntry("i2_min", stats.getInstantaneous()[1][1]),
				hasEntry("i2_max", stats.getInstantaneous()[1][2]),
				hasEntry("a1", stats.getAccumulating()[0][0]),
				hasEntry("a1_start", stats.getAccumulating()[0][1]),
				hasEntry("a1_end", stats.getAccumulating()[0][2])
		));
		// @formatter:on
	}

	@Test
	public void findFilteredReading() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "i1", "i2" },
				new String[] { "a1" }, null, null);
		DatumProperties props = testProps();
		DatumPropertiesStatistics stats = testStats();
		ReadingDatumEntity d = new ReadingDatumEntity(meta.getStreamId(), Instant.now(), Aggregation.Day,
				Instant.now(), props, stats);
		BasicObjectDatumStreamFilterResults<ReadingDatum, DatumPK> daoResults = new BasicObjectDatumStreamFilterResults<>(
				singletonMap(meta.getStreamId(), meta), singleton(d));

		Capture<ReadingDatumCriteria> filterCaptor = new Capture<>();
		expect(readingDao.findDatumReadingFiltered(capture(filterCaptor))).andReturn(daoResults);

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setAggregate(Aggregation.Day);
		filter.setNodeId(TEST_NODE_ID);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));
		FilterResults<ReportingGeneralNodeDatumMatch> results = biz.findFilteredReading(filter,
				DatumReadingType.DifferenceWithin, Period.ofMonths(1));

		// THEN
		assertThat("Query reading type", filterCaptor.getValue().getReadingType(),
				equalTo(DatumReadingType.DifferenceWithin));
		assertThat("Query kind is node", filterCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
		assertThat("Query aggregation", filterCaptor.getValue().getAggregation(),
				equalTo(filter.getAggregation()));
		assertThat("Query node IDs", filterCaptor.getValue().getNodeIds(),
				arrayContaining(filter.getNodeIds()));
		assertThat("Query start date", filterCaptor.getValue().getStartDate(),
				equalTo(filter.getStartDate()));
		assertThat("Query end date", filterCaptor.getValue().getEndDate(), equalTo(filter.getEndDate()));

		assertThat("Results returned", results, notNullValue());
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch match = results.iterator().next();
		assertThat("Match node from meta", match.getId(),
				equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, d.getTimestamp(), TEST_SOURCE_ID)));
		assertThat("Match local date from meta", match.getLocalDate(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalDate()));
		assertThat("Match local time from meta", match.getLocalTime(),
				equalTo(d.getTimestamp().atOffset(ZoneOffset.UTC).toLocalTime()));
		// @formatter:off
		assertThat("Converted datum props", match.getSampleData(), allOf(
				hasEntry("i1", props.getInstantaneous()[0]),
				hasEntry("i1_min", stats.getInstantaneous()[0][1]),
				hasEntry("i1_max", stats.getInstantaneous()[0][2]),
				hasEntry("i2", props.getInstantaneous()[1]),
				hasEntry("i2_min", stats.getInstantaneous()[1][1]),
				hasEntry("i2_max", stats.getInstantaneous()[1][2]),
				hasEntry("a1", stats.getAccumulating()[0][0]),
				hasEntry("a1_start", stats.getAccumulating()[0][1]),
				hasEntry("a1_end", stats.getAccumulating()[0][2])
		));
		// @formatter:on
	}

}
