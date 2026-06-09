/* ==================================================================
 * CloudDatumStreamDatumImportInputFormatServiceTests.java - 15/10/2024 8:52:07 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService.DATUM_STREAM_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService.END_DATE_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService.START_DATE_SETTING;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.datum.domain.DatumValidationType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.imp.domain.BasicInputConfiguration;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link CloudDatumStreamDatumImportInputFormatService}
 * class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CloudDatumStreamDatumImportInputFormatServiceTests {

	private static final Long TEST_USER_ID = randomLong();
	private static final String TEST_DATUM_STREAM_SERVICE_IDENTIFIER = randomString();

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamService datumStreamService;

	@Captor
	private ArgumentCaptor<CloudDatumStreamQueryFilter> filterCaptor;

	@Mock
	private DatumAuxiliaryEntityDao datumAuxiliaryDao;

	@Captor
	private ArgumentCaptor<DatumAuxiliaryEntity> datumAuxiliaryEntityCaptor;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	@Mock
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Captor
	private ArgumentCaptor<ObjectStreamCriteria> datumStreamMetadataFilterCaptor;

	private CloudDatumStreamDatumImportInputFormatService service;

	@BeforeEach
	public void setup() {
		var datumStreamServices = Map.of(TEST_DATUM_STREAM_SERVICE_IDENTIFIER, datumStreamService);
		service = new CloudDatumStreamDatumImportInputFormatService(userEventAppenderBiz, datumStreamDao,
				datumAuxiliaryDao, datumStreamMetadataDao, datumStreamServices::get);
	}

	@Test
	public void singleBatchRange() throws IOException {
		// GIVEN
		final ZonedDateTime startDate = LocalDate.of(2010, 1, 1).atStartOfDay(UTC);
		final ZonedDateTime endDate = LocalDate.of(2010, 2, 1).atStartOfDay(UTC);

		final var datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID, randomLong(), now(),
				randomString(), TEST_DATUM_STREAM_SERVICE_IDENTIFIER, ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		final var config = new BasicInputConfiguration(TEST_USER_ID);
		config.setServiceProps(Map.of(
		// @formatter:off
				DATUM_STREAM_ID_SETTING, datumStream.getConfigId(),
				START_DATE_SETTING, startDate.toInstant().toString(),
				END_DATE_SETTING, endDate.toInstant().toString()
				// @formatter:on
		));
		final var resource = new BasicDatumImportResource(new ByteArrayResource(new byte[0]),
				"application/octet-stream");

		// resolve source IDs
		final Set<String> resolvedSourceIds = Set.of(datumStream.getSourceId());
		given(datumStreamService.datumStreamSourceIds(datumStream)).willReturn(resolvedSourceIds);

		// delete auxiliary, manually capture copies because same instance re-used across invocations
		final List<DatumAuxiliaryCriteria> capturedAuxiliaryFilters = new ArrayList<>(8);
		given(datumAuxiliaryDao.deleteFiltered(any())).will(inv -> {
			var f = inv.getArgument(0, DatumAuxiliaryCriteria.class);
			capturedAuxiliaryFilters.add(new BasicDatumCriteria(f));
			return 0L;
		});

		// request Datum Stream from DAO
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// request datum from service
		final List<BasicQueryFilter> queryFilters = new ArrayList<>(8);
		queryFilters.add(BasicQueryFilter.ofRange(startDate.toInstant(), endDate.toInstant()));

		final List<Datum> datumList = new ArrayList<>(8);
		ZonedDateTime day = startDate;
		while ( day.isBefore(endDate) ) {
			datumList.add(new GeneralDatum(DatumId.nodeId(datumStream.getObjectId(),
					datumStream.getSourceId(), day.toInstant()), new DatumSamples()));
			day = day.plus(1L, ChronoUnit.DAYS);
		}
		// @formatter:off
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(datumList))
				;
		// @formatter:on

		// WHEN
		final var progress = new ArrayList<Double>(8);
		final var datum = new ArrayList<GeneralNodeDatum>(8);

		// WHEN
		try (var ctx = service.createImportContext(config, resource, (_, progressAmount) -> {
			progress.add(progressAmount);
		})) {
			for ( var d : ctx ) {
				datum.add(d);
			}
		}

		// THEN
		// @formatter:off
		thenDeleteAuxiliaryForQueryRanges(datumStream, resolvedSourceIds, capturedAuxiliaryFilters, queryFilters);

		then(datumStreamService).shouldHaveNoMoreInteractions();

		and.then(filterCaptor.getAllValues())
		.as("Queried for pages using original filter plus all next filters provided in service results")
		.satisfies(filters -> {
			for (int i =0, len = filters.size(); i < len; i++ ) {
				and.then(filters).element(i)
					.as("Start date (page %d) from expected page filter", i)
					.returns(queryFilters.get(i).getStartDate(), from(CloudDatumStreamQueryFilter::getStartDate))
					.as("End date (page %d) from original filter", i)
					.returns(endDate.toInstant(), from(CloudDatumStreamQueryFilter::getEndDate))
					;
			}
		})
		;

		// TODO: validate progress?

		and.then(datum)
			.as("All datum from service are returned")
			.hasSize(datumList.size())
			;
		for (int i =0, len = datum.size(); i < len; i++) {
			and.then(datum)
				.element(i)
				.as("Import datum converted from service datum")
				.isEqualTo(DatumUtils.convertGeneralDatum(datumList.get(i)))
				;
		}
		// @formatter:on
	}

	private void thenDeleteAuxiliaryForQueryRanges(final CloudDatumStreamConfiguration datumStream,
			final Set<String> resolvedSourceIds,
			final List<DatumAuxiliaryCriteria> capturedAuxiliaryFilters,
			final List<? extends DateRangeCriteria> queryFilters) {
		// @formatter:off
		and.then(capturedAuxiliaryFilters)
			.as("Delete auxiliary called for each queried datum page")
			.hasSize(queryFilters.size())
			.allSatisfy(f -> {
				and.then(f)
					.as("Delete filter for Mark type")
					.returns(DatumAuxiliaryType.Mark, from(DatumAuxiliaryCriteria::getDatumAuxiliaryType))
					.as("Delete filter has generated metadata search filter")
					.returns(CloudDatumStreamService.GENERATED_AUXILIARY_SEARCH_FILTER,
							from(DatumAuxiliaryCriteria::getSearchFilter))
					.as("Delete filter object kind for Datum Stream kind")
					.returns(datumStream.getKind(), from(DatumAuxiliaryCriteria::getObjectKind))
					.as("Delete filter for Datum Stream node ID")
					.returns(datumStream.getObjectId(), from(DatumAuxiliaryCriteria::getObjectId))
					.as("Delete filter for resolved source IDs")
					.returns(resolvedSourceIds.toArray(String[]::new), from(DatumAuxiliaryCriteria::getSourceIds))
					;
			})
			.satisfies(filters -> {
				for (int i = 0, len = filters.size(); i < len; i++ ) {
					and.then(filters).element(i)
						.as("Start date (page %d) from expected page filter", i)
						.returns(queryFilters.get(i).getStartDate(), from(DatumAuxiliaryCriteria::getStartDate))
						.as("End date (page %d) from expected page filter", i)
						.returns(queryFilters.get(i).getEndDate(), from(DatumAuxiliaryCriteria::getEndDate))
						;
				}
			})
			;
	}

	@Test
	public void multiBatchRange() throws IOException {
		// GIVEN
		final ZonedDateTime startDate = LocalDate.of(2010, 1, 1).atStartOfDay(UTC);
		final ZonedDateTime endDate = LocalDate.of(2010, 2, 1).atStartOfDay(UTC);

		final var datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID, randomLong(), now(),
				randomString(), TEST_DATUM_STREAM_SERVICE_IDENTIFIER, ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		final var config = new BasicInputConfiguration(TEST_USER_ID);
		config.setServiceProps(Map.of(
		// @formatter:off
				DATUM_STREAM_ID_SETTING, datumStream.getConfigId(),
				START_DATE_SETTING, startDate.toInstant().toString(),
				END_DATE_SETTING, endDate.toInstant().toString()
				// @formatter:on
		));
		final var resource = new BasicDatumImportResource(new ByteArrayResource(new byte[0]),
				"application/octet-stream");

		// resolve source IDs
		final Set<String> resolvedSourceIds = Set.of(datumStream.getSourceId());
		given(datumStreamService.datumStreamSourceIds(datumStream)).willReturn(resolvedSourceIds);

		// delete auxiliary, manually capture copies because same instance re-used across invocations
		final List<DatumAuxiliaryCriteria> capturedAuxiliaryFilters = new ArrayList<>(8);
		given(datumAuxiliaryDao.deleteFiltered(any())).will(inv -> {
			var f = inv.getArgument(0, DatumAuxiliaryCriteria.class);
			capturedAuxiliaryFilters.add(new BasicDatumCriteria(f));
			return 0L;
		});

		// request Datum Stream from DAO
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// request datum from service
		final List<List<Datum>> datumPages = new ArrayList<>(8);
		List<Datum> datumList = new ArrayList<>(8);
		ZonedDateTime day = startDate;
		while ( day.isBefore(endDate) ) {
			datumList.add(new GeneralDatum(DatumId.nodeId(datumStream.getObjectId(),
					datumStream.getSourceId(), day.toInstant()), new DatumSamples()));
			day = day.plus(1L, ChronoUnit.DAYS);
			if ( day.getDayOfWeek() == DayOfWeek.MONDAY ) {
				datumPages.add(datumList);
				datumList = new ArrayList<>(8);
			}
		}

		// return some paginated results
		final List<BasicQueryFilter> queryFilters = new ArrayList<>(8);
		queryFilters.add(BasicQueryFilter.ofRange(startDate.toInstant(), endDate.toInstant()));

		var datumStub = given(datumStreamService.datum(same(datumStream), any()));
		for ( List<Datum> datumPage : datumPages ) {
			var nextDay = datumPage.getLast().getTimestamp().plus(1, ChronoUnit.DAYS);
			var nextFilter = BasicQueryFilter.ofRange(nextDay, endDate.toInstant());
			queryFilters.add(nextFilter);
			datumStub = datumStub
					.willReturn(new BasicCloudDatumStreamQueryResult(null, nextFilter, datumPage));
		}
		// return the final non-paginated end result
		datumStub.willReturn(new BasicCloudDatumStreamQueryResult(List.of()));

		// WHEN
		final var progress = new ArrayList<Double>(8);
		final var datum = new ArrayList<GeneralNodeDatum>(8);

		try (var ctx = service.createImportContext(config, resource, (_, progressAmount) -> {
			progress.add(progressAmount);
		})) {
			for ( var d : ctx ) {
				datum.add(d);
			}
		}

		// THEN
		// @formatter:off
		thenDeleteAuxiliaryForQueryRanges(datumStream, resolvedSourceIds, capturedAuxiliaryFilters, queryFilters);

		// query for datum
		then(datumStreamService).should(times(queryFilters.size()))
			.datum(same(datumStream), filterCaptor.capture());

		and.then(filterCaptor.getAllValues())
			.as("Queried for pages using original filter plus all next filters provided in service results")
			.satisfies(filters -> {
				for (int i =0, len = filters.size(); i < len; i++ ) {
					and.then(filters).element(i)
						.as("Start date (page %d) from expected page filter", i)
						.returns(queryFilters.get(i).getStartDate(), from(CloudDatumStreamQueryFilter::getStartDate))
						.as("End date (page %d) from original filter", i)
						.returns(endDate.toInstant(), from(CloudDatumStreamQueryFilter::getEndDate))
						;
				}
			})
			;

		and.then(progress)
			.as("Progress udpated with each datum page")
			.hasSize(datumPages.size())
			;
		for ( int i = 0; i < datumPages.size(); i++ ) {
			Double amount = progress.get(i);
			double overallSeconds = ChronoUnit.SECONDS.between(startDate, endDate);
			BasicQueryFilter usedFilter = queryFilters.get(i+1);
			double expectedSeconds = ChronoUnit.SECONDS.between(startDate.toInstant(), usedFilter.getStartDate());
			and.then(amount)
				.as("Progress amount equals percentage of seconds between overall start and query start dates")
				.isEqualByComparingTo(expectedSeconds / overallSeconds)
				;
		}

		List<Datum> allDatumList = datumPages.stream().flatMap(l -> l.stream()).toList();
		and.then(datum)
			.as("All datum from service are returned")
			.hasSize(allDatumList.size())
			;
		for (int i =0, len = datum.size(); i < len; i++) {
			and.then(datum)
				.element(i)
				.as("Import datum converted from service datum")
				.isEqualTo(DatumUtils.convertGeneralDatum(allDatumList.get(i)))
				;
		}
		// @formatter:on
	}

	@Test
	public void multiBatchRange_nextStartDateRewind() throws IOException {
		// GIVEN
		final ZonedDateTime startDate = LocalDate.of(2010, 1, 1).atStartOfDay(UTC);
		final ZonedDateTime endDate = LocalDate.of(2010, 2, 1).atStartOfDay(UTC);

		final var datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID, randomLong(), now(),
				randomString(), TEST_DATUM_STREAM_SERVICE_IDENTIFIER, ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		final var config = new BasicInputConfiguration(TEST_USER_ID);
		config.setServiceProps(Map.of(
		// @formatter:off
				DATUM_STREAM_ID_SETTING, datumStream.getConfigId(),
				START_DATE_SETTING, startDate.toInstant().toString(),
				END_DATE_SETTING, endDate.toInstant().toString()
				// @formatter:on
		));
		final var resource = new BasicDatumImportResource(new ByteArrayResource(new byte[0]),
				"application/octet-stream");

		// resolve source IDs
		final Set<String> resolvedSourceIds = Set.of(datumStream.getSourceId());
		given(datumStreamService.datumStreamSourceIds(datumStream)).willReturn(resolvedSourceIds);

		// delete auxiliary, manually capture copies because same instance re-used across invocations
		final List<DatumAuxiliaryCriteria> capturedAuxiliaryFilters = new ArrayList<>(8);
		given(datumAuxiliaryDao.deleteFiltered(any())).will(inv -> {
			var f = inv.getArgument(0, DatumAuxiliaryCriteria.class);
			capturedAuxiliaryFilters.add(new BasicDatumCriteria(f));
			return 0L;
		});

		// request Datum Stream from DAO
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// request datum from service
		final List<List<Datum>> datumPages = new ArrayList<>(8);
		List<Datum> datumList = new ArrayList<>(8);

		// return some paginated results
		final List<BasicQueryFilter> queryFilters = new ArrayList<>(8);
		queryFilters.add(BasicQueryFilter.ofRange(startDate.toInstant(), endDate.toInstant()));

		var datumStub = given(datumStreamService.datum(same(datumStream), any()));

		ZonedDateTime queryStart = startDate;
		ZonedDateTime queryEnd = endDate;
		ZonedDateTime day = startDate;
		while ( day.isBefore(endDate) ) {
			datumList.add(new GeneralDatum(DatumId.nodeId(datumStream.getObjectId(),
					datumStream.getSourceId(), day.toInstant()), new DatumSamples()));
			day = day.plus(1L, ChronoUnit.DAYS);
			if ( day.getDayOfWeek() == DayOfWeek.MONDAY ) {
				datumPages.add(datumList);
				queryStart = day;
				var filter = BasicQueryFilter.ofRange(queryStart.toInstant(), queryEnd.toInstant());
				queryFilters.add(filter);
				datumStub = datumStub
						.willReturn(new BasicCloudDatumStreamQueryResult(null, filter, datumList));
				datumList = new ArrayList<>(8);
			}
		}

		// return the final non-paginated end result, with same start date as last page but no results;
		// services like Enphase use their "last reported date" to adjust the next query start date;
		// we expect to abort when this happens
		datumStub.willReturn(
				new BasicCloudDatumStreamQueryResult(null, queryFilters.getLast(), List.of()));

		// WHEN
		final var progress = new ArrayList<Double>(8);
		final var datum = new ArrayList<GeneralNodeDatum>(8);

		// WHEN
		try (var ctx = service.createImportContext(config, resource, (_, progressAmount) -> {
			progress.add(progressAmount);
		})) {
			for ( var d : ctx ) {
				datum.add(d);
			}
		}

		// THEN
		// @formatter:off
		thenDeleteAuxiliaryForQueryRanges(datumStream, resolvedSourceIds, capturedAuxiliaryFilters, queryFilters);

		then(datumStreamService).should(times(queryFilters.size()))
			.datum(same(datumStream), filterCaptor.capture());

		and.then(filterCaptor.getAllValues())
			.as("Queried for pages using original filter plus all next filters provided in service results")
			.satisfies(filters -> {
				for (int i =0, len = filters.size(); i < len; i++ ) {
					and.then(filters).element(i)
						.as("Start date (page %d) from expected page filter", i)
						.returns(queryFilters.get(i).getStartDate(), CloudDatumStreamQueryFilter::getStartDate)
						.as("End date (page %d) from original filter", i)
						.returns(endDate.toInstant(), CloudDatumStreamQueryFilter::getEndDate)
						;
				}
			})
			;

		then(datumStreamService).shouldHaveNoMoreInteractions();

		List<Datum> allDatumList = datumPages.stream().flatMap(l -> l.stream()).toList();
		and.then(datum)
			.as("All datum from service are returned")
			.hasSize(allDatumList.size())
			;
		for (int i =0, len = datum.size(); i < len; i++) {
			and.then(datum)
				.element(i)
				.as("Import datum converted from service datum")
				.isEqualTo(DatumUtils.convertGeneralDatum(allDatumList.get(i)))
				;
		}
		// @formatter:on
	}

	@Test
	public void singleBatchRange_generateAuxiliary() throws IOException {
		// GIVEN
		final ZonedDateTime startDate = LocalDate.of(2010, 1, 1).atStartOfDay(UTC);
		final ZonedDateTime endDate = LocalDate.of(2010, 2, 1).atStartOfDay(UTC);

		final var datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID, randomLong(), now(),
				randomString(), TEST_DATUM_STREAM_SERVICE_IDENTIFIER, ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		final var config = new BasicInputConfiguration(TEST_USER_ID);
		config.setServiceProps(Map.of(
		// @formatter:off
				DATUM_STREAM_ID_SETTING, datumStream.getConfigId(),
				START_DATE_SETTING, startDate.toInstant().toString(),
				END_DATE_SETTING, endDate.toInstant().toString()
				// @formatter:on
		));
		final var resource = new BasicDatumImportResource(new ByteArrayResource(new byte[0]),
				"application/octet-stream");

		// resolve source IDs
		final Set<String> resolvedSourceIds = Set.of(datumStream.getSourceId());
		given(datumStreamService.datumStreamSourceIds(datumStream)).willReturn(resolvedSourceIds);

		// delete auxiliary, manually capture copies because same instance re-used across invocations
		final List<DatumAuxiliaryCriteria> capturedAuxiliaryFilters = new ArrayList<>(8);
		given(datumAuxiliaryDao.deleteFiltered(any())).will(inv -> {
			var f = inv.getArgument(0, DatumAuxiliaryCriteria.class);
			capturedAuxiliaryFilters.add(new BasicDatumCriteria(f));
			return 0L;
		});

		// request Datum Stream from DAO
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// request datum from service
		final List<BasicQueryFilter> queryFilters = new ArrayList<>(8);
		queryFilters.add(BasicQueryFilter.ofRange(startDate.toInstant(), endDate.toInstant()));

		final List<Datum> datumList = new ArrayList<>(8);
		ZonedDateTime day = startDate;
		while ( day.isBefore(endDate) ) {
			datumList.add(new GeneralDatum(DatumId.nodeId(datumStream.getObjectId(),
					datumStream.getSourceId(), day.toInstant()), new DatumSamples()));
			day = day.plus(1L, ChronoUnit.DAYS);
		}

		final List<DatumAuxiliaryRecord> generatedAux = new ArrayList<>(2);
		generatedAux.addAll(
				DatumAuxiliary.createTimeGapValidationRecords(DatumValidationType.TimeGap.getKey(),
						"/foo/bar/prop/val", null, null, startDate.minusDays(2).toInstant(),
						Duration.ofDays(1), datumList.getFirst().datumIdent()));

		// @formatter:off
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(null, null, datumList, generatedAux))
				;
		// @formatter:on

		// persist auxiliary, after looking up stream ID
		final UUID streamId = UUID.randomUUID();
		given(datumStreamMetadataDao.findDatumStreamMetadataIds(any()))
				.willReturn(List.of(new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node,
						datumStream.getObjectId(), datumStream.getSourceId())));

		given(datumAuxiliaryDao.save(any())).will(inv -> {
			return inv.getArgument(0, DatumAuxiliaryEntity.class).getId();
		});

		// WHEN
		final var progress = new ArrayList<Double>(8);
		final var datum = new ArrayList<GeneralNodeDatum>(8);

		// WHEN
		try (var ctx = service.createImportContext(config, resource, (_, progressAmount) -> {
			progress.add(progressAmount);
		})) {
			for ( var d : ctx ) {
				datum.add(d);
			}
		}

		// THEN
		// @formatter:off
		thenDeleteAuxiliaryForQueryRanges(datumStream, resolvedSourceIds, capturedAuxiliaryFilters, queryFilters);

		then(datumStreamService).shouldHaveNoMoreInteractions();

		and.then(filterCaptor.getAllValues())
		.as("Queried for pages using original filter plus all next filters provided in service results")
		.satisfies(filters -> {
			for (int i =0, len = filters.size(); i < len; i++ ) {
				and.then(filters).element(i)
					.as("Start date (page %d) from expected page filter", i)
					.returns(queryFilters.get(i).getStartDate(), from(CloudDatumStreamQueryFilter::getStartDate))
					.as("End date (page %d) from original filter", i)
					.returns(endDate.toInstant(), from(CloudDatumStreamQueryFilter::getEndDate))
					;
			}
		})
		;

		// TODO: validate progress?

		and.then(datum)
			.as("All datum from service are returned")
			.hasSize(datumList.size())
			;
		for (int i =0, len = datum.size(); i < len; i++) {
			and.then(datum)
				.element(i)
				.as("Import datum converted from service datum")
				.isEqualTo(DatumUtils.convertGeneralDatum(datumList.get(i)))
				;
		}

		// datum stream metadata query for expected kind/node/sources
		then(datumStreamMetadataDao).should().findDatumStreamMetadataIds(datumStreamMetadataFilterCaptor.capture());
		and.then(datumStreamMetadataFilterCaptor.getValue())
			.as("Stream metadata filter object kind for Datum Stream kind")
			.returns(datumStream.getKind(), from(ObjectStreamCriteria::getObjectKind))
			.as("Stream metadata filter for Datum Stream node ID")
			.returns(datumStream.getObjectId(), from(ObjectStreamCriteria::getNodeId))
			.as("Stream metadata filter for resolved source IDs")
			.returns(resolvedSourceIds.toArray(String[]::new), from(ObjectStreamCriteria::getSourceIds))
			;

		final String timeGapValidationType = DatumValidationType.TimeGap.getKey();

		then(datumAuxiliaryDao).should(times(2)).save(datumAuxiliaryEntityCaptor.capture());
		and.then(datumAuxiliaryEntityCaptor.getAllValues())
			.allSatisfy(aux -> {
				and.then(aux)
					.as("Auxiliary stream ID resolved from stream metadata query results")
					.returns(streamId, from(DatumAuxiliaryEntity::getStreamId))
					.as("Auxiliary type is Mark")
					.returns(DatumAuxiliaryType.Mark, from(DatumAuxiliaryEntity::getType))
					.extracting(DatumAuxiliaryEntity::getMetadata)
					.as("Mark is generated by SolarNetwork")
					.returns(DatumAuxiliary.GENERATED_BY_SOLARNETWORK, from(e -> e.getInfoString(DatumAuxiliary.GENERATED_BY_META_KEY)))
					.as("Mark type is data validation")
					.returns(DatumAuxiliary.DATA_VALIDATION_TYPE, from(e -> e.getInfoString(DatumAuxiliary.TYPE_META_KEY)))
					.as("Mark sub-types is time-gap")
					.returns(List.of(timeGapValidationType), from(e -> e.getInfo(DatumAuxiliary.SUB_TYPES_META_KEY)))
					;
			})
			.satisfies(records -> {
				for (int i = 0; i < generatedAux.size(); i++ ) {
					final var expected = generatedAux.get(i);
					and.then(records).element(i)
						.as("Generated aux %d timestamp saved as-is", i)
						.returns(expected.getTimestamp(), from(DatumAuxiliaryEntity::getTimestamp))
						.as("Generated aux %d notes saved as-is", i)
						.returns(expected.getNotes(), from(DatumAuxiliaryEntity::getNotes))
						.as("Generated aux %d metadata saved as-is", i)
						.returns(expected.getMetadata(), from(DatumAuxiliaryEntity::getMetadata))
						;

				}
			})
			;

		// @formatter:on
	}

}
