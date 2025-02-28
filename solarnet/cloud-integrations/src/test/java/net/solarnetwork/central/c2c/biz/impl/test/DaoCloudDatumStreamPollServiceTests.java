/* ==================================================================
 * DaoCloudDatumStreamPollServiceTests.java - 11/10/2024 7:12:23 am
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
import static net.solarnetwork.central.c2c.biz.impl.DaoCloudDatumStreamPollService.DEFAULT_DATUM_STREAM_SETTINGS;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Claimed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.DaoCloudDatumStreamPollService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Test cases for the {@link DaoCloudDatumStreamPollService} class.
 *
 * @author matt
 * @version 1.5
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoCloudDatumStreamPollServiceTests {

	private static final Long TEST_USER_ID = randomLong();
	private static final String TEST_DATUM_STREAM_SERVICE_IDENTIFIER = randomString();

	private Clock clock;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private CloudDatumStreamPollTaskDao taskDao;

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamSettingsEntityDao datumStreamSettingsDao;

	@Mock
	private DatumWriteOnlyDao datumDao;

	@Mock
	private CloudDatumStreamService datumStreamService;

	@Mock
	private ExecutorService executor;

	@Mock
	private DatumProcessor fluxProcessor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamQueryFilter> queryFilterCaptor;

	@Captor
	private ArgumentCaptor<CloudDatumStreamPollTaskEntity> taskCaptor;

	@Captor
	private ArgumentCaptor<Datum> datumCaptor;

	@Captor
	private ArgumentCaptor<Identity<GeneralNodeDatumPK>> generalNodeDatumCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> logEventCaptor;

	private DaoCloudDatumStreamPollService service;

	@BeforeEach
	public void setup() {
		// clock for "now" is fixed just after the top of the hour, to simulate executing
		// a task just after it's scheduled time
		clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS).plusSeconds(1), ZoneOffset.UTC);

		var datumStreamServices = Map.of(TEST_DATUM_STREAM_SERVICE_IDENTIFIER, datumStreamService);
		service = new DaoCloudDatumStreamPollService(clock, userEventAppenderBiz, nodeOwnershipDao,
				taskDao, datumStreamDao, datumStreamSettingsDao, datumDao, executor,
				datumStreamServices::get);
		service.setFluxPublisher(fluxProcessor);

	}

	@Test
	public void claimTask() {
		// GIVEN
		final CloudDatumStreamPollTaskEntity entity = new CloudDatumStreamPollTaskEntity(TEST_USER_ID,
				randomLong());

		given(taskDao.claimQueuedTask()).willReturn(entity);

		// WHEN
		CloudDatumStreamPollTaskEntity result = service.claimQueuedTask();

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
		CloudDatumStreamPollTaskEntity result = service.claimQueuedTask();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Null result from DAO returned")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void executeTask() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamPollTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamPollTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant hour = clock.instant().truncatedTo(ChronoUnit.HOURS);

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

		// resolve datum stream settings
		given(datumStreamSettingsDao.resolveSettings(TEST_USER_ID, datumStream.getConfigId(),
				DEFAULT_DATUM_STREAM_SETTINGS)).willReturn(DEFAULT_DATUM_STREAM_SETTINGS);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data associated with service configured on datum stream
		// here we return a datum for "5 min ago"
		final Datum datum1 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), hour.minusSeconds(300)),
				new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 23456L), null));
		final Datum datum2 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), hour.minusSeconds(120)),
				new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 34567L), null));
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(List.of(datum1, datum2)));

		// persist datum
		final var streamId = UUID.randomUUID();
		final var datumId1 = new DatumPK(streamId, datum1.getTimestamp());
		final var datumId2 = new DatumPK(streamId, datum2.getTimestamp());
		given(datumDao.store(any(Datum.class))).willReturn(datumId1, datumId2);

		// update task details
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamPollTaskEntity(datumStream.getId());
		task.setState(Claimed);
		task.setExecuteAt(hour);
		task.setStartAt(hour.minusSeconds(300));

		Future<CloudDatumStreamPollTaskEntity> result = service.executeTask(task);
		CloudDatumStreamPollTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(datumStreamService).should().datum(same(datumStream), queryFilterCaptor.capture());
		and.then(queryFilterCaptor.getValue())
			.as("The query start date is the startAt of the task")
			.returns(task.getStartAt(), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("The query end date is the current date")
			.returns(clock.instant(), from(CloudDatumStreamQueryFilter::getEndDate))
			;

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again")
			.returns(Queued, from(CloudDatumStreamPollTaskEntity::getState))
			.as("Update task execute date to next time based on configuration schedule (every 5min)")
			.returns(task.getExecuteAt().plusSeconds(300), from(CloudDatumStreamPollTaskEntity::getExecuteAt))
			.as("Update task start date to highest date of datum captured")
			.returns(datum2.getTimestamp(), from(CloudDatumStreamPollTaskEntity::getStartAt))
			.as("No message generated for successful execution")
			.returns(null, from(CloudDatumStreamPollTaskEntity::getMessage))
			.as("No service properties generated for successful execution")
			.returns(null, from(CloudDatumStreamPollTaskEntity::getServiceProperties))
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
					.returns(CloudIntegrationsUserEvents.POLL_TAGS, from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							"configId", datumStream.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt()),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(task.getStartAt()),
							"endAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant()),
							"startedAt", ISO_DATE_TIME_ALT_UTC.format(clock.instant())
						), from(e -> JsonUtils.getStringMap(e.getData())))
					;

				and.then(events).element(1)
					.as("Task success reset event generated")
					.isNotNull()
					.as("Poll tags provided in event")
					.returns(CloudIntegrationsUserEvents.POLL_TAGS, from(LogEventInfo::getTags))
					.as("Task dates provided in event data")
					.returns(Map.of(
							"configId", datumStream.getConfigId(),
							"executeAt", ISO_DATE_TIME_ALT_UTC.format(task.getExecuteAt().plusSeconds(300)),
							"startAt", ISO_DATE_TIME_ALT_UTC.format(datum2.getTimestamp())
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
	public void executeTask_notNodeOwner() throws Exception {
		// GIVEN
		var future = new CompletableFuture<CloudDatumStreamPollTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamPollTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant hour = clock.instant().truncatedTo(ChronoUnit.HOURS);

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

		// resolve datum stream settings
		given(datumStreamSettingsDao.resolveSettings(TEST_USER_ID, datumStream.getConfigId(),
				DEFAULT_DATUM_STREAM_SETTINGS)).willReturn(DEFAULT_DATUM_STREAM_SETTINGS);

		// verify node ownership (returning different user, so not owner)
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), -1L, "NZ", UTC,
				true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// update task details for ownerhip check failure
		given(taskDao.updateTask(any(), eq(Claimed))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamPollTaskEntity(datumStream.getId());
		task.setState(Claimed);
		task.setExecuteAt(hour);
		task.setStartAt(hour.minusSeconds(300));

		Future<CloudDatumStreamPollTaskEntity> result = service.executeTask(task);
		CloudDatumStreamPollTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Claimed));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Completed to signal error")
			.returns(Completed, from(CloudDatumStreamPollTaskEntity::getState))
			.as("Task execute date is unchanged")
			.returns(task.getExecuteAt(), from(CloudDatumStreamPollTaskEntity::getExecuteAt))
			.as("Task start date is unchanged")
			.returns(task.getStartAt(), from(CloudDatumStreamPollTaskEntity::getStartAt))
			.as("Update task with error details")
			.satisfies(t -> {
				and.then(t.getMessage())
					.as("Message generated for ownership failure")
					.containsIgnoringCase("denied")
					;
				and.then(t.getServiceProps())
					.as("Offending node ID provided in error data")
					.containsEntry(CloudIntegrationsUserEvents.SOURCE_DATA_KEY, datumStream.getObjectId())
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
	public void executeTask_produceDatumWithObjectIdDifferentFromConfiguration() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamPollTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamPollTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant hour = clock.instant().truncatedTo(ChronoUnit.HOURS);

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

		// resolve datum stream settings
		given(datumStreamSettingsDao.resolveSettings(TEST_USER_ID, datumStream.getConfigId(),
				DEFAULT_DATUM_STREAM_SETTINGS)).willReturn(DEFAULT_DATUM_STREAM_SETTINGS);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data BUT return node ID != datumStream.objectId, which should not be allowed
		final Datum datum1 = new GeneralDatum(
				nodeId(randomLong(), datumStream.getSourceId(), hour.minusSeconds(300)),
				new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 23456L), null));
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(List.of(datum1)));

		// update task details after object ID check failure
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamPollTaskEntity(datumStream.getId());
		task.setState(Claimed);
		task.setExecuteAt(hour);
		task.setStartAt(hour.minusSeconds(300));

		Future<CloudDatumStreamPollTaskEntity> result = service.executeTask(task);
		CloudDatumStreamPollTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(datumStreamService).should().datum(same(datumStream), queryFilterCaptor.capture());
		and.then(queryFilterCaptor.getValue())
			.as("The query start date is the startAt of the task")
			.returns(task.getStartAt(), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("The query end date is the current date")
			.returns(clock.instant(), from(CloudDatumStreamQueryFilter::getEndDate))
			;

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Completed to signal error")
			.returns(Completed, from(CloudDatumStreamPollTaskEntity::getState))
			.as("Task execute date is unchanged")
			.returns(task.getExecuteAt(), from(CloudDatumStreamPollTaskEntity::getExecuteAt))
			.as("Task start date is unchanged")
			.returns(task.getStartAt(), from(CloudDatumStreamPollTaskEntity::getStartAt))
			.as("Update task with error details")
			.satisfies(t -> {
				and.then(t.getMessage())
					.as("Message generated for ownership failure")
					.containsIgnoringCase("denied")
					;
				and.then(t.getServiceProps())
					.as("Offending node ID provided in error data")
					.containsEntry(CloudIntegrationsUserEvents.SOURCE_DATA_KEY, datum1.getObjectId())
					;
				and.then(t.getServiceProps())
					.as("Expected node ID provided in error data")
					.containsEntry("expected", datumStream.getObjectId())
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
		var task = new CloudDatumStreamPollTaskEntity(datumStream.getId());
		task.setState(Claimed);
		task.setExecuteAt(hour);
		task.setStartAt(hour.minusSeconds(300));

		Future<CloudDatumStreamPollTaskEntity> result = service.executeTask(task);

		// THEN
		// @formatter:off
		and.thenThrownBy(() -> result.get(1, TimeUnit.MINUTES), "Task fails to execute")
			.isInstanceOf(ExecutionException.class)
			.extracting(e -> e.getCause())
			.as("The exception cause is the one thrown by the submit() call")
			.isSameAs(rejectedException)
			;
		// @formatter:on
	}

	@Test
	public void executeTask_fluxPublish() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamPollTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamPollTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant hour = clock.instant().truncatedTo(ChronoUnit.HOURS);

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

		// resolve datum stream settings (SolarIn OFF, SolarFlux ON)
		BasicCloudDatumStreamSettings datumStreamSettings = new BasicCloudDatumStreamSettings(false,
				true);
		given(datumStreamSettingsDao.resolveSettings(TEST_USER_ID, datumStream.getConfigId(),
				DEFAULT_DATUM_STREAM_SETTINGS)).willReturn(datumStreamSettings);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data associated with service configured on datum stream
		// here we return a datum for "5 min ago"
		final Datum datum1 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), hour.minusSeconds(300)),
				new DatumSamples(Map.of("watts", 123), Map.of("wattHours", 23456L), null));
		final Datum datum2 = new GeneralDatum(
				nodeId(datumStream.getObjectId(), datumStream.getSourceId(), hour.minusSeconds(120)),
				new DatumSamples(Map.of("watts", 234), Map.of("wattHours", 34567L), null));
		given(datumStreamService.datum(same(datumStream), any()))
				.willReturn(new BasicCloudDatumStreamQueryResult(List.of(datum1, datum2)));

		// post datum to SolarFlux
		given(fluxProcessor.processDatum(any())).willReturn(true);

		// update task details
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamPollTaskEntity(datumStream.getId());
		task.setState(Claimed);
		task.setExecuteAt(hour);
		task.setStartAt(hour.minusSeconds(300));

		Future<CloudDatumStreamPollTaskEntity> result = service.executeTask(task);
		CloudDatumStreamPollTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(datumStreamService).should().datum(same(datumStream), queryFilterCaptor.capture());
		and.then(queryFilterCaptor.getValue())
			.as("The query start date is the startAt of the task")
			.returns(task.getStartAt(), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("The query end date is the current date")
			.returns(clock.instant(), from(CloudDatumStreamQueryFilter::getEndDate))
			;

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again")
			.returns(Queued, from(CloudDatumStreamPollTaskEntity::getState))
			.as("Update task execute date to next time based on configuration schedule (every 5min)")
			.returns(task.getExecuteAt().plusSeconds(300), from(CloudDatumStreamPollTaskEntity::getExecuteAt))
			.as("Update task start date to highest date of datum captured")
			.returns(datum2.getTimestamp(), from(CloudDatumStreamPollTaskEntity::getStartAt))
			.as("No message generated for successful execution")
			.returns(null, from(CloudDatumStreamPollTaskEntity::getMessage))
			.as("No service properties generated for successful execution")
			.returns(null, from(CloudDatumStreamPollTaskEntity::getServiceProperties))
			;

		and.then(resultTask)
			.as("Result task is same as passed to DAO for update")
			.isSameAs(taskCaptor.getValue())
			;

		then(fluxProcessor).should(times(2)).processDatum(generalNodeDatumCaptor.capture());
		and.then(generalNodeDatumCaptor.getAllValues())
			.as("Both datum posted to SolarFlux")
			.hasSize(2)
			.satisfies(list -> {
				and.then(list)
					.element(0, type(GeneralNodeDatum.class))
					.as("GeneralNodeDatum ID derived from datum")
					.returns(new GeneralNodeDatumPK(datum1.getObjectId(), datum1.getTimestamp(), datum1.getSourceId()),
							from(GeneralNodeDatum::getId))
					.as("GeneralNodeDatum properties derviced from datum")
					.returns(datum1.getSampleData(), from(GeneralNodeDatum::getSampleData))
					;
				and.then(list)
					.element(1, type(GeneralNodeDatum.class))
					.as("GeneralNodeDatum ID derived from datum")
					.returns(new GeneralNodeDatumPK(datum2.getObjectId(), datum2.getTimestamp(), datum2.getSourceId()),
							from(GeneralNodeDatum::getId))
					.as("GeneralNodeDatum properties derviced from datum")
					.returns(datum2.getSampleData(), from(GeneralNodeDatum::getSampleData))
					;
			})
			;

		// @formatter:on
	}

	@Test
	public void executeTask_remoteException_clientHttpError() throws Exception {
		// GIVEN
		// submit task
		var future = new CompletableFuture<CloudDatumStreamPollTaskEntity>();
		given(executor.submit(argThat((Callable<CloudDatumStreamPollTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);

		final Instant hour = clock.instant().truncatedTo(ChronoUnit.HOURS);

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

		// resolve datum stream settings
		given(datumStreamSettingsDao.resolveSettings(TEST_USER_ID, datumStream.getConfigId(),
				DEFAULT_DATUM_STREAM_SETTINGS)).willReturn(DEFAULT_DATUM_STREAM_SETTINGS);

		// verify node ownership
		final var nodeOwner = new BasicSolarNodeOwnership(datumStream.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(datumStream.getObjectId())).willReturn(nodeOwner);

		// update task state to "processing"
		given(taskDao.updateTaskState(datumStream.getId(), Executing, Claimed)).willReturn(true);

		// query for data associated with service configured on datum stream; but throw 404 Not Found
		final String httpClientErrorExceptionMessage = randomString();
		final RemoteServiceException remoteServiceException = new RemoteServiceException("Remote error",
				HttpClientErrorException.create(httpClientErrorExceptionMessage, HttpStatus.NOT_FOUND,
						"404 Not Found", new HttpHeaders(), new byte[0], null));
		given(datumStreamService.datum(same(datumStream), any())).willThrow(remoteServiceException);

		// update task details
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		var task = new CloudDatumStreamPollTaskEntity(datumStream.getId());
		task.setState(Claimed);
		task.setExecuteAt(hour);
		task.setStartAt(hour.minusSeconds(300));

		Future<CloudDatumStreamPollTaskEntity> result = service.executeTask(task);

		// THEN
		// @formatter:off
		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task to update is copy of given task")
			.isNotSameAs(task)
			.as("Task to update has same ID as given task")
			.isEqualTo(task)
			.as("Update task state to Queued to run again after client HTTP error")
			.returns(Queued, from(CloudDatumStreamPollTaskEntity::getState))
			.as("Update task execute date to 1min in future")
			.returns(clock.instant().plusSeconds(60), from(CloudDatumStreamPollTaskEntity::getExecuteAt))
			.as("Update task start date unchanged")
			.returns(task.getStartAt(), from(CloudDatumStreamPollTaskEntity::getStartAt))
			.as("Error message saved")
			.returns("Error executing poll task.", from(CloudDatumStreamPollTaskEntity::getMessage))
			.as("Service properties saved with exception message")
			.returns(Map.of("message", httpClientErrorExceptionMessage), from(CloudDatumStreamPollTaskEntity::getServiceProperties))
			;

		and.thenThrownBy(() -> {
				result.get(1, TimeUnit.MINUTES);
			}, "ExecutionException thrown")
			.isInstanceOf(ExecutionException.class)
			.cause()
			.isSameAs(remoteServiceException)
			;

		// @formatter:on
	}

	@Test
	public void resetTasks() {
		// GIVEN
		final Instant ts = Instant.now();
		final int resetCount = RNG.nextInt(Integer.MAX_VALUE);
		given(taskDao.resetAbandondedExecutingTasks(ts)).willReturn(resetCount);

		// WHEN
		int result = service.resetAbandondedExecutingTasks(ts);

		and.then(result).as("DAO result returned").isEqualTo(resetCount);
	}

}
