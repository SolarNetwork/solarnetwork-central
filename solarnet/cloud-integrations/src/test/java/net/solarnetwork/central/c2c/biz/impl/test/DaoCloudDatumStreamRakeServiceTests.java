/* ==================================================================
 * DaoCloudDatumStreamRakeServiceTests.java - 21/09/2025 2:19:52 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.CONFIG_ID_DATA_KEY;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.CONFIG_SUB_ID_DATA_KEY;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.INTEGRATION_RAKE_TAGS;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Claimed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.DaoCloudDatumStreamRakeService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link DaoCloudDatumStreamRakeService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoCloudDatumStreamRakeServiceTests {

	private static final Long TEST_USER_ID = randomLong();
	private static final String TEST_DATUM_STREAM_SERVICE_IDENTIFIER = randomString();

	private Clock clock;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private CloudDatumStreamRakeTaskDao taskDao;

	@Mock
	private CloudDatumStreamPollTaskDao pollTaskDao;

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private CloudDatumStreamService datumStreamService;

	@Mock
	private ExecutorService executor;

	@Mock
	private DatumProcessor fluxProcessor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamQueryFilter> queryFilterCaptor;

	@Captor
	private ArgumentCaptor<DatumCriteria> datumFilterCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamRakeTaskEntity> taskCaptor;

	@Captor
	private ArgumentCaptor<Datum> datumCaptor;

	@Captor
	private ArgumentCaptor<Identity<GeneralNodeDatumPK>> generalNodeDatumCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> logEventCaptor;

	private DaoCloudDatumStreamRakeService service;

	@BeforeEach
	public void setup() {
		// clock for "now" is fixed just after the top of the day, to simulate executing
		// a task just after it's scheduled time
		clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.DAYS).plusSeconds(1), ZoneOffset.UTC);

		var datumStreamServices = Map.of(TEST_DATUM_STREAM_SERVICE_IDENTIFIER, datumStreamService);
		service = new DaoCloudDatumStreamRakeService(clock, userEventAppenderBiz, nodeOwnershipDao,
				taskDao, pollTaskDao, datumStreamDao, datumDao, executor, datumStreamServices::get);

	}

	@Test
	public void claimTask() {
		// GIVEN
		final CloudDatumStreamRakeTaskEntity entity = new CloudDatumStreamRakeTaskEntity(TEST_USER_ID,
				randomLong());

		given(taskDao.claimQueuedTask()).willReturn(entity);

		// WHEN
		CloudDatumStreamRakeTaskEntity result = service.claimQueuedTask();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void claimTask_null() {
		given(taskDao.claimQueuedTask()).willReturn(null);

		// WHEN
		CloudDatumStreamRakeTaskEntity result = service.claimQueuedTask();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Null result from DAO returned")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void executeTask_noDifference() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamRakeTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamRakeTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant sod = clock.instant().truncatedTo(ChronoUnit.DAYS);

		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setSchedule("0 0/5 * * * *");
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		// look up datum stream associated with task
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// load poll task to check its start date
		final CloudDatumStreamPollTaskEntity pollTask = new CloudDatumStreamPollTaskEntity(
				datumStream.getId());
		pollTask.setStartAt(clock.instant().truncatedTo(HOURS));
		given(pollTaskDao.get(datumStream.getId())).willReturn(pollTask);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data associated with service configured on datum stream
		final var datum1 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod),
				new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 23456L), null));
		final var datum2 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod.plus(1, HOURS)),
				new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 34567L), null));
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(List.of(datum1, datum2)));

		// query for existing datum with same time range
		final BasicObjectDatumStreamMetadata meta1 = new BasicObjectDatumStreamMetadata(
				UUID.randomUUID(), "UTC", ObjectDatumKind.Node, datumStream.getObjectId(),
				datumStream.getSourceId(), new String[] { "watts" }, new String[] { "wattHours" }, null);
		final var d1 = new DatumEntity(meta1.getStreamId(), datum1.getTimestamp(), now(),
				DatumProperties.propertiesFrom(datum1, meta1));
		final var d2 = new DatumEntity(meta1.getStreamId(), datum2.getTimestamp(), now(),
				DatumProperties.propertiesFrom(datum2, meta1));
		given(datumDao.findFiltered(any())).willReturn(new BasicObjectDatumStreamFilterResults<>(
				Map.of(meta1.getStreamId(), meta1), List.of(d1, d2)));

		// update task details
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamRakeTaskEntity(datumStream.getId());
		task.setDatumStreamId(datumStream.getConfigId());
		task.setState(Claimed);
		task.setExecuteAt(sod);
		task.setOffset(Period.ofDays(1));

		Future<CloudDatumStreamRakeTaskEntity> result = service.executeTask(task);
		CloudDatumStreamRakeTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(datumStreamService).should().datum(same(datumStream), queryFilterCaptor.capture());
		and.then(queryFilterCaptor.getValue())
			.as("The query start date is the day offset from 'now'")
			.returns(sod.minus(1, DAYS), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("The query end date is the current date")
			.returns(sod, from(CloudDatumStreamQueryFilter::getEndDate))
			;

		then(datumDao).should().findFiltered(datumFilterCaptor.capture());
		and.then(datumFilterCaptor.getValue())
			.as("The existing datum query is for stream object ID")
			.returns(meta1.getObjectId(), from(DatumCriteria::getNodeId))
			.as("The existing datum query is for the stream source ID")
			.returns(meta1.getSourceId(), from(DatumCriteria::getSourceId))
			.as("The existing datum query start date is the same as in the query filter")
			.returns(sod.minus(1, DAYS), from(DatumCriteria::getStartDate))
			.as("The existing datum query end date is the same as in the query filter")
			.returns(sod, from(DatumCriteria::getEndDate))
			;

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again")
			.returns(Queued, from(CloudDatumStreamRakeTaskEntity::getState))
			.as("Update task execute date to start of 'tomorrow'")
			.returns(sod.plus(1, DAYS), from(CloudDatumStreamRakeTaskEntity::getExecuteAt))
			.as("No message generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getMessage))
			.as("No service properties generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getServiceProperties))
			;

		then(userEventAppenderBiz).should(times(2)).addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Events for start/reset generated")
			.hasSize(2)
			.satisfies(events -> {
				and.then(events).element(0)
					.as("Task start event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"configId", datumStream.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt()),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(1, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod),
							"startedAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant())
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;

				and.then(events).element(1)
					.as("Task success reset event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"configId", datumStream.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(sod.plus(1, DAYS)),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(1, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod),
							"datumUpdateCount", 0
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;
			})
			;

		and.then(resultTask)
			.as("Result task is same as passed to DAO for update")
			.isSameAs(taskCaptor.getValue())
			;

		// @formatter:on
	}

	@Test
	public void executeTask_difference() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamRakeTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamRakeTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant sod = clock.instant().truncatedTo(ChronoUnit.DAYS);

		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setSchedule("0 0/5 * * * *");
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		// look up datum stream associated with task
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// load poll task to check its start date
		final CloudDatumStreamPollTaskEntity pollTask = new CloudDatumStreamPollTaskEntity(
				datumStream.getId());
		pollTask.setStartAt(clock.instant().truncatedTo(HOURS));
		given(pollTaskDao.get(datumStream.getId())).willReturn(pollTask);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data associated with service configured on datum stream
		final var datum1 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod),
				new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 23456L), null));
		final var datum2 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod.plus(1, HOURS)),
				new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 34567L), null));
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(List.of(datum1, datum2)));

		// query for existing datum with same time range
		final BasicObjectDatumStreamMetadata meta1 = new BasicObjectDatumStreamMetadata(
				UUID.randomUUID(), "UTC", ObjectDatumKind.Node, datumStream.getObjectId(),
				datumStream.getSourceId(), new String[] { "watts" }, new String[] { "wattHours" }, null);
		final var d1 = new DatumEntity(meta1.getStreamId(), datum1.getTimestamp(), now(),
				DatumProperties.propertiesFrom(datum1, meta1));
		final var d2 = new DatumEntity(meta1.getStreamId(), datum2.getTimestamp(), now(),
				DatumProperties.propertiesFrom(datum2, meta1));

		// now tweak wattHours value to be different from what cloud says
		d2.getProperties().setAccumulating(decimalArray("0"));

		given(datumDao.findFiltered(any())).willReturn(new BasicObjectDatumStreamFilterResults<>(
				Map.of(meta1.getStreamId(), meta1), List.of(d1, d2)));

		// persist datum with difference
		given(datumDao.store(any(Datum.class))).willReturn(d2.getId());

		// update task details
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamRakeTaskEntity(datumStream.getId());
		task.setDatumStreamId(datumStream.getConfigId());
		task.setState(Claimed);
		task.setExecuteAt(sod);
		task.setOffset(Period.ofDays(1));

		Future<CloudDatumStreamRakeTaskEntity> result = service.executeTask(task);
		CloudDatumStreamRakeTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(datumStreamService).should().datum(same(datumStream), queryFilterCaptor.capture());
		and.then(queryFilterCaptor.getValue())
			.as("The query start date is the day offset from 'now'")
			.returns(sod.minus(1, DAYS), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("The query end date is the current date")
			.returns(sod, from(CloudDatumStreamQueryFilter::getEndDate))
			;

		then(datumDao).should().findFiltered(datumFilterCaptor.capture());
		and.then(datumFilterCaptor.getValue())
			.as("The existing datum query is for stream object ID")
			.returns(meta1.getObjectId(), from(DatumCriteria::getNodeId))
			.as("The existing datum query is for the stream source ID")
			.returns(meta1.getSourceId(), from(DatumCriteria::getSourceId))
			.as("The existing datum query start date is the same as in the query filter")
			.returns(sod.minus(1, DAYS), from(DatumCriteria::getStartDate))
			.as("The existing datum query end date is the same as in the query filter")
			.returns(sod, from(DatumCriteria::getEndDate))
			;

		then(datumDao).should().store(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Datum with difference persisted")
			.isSameAs(datum2)
			;

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again")
			.returns(Queued, from(CloudDatumStreamRakeTaskEntity::getState))
			.as("Update task execute date to start of 'tomorrow'")
			.returns(sod.plus(1, DAYS), from(CloudDatumStreamRakeTaskEntity::getExecuteAt))
			.as("No message generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getMessage))
			.as("No service properties generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getServiceProperties))
			;

		then(userEventAppenderBiz).should(times(2)).addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Events for start/reset generated")
			.hasSize(2)
			.satisfies(events -> {
				and.then(events).element(0)
					.as("Task start event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt()),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(1, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod),
							"startedAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant())
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;

				and.then(events).element(1)
					.as("Task success reset event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(sod.plus(1, DAYS)),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(1, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod),
							"datumUpdateCount", 1,
							"datumUpdateCountBySource", Map.of(
									datum2.getSourceId(), 1
									)
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;
			})
			;

		and.then(resultTask)
			.as("Result task is same as passed to DAO for update")
			.isSameAs(taskCaptor.getValue())
			;

		// @formatter:on
	}

	@Test
	public void executeTask_missing() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamRakeTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamRakeTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant sod = clock.instant().truncatedTo(ChronoUnit.DAYS);

		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setSchedule("0 0/5 * * * *");
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		// look up datum stream associated with task
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// load poll task to check its start date
		final CloudDatumStreamPollTaskEntity pollTask = new CloudDatumStreamPollTaskEntity(
				datumStream.getId());
		pollTask.setStartAt(clock.instant().truncatedTo(HOURS));
		given(pollTaskDao.get(datumStream.getId())).willReturn(pollTask);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data associated with service configured on datum stream
		final var datum1 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod),
				new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 23456L), null));
		final var datum2 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod.plus(1, HOURS)),
				new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 34567L), null));
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(List.of(datum1, datum2)));

		// query for existing datum with same time range
		final BasicObjectDatumStreamMetadata meta1 = new BasicObjectDatumStreamMetadata(
				UUID.randomUUID(), "UTC", ObjectDatumKind.Node, datumStream.getObjectId(),
				datumStream.getSourceId(), new String[] { "watts" }, new String[] { "wattHours" }, null);
		final var d1 = new DatumEntity(meta1.getStreamId(), datum1.getTimestamp(), now(),
				DatumProperties.propertiesFrom(datum1, meta1));
		final var d2 = new DatumEntity(meta1.getStreamId(), datum2.getTimestamp(), now(),
				DatumProperties.propertiesFrom(datum2, meta1));

		given(datumDao.findFiltered(any())).willReturn(new BasicObjectDatumStreamFilterResults<>(
				Map.of(meta1.getStreamId(), meta1), List.of(d1)));

		// persist datum with difference
		given(datumDao.store(any(Datum.class))).willReturn(d2.getId());

		// update task details
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamRakeTaskEntity(datumStream.getId());
		task.setDatumStreamId(datumStream.getConfigId());
		task.setState(Claimed);
		task.setExecuteAt(sod);
		task.setOffset(Period.ofDays(1));

		Future<CloudDatumStreamRakeTaskEntity> result = service.executeTask(task);
		CloudDatumStreamRakeTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(datumStreamService).should().datum(same(datumStream), queryFilterCaptor.capture());
		and.then(queryFilterCaptor.getValue())
			.as("The query start date is the day offset from 'now'")
			.returns(sod.minus(1, DAYS), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("The query end date is the current date")
			.returns(sod, from(CloudDatumStreamQueryFilter::getEndDate))
			;

		then(datumDao).should().findFiltered(datumFilterCaptor.capture());
		and.then(datumFilterCaptor.getValue())
			.as("The existing datum query is for stream object ID")
			.returns(meta1.getObjectId(), from(DatumCriteria::getNodeId))
			.as("The existing datum query is for the stream source ID")
			.returns(meta1.getSourceId(), from(DatumCriteria::getSourceId))
			.as("The existing datum query start date is the same as in the query filter")
			.returns(sod.minus(1, DAYS), from(DatumCriteria::getStartDate))
			.as("The existing datum query end date is the same as in the query filter")
			.returns(sod, from(DatumCriteria::getEndDate))
			;

		then(datumDao).should().store(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Datum with difference persisted")
			.isSameAs(datum2)
			;

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again")
			.returns(Queued, from(CloudDatumStreamRakeTaskEntity::getState))
			.as("Update task execute date to start of 'tomorrow'")
			.returns(sod.plus(1, DAYS), from(CloudDatumStreamRakeTaskEntity::getExecuteAt))
			.as("No message generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getMessage))
			.as("No service properties generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getServiceProperties))
			;

		then(userEventAppenderBiz).should(times(2)).addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Events for start/reset generated")
			.hasSize(2)
			.satisfies(events -> {
				and.then(events).element(0)
					.as("Task start event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt()),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(1, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod),
							"startedAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant())
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;

				and.then(events).element(1)
					.as("Task success reset event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(sod.plus(1, DAYS)),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(1, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod),
							"datumUpdateCount", 1,
							"datumUpdateCountBySource", Map.of(
									datum2.getSourceId(), 1
									)
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;
			})
			;

		and.then(resultTask)
			.as("Result task is same as passed to DAO for update")
			.isSameAs(taskCaptor.getValue())
			;

		// @formatter:on
	}

	@Test
	public void executeTask_iterateUntilSame() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamRakeTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamRakeTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant sod = clock.instant().truncatedTo(ChronoUnit.DAYS);

		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setSchedule("0 0/5 * * * *");
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		// look up datum stream associated with task
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// load poll task to check its start date
		final CloudDatumStreamPollTaskEntity pollTask = new CloudDatumStreamPollTaskEntity(
				datumStream.getId());
		pollTask.setStartAt(clock.instant().truncatedTo(HOURS));
		given(pollTaskDao.get(datumStream.getId())).willReturn(pollTask);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data associated with service configured on datum stream, over 3 iterations (days)
		final List<Datum> cloudDatum1 = List.of(
				new GeneralDatum(nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod),
						new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 23456L), null)),
				new GeneralDatum(
						nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod.plus(1, HOURS)),
						new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 34567L), null)));
		final List<Datum> cloudDatum2 = List.of(
				new GeneralDatum(
						nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod.plus(1, DAYS)),
						new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 45678L), null)),
				new GeneralDatum(
						nodeId(datumStream.getObjectId(), datumStream.getSourceId(),
								sod.plus(1, DAYS).plus(1, HOURS)),
						new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 56789L), null)));
		final List<Datum> cloudDatum3 = List.of(
				new GeneralDatum(
						nodeId(datumStream.getObjectId(), datumStream.getSourceId(), sod.plus(2, DAYS)),
						new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 67890L), null)),
				new GeneralDatum(
						nodeId(datumStream.getObjectId(), datumStream.getSourceId(),
								sod.plus(2, DAYS).plus(1, HOURS)),
						new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 78901L), null)));
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(cloudDatum1))
				.willReturn(new BasicCloudDatumStreamQueryResult(cloudDatum2))
				.willReturn(new BasicCloudDatumStreamQueryResult(cloudDatum3));

		// query for existing datum with same time range
		final BasicObjectDatumStreamMetadata meta1 = new BasicObjectDatumStreamMetadata(
				UUID.randomUUID(), "UTC", ObjectDatumKind.Node, datumStream.getObjectId(),
				datumStream.getSourceId(), new String[] { "watts" }, new String[] { "wattHours" }, null);
		final List<net.solarnetwork.central.datum.v2.domain.Datum> existingDatum1 = List.of(); // missing day 1
		final List<net.solarnetwork.central.datum.v2.domain.Datum> existingDatum2 = List.of(); // missing day 2
		final List<net.solarnetwork.central.datum.v2.domain.Datum> existingDatum3 = cloudDatum3.stream()
				.map(d -> (net.solarnetwork.central.datum.v2.domain.Datum) new DatumEntity(
						meta1.getStreamId(), d.getTimestamp(), now(),
						DatumProperties.propertiesFrom(d, meta1)))
				.toList();

		given(datumDao.findFiltered(any()))
				.willReturn(new BasicObjectDatumStreamFilterResults<>(Map.of(meta1.getStreamId(), meta1),
						existingDatum1))
				.willReturn(new BasicObjectDatumStreamFilterResults<>(Map.of(meta1.getStreamId(), meta1),
						existingDatum2))
				.willReturn(new BasicObjectDatumStreamFilterResults<>(Map.of(meta1.getStreamId(), meta1),
						existingDatum3));

		// persist datum with difference (missing)
		final List<Datum> allCloudDatum = List.of(cloudDatum1, cloudDatum2).stream()
				.flatMap(l -> l.stream()).toList();
		var datumDaoStoreGiven = given(datumDao.store(any(Datum.class)));
		for ( Datum d : allCloudDatum ) {
			datumDaoStoreGiven = datumDaoStoreGiven
					.willReturn(new DatumPK(meta1.getStreamId(), d.getTimestamp()));
		}

		// update task details
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamRakeTaskEntity(datumStream.getId());
		task.setDatumStreamId(datumStream.getConfigId());
		task.setState(Claimed);
		task.setExecuteAt(sod);
		task.setOffset(Period.ofDays(7));

		Future<CloudDatumStreamRakeTaskEntity> result = service.executeTask(task);
		CloudDatumStreamRakeTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(datumStreamService).should(times(3)).datum(same(datumStream), queryFilterCaptor.capture());
		and.then(queryFilterCaptor.getAllValues())
			.satisfies(l -> {
				and.then(l)
					.as("Iteration 1")
					.element(0)
					.as("The query start date is the day offset from 'now'")
					.returns(sod.minus(7, DAYS), from(CloudDatumStreamQueryFilter::getStartDate))
					.as("The query end date is 1 day after start date")
					.returns(sod.minus(6, DAYS), from(CloudDatumStreamQueryFilter::getEndDate))
					;
				and.then(l)
					.as("Iteration 2")
					.element(1)
					.as("The query start date is the start of next iteration day")
					.returns(sod.minus(6, DAYS), from(CloudDatumStreamQueryFilter::getStartDate))
					.as("The query end date is 1 day after start date")
					.returns(sod.minus(5, DAYS), from(CloudDatumStreamQueryFilter::getEndDate))
					;
				and.then(l)
					.as("Iteration 3")
					.element(2)
					.as("The query start date is the start of next iteration day")
					.returns(sod.minus(5, DAYS), from(CloudDatumStreamQueryFilter::getStartDate))
					.as("The query end date is 1 day after start date")
					.returns(sod.minus(4, DAYS), from(CloudDatumStreamQueryFilter::getEndDate))
					;
			})
			;

		then(datumDao).should(times(3)).findFiltered(datumFilterCaptor.capture());
		and.then(datumFilterCaptor.getAllValues())
			.allSatisfy(c -> {
				and.then(c)
					.as("The existing datum query is for stream object ID")
					.returns(meta1.getObjectId(), from(DatumCriteria::getNodeId))
					.as("The existing datum query is for the stream source ID")
					.returns(meta1.getSourceId(), from(DatumCriteria::getSourceId))
					;
			})
			.satisfies(l -> {
				and.then(l)
					.as("Iteration 1")
					.element(0)
					.as("The existing datum query start date is the same as in the query filter")
					.returns(sod.minus(7, DAYS), from(DatumCriteria::getStartDate))
					.as("The existing datum query end date is the same as in the query filter")
					.returns(sod.minus(6, DAYS), from(DatumCriteria::getEndDate))
					;
				and.then(l)
					.as("Iteration 2")
					.element(1)
					.as("The existing datum query start date is the same as in the query filter")
					.returns(sod.minus(6, DAYS), from(DatumCriteria::getStartDate))
					.as("The existing datum query end date is the same as in the query filter")
					.returns(sod.minus(5, DAYS), from(DatumCriteria::getEndDate))
					;
				and.then(l)
					.as("Iteration 3")
					.element(2)
					.as("The existing datum query start date is the same as in the query filter")
					.returns(sod.minus(5, DAYS), from(DatumCriteria::getStartDate))
					.as("The existing datum query end date is the same as in the query filter")
					.returns(sod.minus(4, DAYS), from(DatumCriteria::getEndDate))
					;
			})
			;

		then(datumDao).should(times(4)).store(datumCaptor.capture());
		and.then(datumCaptor.getAllValues())
			.as("Missing datum persisted")
			.hasSize(4)
			;

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again")
			.returns(Queued, from(CloudDatumStreamRakeTaskEntity::getState))
			.as("Update task execute date to start of 'tomorrow'")
			.returns(sod.plus(1, DAYS), from(CloudDatumStreamRakeTaskEntity::getExecuteAt))
			.as("No message generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getMessage))
			.as("No service properties generated for successful execution")
			.returns(null, from(CloudDatumStreamRakeTaskEntity::getServiceProperties))
			;

		then(userEventAppenderBiz).should(times(4)).addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Events for 3 iterations + final result generated")
			.hasSize(4)
			.satisfies(events -> {
				and.then(events)
					.as("Task iteration 1 start event generated")
					.element(0)
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt()),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(7, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(6, DAYS)),
							"startedAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant())
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;

				and.then(events)
					.as("Task iteration 2 start event generated")
					.element(1)
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt()),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(6, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(5, DAYS)),
							"startedAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant())
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;

				and.then(events)
					.as("Task iteration 3 start event generated")
					.element(2)
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt()),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(5, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(4, DAYS)),
							"startedAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant())
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;


				and.then(events).element(3)
					.as("Task success reset event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(INTEGRATION_RAKE_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, datumStream.getConfigId(),
							CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(sod.plus(1, DAYS)),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(7, DAYS)),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(sod.minus(4, DAYS)),
							"datumUpdateCount", 4,
							"datumUpdateCountBySource", Map.of(
									datumStream.getSourceId(), 4
									)
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;
			})
			;

		and.then(resultTask)
			.as("Result task is same as passed to DAO for update")
			.isSameAs(taskCaptor.getValue())
			;

		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void executeTask_shutdown() throws Exception {
		// GIVEN
		// submit task
		final var rejectedException = new RejectedExecutionException("Executor is shut down.");
		given(executor.submit(any(Callable.class))).willThrow(rejectedException);

		final Instant hour = clock.instant().truncatedTo(ChronoUnit.HOURS);

		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setSchedule("0 0/5 * * * *");
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		// update task state to "queued"
		given(taskDao.updateTaskState(datumStream.getId(), Queued, Claimed)).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamRakeTaskEntity(datumStream.getId());
		task.setDatumStreamId(datumStream.getConfigId());
		task.setState(Claimed);
		task.setExecuteAt(hour);
		task.setOffset(Period.ofDays(1));

		// THEN
		// @formatter:off
		and.thenThrownBy(() -> service.executeTask(task), "Task fails to execute")
			.as("The exception cause is the one thrown by the submit() call")
			.isSameAs(rejectedException)
			;
		// @formatter:on
	}

	@Test
	public void executeTask_afterPollStartDate() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamRakeTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamRakeTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant sod = clock.instant().truncatedTo(ChronoUnit.DAYS);

		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(randomLong());
		datumStream.setServiceIdentifier(TEST_DATUM_STREAM_SERVICE_IDENTIFIER);
		datumStream.setSchedule("0 0/5 * * * *");
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(randomLong());
		datumStream.setSourceId(randomString());

		// look up datum stream associated with task
		given(datumStreamDao.get(datumStream.getId())).willReturn(datumStream);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// load poll task to check its start date
		final CloudDatumStreamPollTaskEntity pollTask = new CloudDatumStreamPollTaskEntity(
				datumStream.getId());
		pollTask.setStartAt(sod.minus(1, DAYS));
		given(pollTaskDao.get(datumStream.getId())).willReturn(pollTask);

		// update task state to "queued" and stop because execute date after poll start date
		given(taskDao.updateTask(any(), eq(Claimed))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamRakeTaskEntity(datumStream.getId());
		task.setDatumStreamId(datumStream.getConfigId());
		task.setState(Claimed);
		task.setExecuteAt(sod);
		task.setOffset(Period.ofDays(1));

		Future<CloudDatumStreamRakeTaskEntity> result = service.executeTask(task);
		CloudDatumStreamRakeTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Claimed));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again")
			.returns(Queued, from(CloudDatumStreamRakeTaskEntity::getState))
			.as("Update task execute date to start of 'tomorrow'")
			.returns(sod.plus(1, DAYS), from(CloudDatumStreamRakeTaskEntity::getExecuteAt))
			.as("Message generated for failed execution")
			.returns("Rake task date is after poll task start.", from(CloudDatumStreamRakeTaskEntity::getMessage))
			.as("Service properties generated for failed execution")
			.returns(Map.of(
					CONFIG_SUB_ID_DATA_KEY, task.getConfigId(),
					"endDate", sod,
					"startDate", pollTask.getStartAt()
				), from(CloudDatumStreamRakeTaskEntity::getServiceProperties))
			;

		and.then(resultTask)
			.as("Result task is same as passed to DAO for update")
			.isSameAs(taskCaptor.getValue())
			;

		// @formatter:on
	}

}
