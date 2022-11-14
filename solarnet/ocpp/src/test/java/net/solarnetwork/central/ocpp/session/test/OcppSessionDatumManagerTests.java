/* ==================================================================
 * OcppSessionDatumManagerTests.java - 27/02/2020 8:45:07 pm
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

package net.solarnetwork.central.ocpp.session.test;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.ocpp.session.OcppSessionDatumManager;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.AcEnergyDatum;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.dao.PurgePostedChargeSessionsTask;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.ocpp.domain.ChargeSessionEndInfo;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;
import net.solarnetwork.ocpp.domain.ChargeSessionStartInfo;
import net.solarnetwork.ocpp.domain.Location;
import net.solarnetwork.ocpp.domain.Measurand;
import net.solarnetwork.ocpp.domain.Phase;
import net.solarnetwork.ocpp.domain.ReadingContext;
import net.solarnetwork.ocpp.domain.SampledValue;
import net.solarnetwork.ocpp.domain.UnitOfMeasure;
import net.solarnetwork.ocpp.service.AuthorizationException;
import net.solarnetwork.ocpp.service.AuthorizationService;

/**
 * Test cases for the {@link OcppSessionDatumManager} class.
 * 
 * @author matt
 * @version 2.1
 */
public class OcppSessionDatumManagerTests {

	private AuthorizationService authService;
	private ChargePointDao chargePointDao;
	private ChargeSessionDao chargeSessionDao;
	private DatumEntityDao datumDao;
	private ChargePointSettingsDao chargePointSettingsDao;
	private DatumProcessor fluxPublisher;
	private TaskScheduler taskScheduler;
	private OcppSessionDatumManager manager;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Before
	public void setup() {
		authService = createMock(AuthorizationService.class);
		chargePointDao = createMock(ChargePointDao.class);
		chargeSessionDao = createMock(ChargeSessionDao.class);
		datumDao = createMock(DatumEntityDao.class);
		chargePointSettingsDao = createMock(ChargePointSettingsDao.class);
		fluxPublisher = createMock(DatumProcessor.class);
		taskScheduler = createMock(TaskScheduler.class);
		expect(chargePointDao.getObjectType()).andReturn((Class) CentralChargePoint.class);
		EasyMock.replay(chargePointDao);
		manager = new OcppSessionDatumManager(authService, chargePointDao, chargeSessionDao, datumDao,
				chargePointSettingsDao);
		manager.setFluxPublisher(fluxPublisher);
		EasyMock.verify(chargePointDao);
		EasyMock.reset(chargePointDao);
		manager.setTaskScheduler(taskScheduler);
	}

	@After
	public void teardown() {
		EasyMock.verify(authService, chargePointDao, chargeSessionDao, datumDao, chargePointSettingsDao,
				fluxPublisher, taskScheduler);
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(authService, chargePointDao, chargeSessionDao, datumDao, chargePointSettingsDao,
				fluxPublisher, taskScheduler);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
		}
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void startup() {
		// given
		int expireHours = 2;
		manager.setPurgePostedChargeSessionsExpirationHours(expireHours);

		Capture<Runnable> startupTaskCaptor = new Capture<>();
		ScheduledFuture<Object> startupTaskFuture = createMock(ScheduledFuture.class);
		expect(taskScheduler.schedule(capture(startupTaskCaptor), anyObject(Date.class)))
				.andReturn((ScheduledFuture) startupTaskFuture);

		long taskDelay = TimeUnit.HOURS.toMillis(expireHours) / 4;
		ScheduledFuture<Object> purgePostedTaskFuture = createMock(ScheduledFuture.class);
		Capture<Runnable> purgeTaskCaptor = new Capture<>();
		expect(taskScheduler.scheduleWithFixedDelay(capture(purgeTaskCaptor), anyObject(),
				eq(taskDelay))).andReturn((ScheduledFuture) purgePostedTaskFuture);

		// when
		replayAll(startupTaskFuture, purgePostedTaskFuture);

		manager.serviceDidStartup();

		Runnable startupTask = startupTaskCaptor.getValue();
		startupTask.run();

		// then
		assertThat("Purge posted task scheduled", purgeTaskCaptor.getValue(),
				instanceOf(PurgePostedChargeSessionsTask.class));
		verify(startupTaskFuture, purgePostedTaskFuture);
	}

	@Test
	public void startSession_ok() {
		// GIVEN

		// verify authorization
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity chargePointId = new ChargePointIdentity(identifier, "foo");
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		AuthorizationInfo authInfo = new AuthorizationInfo(idTag, AuthorizationStatus.Accepted);
		expect(authService.authorize(chargePointId, idTag)).andReturn(authInfo);

		// get ChargePoint
		expect(chargePointDao.getForIdentity(chargePointId)).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// verify concurrent tx
		int connectorId = 1;
		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(), connectorId))
				.andReturn(null);

		// create new session
		Capture<ChargeSession> sessionCaptor = new Capture<>();
		expect(chargeSessionDao.save(capture(sessionCaptor))).andAnswer(new IAnswer<UUID>() {

			@Override
			public UUID answer() throws Throwable {
				return sessionCaptor.getValue().getId();
			}
		});

		// refresh to get txid
		Capture<UUID> sessionIdCaptor = new Capture<>();
		int transactionId = 123;
		expect(chargeSessionDao.get(capture(sessionIdCaptor))).andAnswer(new IAnswer<ChargeSession>() {

			@Override
			public ChargeSession answer() throws Throwable {
				ChargeSession old = sessionCaptor.getValue();
				return new ChargeSession(old.getId(), old.getCreated(), old.getAuthId(),
						old.getChargePointId(), old.getConnectorId(), transactionId);
			}
		});

		// store initial reading
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		// generate datum from initial reading
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null));

		// publish to SolarFlux
		Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>();
		expect(fluxPublisher.isConfigured()).andReturn(true);
		expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true);

		// WHEN
		replayAll();

		// @formatter:off
		ChargeSessionStartInfo info = ChargeSessionStartInfo.builder()
				.withTimestampStart(Instant.now())
				.withChargePointId(chargePointId)
				.withAuthorizationId(idTag)
				.withConnectorId(connectorId)
				.withMeterStart(1234)
				.build();
		// @formatter:on

		ChargeSession sess = manager.startChargingSession(info);

		// THEN
		assertThat("Session created", sess, notNullValue());

		assertThat("Stored session ID not null", sessionCaptor.getValue(), notNullValue());
		assertThat("Stored session timestamp ID matches request", sessionCaptor.getValue().getCreated(),
				equalTo(info.getTimestampStart()));
		assertThat("Stored session Charge Point ID matches request",
				sessionCaptor.getValue().getChargePointId(), equalTo(cp.getId()));
		assertThat("Stored session auth ID matches request", sessionCaptor.getValue().getAuthId(),
				equalTo(info.getAuthorizationId()));
		assertThat("Stored session connector ID matches request",
				sessionCaptor.getValue().getConnectorId(), equalTo(info.getConnectorId()));

		assertThat("Created session ID matches refresh ID request", sessionIdCaptor.getValue(),
				equalTo(sessionCaptor.getValue().getId()));
		assertThat("Charge Point ID returned", sess.getChargePointId(), equalTo(cp.getId()));
		assertThat("Auth ID returned", sess.getAuthId(), equalTo(idTag));
		assertThat("Connector ID returned", sess.getConnectorId(), equalTo(connectorId));
		assertThat("Transaction ID returned", sess.getTransactionId(), equalTo(transactionId));

		List<SampledValue> samples = StreamSupport.stream(readingsCaptor.getValue().spliterator(), false)
				.collect(Collectors.toList());

		// @formatter:off
		SampledValue expectedReading = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		// @formatter:on

		assertThat("One reading saved", samples, hasSize(1));
		assertThat("Initial reading core properties", samples.get(0), equalTo(expectedReading));
		assertThat("Initial reading unit", samples.get(0).getUnit(), equalTo(UnitOfMeasure.Wh));
		assertThat("Initial reading value", samples.get(0).getValue(),
				equalTo(String.valueOf(info.getMeterStart())));

		GeneralNodeDatum datum = datumCaptor.getValue();
		assertThat("Datum generated", datum, notNullValue());
		assertThat("Datum date", datum.getCreated(), equalTo(sess.getCreated()));
		assertThat("Datum source ID", datum.getSourceId(),
				equalTo(String.format("/ocpp/cp/%s/%d/%s", identifier, connectorId, Location.Outlet)));
		assertThat("Energy prop",
				datum.getSamples().getAccumulatingSampleLong(AcEnergyDatum.WATT_HOUR_READING_KEY),
				equalTo(info.getMeterStart()));
		assertThat("Datum prop session ID",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.SessionId.getPropertyName()),
				equalTo(sess.getId().toString()));
		assertThat("Datum prop transaction ID",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.TransactionId.getPropertyName()),
				equalTo(String.valueOf(sess.getTransactionId())));
		assertThat("Datum prop auth token",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.AuthorizationToken.getPropertyName()),
				equalTo(sess.getAuthId().toString()));
		assertThat("Datum prop duration",
				datum.getSamples().getStatusSampleInteger(
						OcppSessionDatumManager.DatumProperty.SessionDuration.getPropertyName()),
				equalTo(0));
		assertThat("Datum prop end date",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.SessionEndDate.getPropertyName()),
				nullValue());
		assertThat("Datum prop end reason",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.SessionEndReason.getPropertyName()),
				nullValue());
		assertThat("Datum prop end auth token", datum.getSamples().getStatusSampleString(
				OcppSessionDatumManager.DatumProperty.SessionEndAuthorizationToken.getPropertyName()),
				nullValue());

		Identity<GeneralNodeDatumPK> fluxDatum = datumCaptor.getValue();
		assertThat("Same datum published to SolarFLux as SolarIn", fluxDatum, sameInstance(datum));
	}

	@Test
	public void startSession_concurrentTx() {
		// given

		// verify authorization
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity chargePointId = new ChargePointIdentity(identifier, "foo");
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		String idTag = UUID.randomUUID().toString().substring(0, 20);
		AuthorizationInfo authInfo = new AuthorizationInfo(idTag, AuthorizationStatus.Accepted);
		expect(authService.authorize(chargePointId, idTag)).andReturn(authInfo);

		// get ChargePoint
		expect(chargePointDao.getForIdentity(chargePointId)).andReturn(cp);

		// verify concurrent tx
		int connectorId = 1;
		int transactionId = 123;
		ChargeSession existingSess = new ChargeSession(UUID.randomUUID(), Instant.now().minusSeconds(60),
				idTag, cp.getId(), connectorId, transactionId);
		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(), connectorId))
				.andReturn(existingSess);

		// when
		replayAll();

		// @formatter:off
		ChargeSessionStartInfo info = ChargeSessionStartInfo.builder()
				.withTimestampStart(Instant.now())
				.withChargePointId(chargePointId)
				.withAuthorizationId(idTag)
				.withConnectorId(connectorId)
				.withMeterStart(1234)
				.build();
		// @formatter:on

		try {
			manager.startChargingSession(info);
			fail("Should have failed with ConcurrentTx");
		} catch ( AuthorizationException e ) {
			assertThat("Authorization info available", e.getInfo(), notNullValue());
			assertThat("Authorization status is ConcurrentTx", e.getInfo().getStatus(),
					equalTo(AuthorizationStatus.ConcurrentTx));
		}
	}

	@Test
	public void endSession_ok() {
		// GIVEN
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		ChargePointIdentity chargePointId = new ChargePointIdentity(identifier, "foo");
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(chargePointId)).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.getIncompleteChargeSessionForTransaction(cp.getId(), transactionId))
				.andReturn(sess);

		Capture<ChargeSession> updatedCaptor = new Capture<>();
		expect(chargeSessionDao.save(capture(updatedCaptor))).andReturn(sess.getId());

		// @formatter:off
		SampledValue startReading = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		SampledValue middleReading = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(10))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		// @formatter:on
		expect(chargeSessionDao.findReadingsForSession(sess.getId()))
				.andReturn(Arrays.asList(startReading, middleReading));

		Capture<Iterable<SampledValue>> newReadingsCapture = new Capture<>();
		chargeSessionDao.addReadings(capture(newReadingsCapture));

		// generate datum from initial reading
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null));

		// publish to SolarFlux
		Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>();
		expect(fluxPublisher.isConfigured()).andReturn(true);
		expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true);

		// WHEN
		replayAll();

		// @formatter:off
		ChargeSessionEndInfo info = ChargeSessionEndInfo.builder()
				.withTimestampEnd(Instant.now())
				.withAuthorizationId(idTag)
				.withChargePointId(chargePointId)
				.withTransactionId(transactionId)
				.withMeterEnd(54321)
				.withReason(ChargeSessionEndReason.Local)
				.build();
		// @formatter:on
		manager.endChargingSession(info);

		// THEN

		// @formatter:off
		SampledValue endReading = SampledValue.builder()
				.withTimestamp(info.getTimestampEnd())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.build();
		// @formatter:on

		List<SampledValue> samples = StreamSupport
				.stream(newReadingsCapture.getValue().spliterator(), false).collect(Collectors.toList());
		assertThat("One reading saved", samples, hasSize(1));
		assertThat("Initial reading core properties", samples.get(0), equalTo(endReading));
		assertThat("Initial reading unit", samples.get(0).getUnit(), equalTo(UnitOfMeasure.Wh));
		assertThat("Initial reading value", samples.get(0).getValue(),
				equalTo(String.valueOf(info.getMeterEnd())));

		// session should be update with end/posted dates
		ChargeSession updated = updatedCaptor.getValue();
		assertThat("Session ID same", updated.getId(), equalTo(sess.getId()));
		assertThat("End date set", updated.getEnded(), equalTo(info.getTimestampEnd()));
		assertThat("End auth ID set", updated.getAuthId(), equalTo(info.getAuthorizationId()));
		assertThat("Posted date set", updated.getPosted(), notNullValue());

		GeneralNodeDatum datum = datumCaptor.getValue();
		assertThat("Datum generated", datum, notNullValue());
		assertThat("Datum date", datum.getCreated(), equalTo(info.getTimestampEnd()));
		assertThat("Datum source ID", datum.getSourceId(),
				equalTo(String.format("/ocpp/cp/%s/%d/%s", identifier, connectorId, Location.Outlet)));
		assertThat("Energy prop",
				datum.getSamples().getAccumulatingSampleLong(AcEnergyDatum.WATT_HOUR_READING_KEY),
				equalTo(info.getMeterEnd()));
		assertThat("Datum prop session ID",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.SessionId.getPropertyName()),
				equalTo(sess.getId().toString()));
		assertThat("Datum prop transaction ID",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.TransactionId.getPropertyName()),
				equalTo(String.valueOf(sess.getTransactionId())));
		assertThat("Datum prop auth token",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.AuthorizationToken.getPropertyName()),
				equalTo(sess.getAuthId().toString()));
		assertThat("Datum prop duration",
				datum.getSamples().getStatusSampleLong(
						OcppSessionDatumManager.DatumProperty.SessionDuration.getPropertyName()),
				equalTo(Duration.between(sess.getCreated(), info.getTimestampEnd()).getSeconds()));
		assertThat("Datum prop end date",
				datum.getSamples().getStatusSampleLong(
						OcppSessionDatumManager.DatumProperty.SessionEndDate.getPropertyName()),
				equalTo(info.getTimestampEnd().toEpochMilli()));
		assertThat("Datum prop end reason",
				datum.getSamples().getStatusSampleString(
						OcppSessionDatumManager.DatumProperty.SessionEndReason.getPropertyName()),
				equalTo(ChargeSessionEndReason.Local.toString()));
		assertThat("Datum prop end auth token", datum.getSamples().getStatusSampleString(
				OcppSessionDatumManager.DatumProperty.SessionEndAuthorizationToken.getPropertyName()),
				equalTo(idTag));

		Identity<GeneralNodeDatumPK> fluxDatum = datumCaptor.getValue();
		assertThat("Same datum published to SolarFLux as SolarIn", fluxDatum, sameInstance(datum));
	}

	@Test
	public void addReadings_consolidate() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null)).times(3);

		// publish to SolarFlux
		Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>(CaptureType.ALL);
		expect(fluxPublisher.isConfigured()).andReturn(true).times(3);
		expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true).times(3);

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		SampledValue r2 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.PowerActiveImport)
				.withUnit(UnitOfMeasure.W)
				.withValue("500")
				.build();
		SampledValue r3 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(60))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("2345")
				.build();
		SampledValue r4 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(60))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.PowerActiveImport)
				.withUnit(UnitOfMeasure.W)
				.withValue("400")
				.build();
		SampledValue r5 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(60))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.SamplePeriodic)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.Frequency)
				.withValue("59.89")
				.build();
		SampledValue r6 = SampledValue.builder()
				.withTimestamp(sess.getCreated().plusSeconds(90))
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionEnd)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withValue("3456")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1, r2, r3, r4, r5, r6));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(),
				contains(r1, r2, r3, r4, r5, r6));

		List<GeneralNodeDatum> persistedDatum = datumCaptor.getValues();
		assertThat("Consolidated readings into 3 datum based on date", persistedDatum, hasSize(3));

		for ( int i = 0; i < persistedDatum.size(); i++ ) {
			GeneralNodeDatum d = persistedDatum.get(i);
			assertThat("Datum source ID " + i, d.getSourceId(),
					equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
			assertThat("Datum session ID " + i, d.getSamples().getStatusSampleString("sessionId"),
					equalTo(sess.getId().toString()));
		}

		GeneralNodeDatum d = persistedDatum.get(0);
		assertThat("Datum 1 @ transaction start", d.getCreated(), equalTo(r1.getTimestamp()));
		assertThat("Datum 1 consolidated properties", d.getSampleData(),
				allOf(hasEntry("wattHours", new BigDecimal(r1.getValue())),
						hasEntry("watts", new BigDecimal(r2.getValue()))));

		d = persistedDatum.get(1);
		assertThat("Datum 2 @ middle", d.getCreated(), equalTo(r3.getTimestamp()));
		assertThat("Datum 2 consolidated properties", d.getSampleData(),
				allOf(hasEntry("wattHours", new BigDecimal(r3.getValue())),
						hasEntry("watts", new BigDecimal(r4.getValue())),
						hasEntry("frequency", new BigDecimal(r5.getValue()))));

		d = persistedDatum.get(2);
		assertThat("Datum 2 @ transaction end", d.getCreated(), equalTo(r6.getTimestamp()));
		assertThat("Datum 2 consolidated properties", d.getSampleData(),
				hasEntry("wattHours", new BigDecimal(r6.getValue())));

		List<Identity<GeneralNodeDatumPK>> fluxDatum = fluxPublishCaptor.getValues();
		assertThat("Same number datum published to SolarFlux as SolarIn", fluxDatum.size(),
				equalTo(persistedDatum.size()));
		for ( int i = 0; i < fluxDatum.size(); i++ ) {
			assertThat("Same datum published to SolarFlux as SolarIn", fluxDatum.get(i),
					sameInstance(persistedDatum.get(i)));
		}
	}

	@Test
	public void addReadings_tempInKelvin() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null));

		// publish to SolarFlux
		Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>();
		expect(fluxPublisher.isConfigured()).andReturn(true);
		expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true);

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.Temperature)
				.withUnit(UnitOfMeasure.K)
				.withValue("305.6")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1));

		GeneralNodeDatum persistedDatum = datumCaptor.getValue();
		assertThat("Datum source ID ", persistedDatum.getSourceId(),
				equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
		assertThat("Datum session ID ", persistedDatum.getSamples().getStatusSampleString("sessionId"),
				equalTo(sess.getId().toString()));
		assertThat("Datum 1 @ transaction start", persistedDatum.getCreated(),
				equalTo(r1.getTimestamp()));
		assertThat("Datum 1 consolidated properties", persistedDatum.getSampleData(),
				hasEntry("temp", new BigDecimal("32.5")));

		Identity<GeneralNodeDatumPK> fluxDatum = fluxPublishCaptor.getValue();
		assertThat("Published datum is GeneralNodeDatum", fluxDatum, instanceOf(GeneralNodeDatum.class));
		GeneralNodeDatum publishedDatum = (GeneralNodeDatum) fluxDatum;
		assertThat("Datum source ID ", publishedDatum.getSourceId(),
				equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
		assertThat("Datum session ID ", publishedDatum.getSamples().getStatusSampleString("sessionId"),
				equalTo(sess.getId().toString()));
		assertThat("Datum 1 @ transaction start", publishedDatum.getCreated(),
				equalTo(r1.getTimestamp()));
		assertThat("Datum 1 consolidated properties", publishedDatum.getSampleData(),
				hasEntry("temp", new BigDecimal("32.5")));
	}

	@Test
	public void addReadings_phaseCurrent() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		cps.setPublishToSolarFlux(false);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null));

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.CurrentImport)
				.withUnit(UnitOfMeasure.A)
				.withPhase(Phase.L1)
				.withValue("3.6")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1));

		GeneralNodeDatum persistedDatum = datumCaptor.getValue();
		assertThat("Datum source ID ", persistedDatum.getSourceId(),
				equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
		assertThat("Datum session ID ", persistedDatum.getSamples().getStatusSampleString("sessionId"),
				equalTo(sess.getId().toString()));
		assertThat("Datum @ transaction start", persistedDatum.getCreated(), equalTo(r1.getTimestamp()));
		assertThat("Datum consolidated properties", persistedDatum.getSampleData(),
				hasEntry("current_a", new BigDecimal("3.6")));
	}

	@Test
	public void addReadings_multiPhaseCurrent() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		cps.setPublishToSolarFlux(false);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null));

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.CurrentImport)
				.withUnit(UnitOfMeasure.A)
				.withPhase(Phase.L1)
				.withValue("3.6")
				.build();
		SampledValue r2 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.CurrentImport)
				.withUnit(UnitOfMeasure.A)
				.withPhase(Phase.L2)
				.withValue("3.5")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1, r2));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1, r2));

		GeneralNodeDatum persistedDatum = datumCaptor.getValues().get(0);
		assertThat("Datum source ID ", persistedDatum.getSourceId(),
				equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
		assertThat("Datum session ID ", persistedDatum.getSamples().getStatusSampleString("sessionId"),
				equalTo(sess.getId().toString()));
		assertThat("Datum @ transaction start", persistedDatum.getCreated(), equalTo(r1.getTimestamp()));
		assertThat("Datum consolidated properties", persistedDatum.getSampleData(),
				allOf(hasEntry("current_a", new BigDecimal("3.6")),
						hasEntry("current_b", new BigDecimal("3.5"))));
	}

	@Test
	public void addReadings_multiValuesWithPhases() {
		// GIVEN
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setPublishToSolarFlux(false);
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null));

		// WHEN
		replayAll();

		List<SampledValue> sampledValues = new ArrayList<>();
		// @formatter:off
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("300.0")
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("225.1")
				.withMeasurand(Measurand.Voltage)
				.withPhase(Phase.L1N)
				.withUnit(UnitOfMeasure.V)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("0.1")
				.withMeasurand(Measurand.Voltage)
				.withPhase(Phase.L2N)
				.withUnit(UnitOfMeasure.V)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("0.2")
				.withMeasurand(Measurand.Voltage)
				.withPhase(Phase.L3N)
				.withUnit(UnitOfMeasure.V)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("0.3")
				.withMeasurand(Measurand.CurrentImport)
				.withPhase(Phase.L1)
				.withUnit(UnitOfMeasure.A)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("0.4")
				.withMeasurand(Measurand.CurrentImport)
				.withPhase(Phase.L2)
				.withUnit(UnitOfMeasure.A)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("0.5")
				.withMeasurand(Measurand.CurrentImport)
				.withPhase(Phase.L3)
				.withUnit(UnitOfMeasure.A)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("250.0")
				.withMeasurand(Measurand.CurrentOffered)
				.withUnit(UnitOfMeasure.A)
				.build());
		sampledValues.add(SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withValue("41.0")
				.withMeasurand(Measurand.Temperature)
				.withUnit(UnitOfMeasure.Celsius)
				.build());
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), sampledValues);

		// THEN
		SampledValue[] sortedSampledValues = sampledValues
				.toArray(new SampledValue[sampledValues.size()]);
		Arrays.sort(sortedSampledValues);
		assertThat("Persisted readings same as passed in but sorted", readingsCaptor.getValue(),
				contains(sortedSampledValues));

		GeneralNodeDatum persistedDatum = datumCaptor.getValue();
		assertThat("Datum source ID ", persistedDatum.getSourceId(),
				equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
		assertThat("Datum session ID ", persistedDatum.getSamples().getStatusSampleString("sessionId"),
				equalTo(sess.getId().toString()));
		assertThat("Datum @ transaction start", persistedDatum.getCreated(), equalTo(sess.getCreated()));
		assertThat("Datum consolidated properties", persistedDatum.getSampleData(),
				allOf(asList(hasEntry("wattHours", new BigDecimal("300.0")),
						hasEntry("voltage_a", new BigDecimal("225.1")),
						hasEntry("voltage_b", new BigDecimal("0.1")),
						hasEntry("voltage_c", new BigDecimal("0.2")),
						hasEntry("current_a", new BigDecimal("0.3")),
						hasEntry("current_b", new BigDecimal("0.4")),
						hasEntry("current_c", new BigDecimal("0.5")),
						hasEntry("currentOffered", new BigDecimal("250.0")),
						hasEntry("temp", new BigDecimal("41.0")))));
	}

	@Test
	public void addReadings_noSolarFlux() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		cps.setPublishToSolarFlux(false);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null));

		// publish to SolarFlux
		//Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>();
		//expect(fluxPublisher.isConfigured()).andReturn(true).times(3);
		//expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true);

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1));

		GeneralNodeDatum persistedDatum = datumCaptor.getValue();
		assertThat("Datum source ID ", persistedDatum.getSourceId(),
				equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
		assertThat("Datum session ID ", persistedDatum.getSamples().getStatusSampleString("sessionId"),
				equalTo(sess.getId().toString()));
		assertThat("Datum 1 @ transaction start", persistedDatum.getCreated(),
				equalTo(r1.getTimestamp()));
		assertThat("Datum 1 consolidated properties", persistedDatum.getSampleData(),
				hasEntry("wattHours", new BigDecimal(r1.getValue())));

		//Identity<GeneralNodeDatumPK> fluxDatum = fluxPublishCaptor.getValue();
		//assertThat("Same datum published to SolarFlux as SolarIn", fluxDatum,
		//		sameInstance(persistedDatum));
	}

	@Test
	public void addReadings_noSolarIn() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		cps.setPublishToSolarIn(false);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		// publish to SolarFlux
		Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>();
		expect(fluxPublisher.isConfigured()).andReturn(true);
		expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true);

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1));

		Identity<GeneralNodeDatumPK> fluxDatum = fluxPublishCaptor.getValue();
		assertThat("Published datum is GeneralNodeDatum", fluxDatum, instanceOf(GeneralNodeDatum.class));
		GeneralNodeDatum publishedDatum = (GeneralNodeDatum) fluxDatum;
		assertThat("Datum source ID ", publishedDatum.getSourceId(),
				equalTo("/ocpp/cp/" + identifier + "/" + connectorId + "/Outlet"));
		assertThat("Datum session ID ", publishedDatum.getSamples().getStatusSampleString("sessionId"),
				equalTo(sess.getId().toString()));
		assertThat("Datum 1 @ transaction start", publishedDatum.getCreated(),
				equalTo(r1.getTimestamp()));
		assertThat("Datum 1 consolidated properties", publishedDatum.getSampleData(),
				hasEntry("wattHours", new BigDecimal(r1.getValue())));
	}

	@Test
	public void addReadings_noSolarInOrSolarFlux() {
		// given
		String idTag = "tester";
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));
		int connectorId = 1;
		int transactionId = 123;

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		cps.setPublishToSolarIn(false);
		cps.setPublishToSolarFlux(false);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		// get current session
		ChargeSession sess = new ChargeSession(UUID.randomUUID(), Instant.now(), idTag, cp.getId(),
				connectorId, transactionId);
		expect(chargeSessionDao.get(sess.getId())).andReturn(sess);

		// get current readings for session
		expect(chargeSessionDao.findReadingsForSession(sess.getId())).andReturn(Collections.emptyList());

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		// when
		replayAll();

		// @formatter:off
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(sess.getCreated())
				.withSessionId(sess.getId())
				.withContext(ReadingContext.TransactionBegin)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1));
	}

	@Test
	public void addReadings_noTransaction_oneLocation() {
		// given
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		final int expectedDatumCount = 1;

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null))
				.times(expectedDatumCount);

		// publish to SolarFlux
		Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>(CaptureType.ALL);
		expect(fluxPublisher.isConfigured()).andReturn(true).times(expectedDatumCount);
		expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true)
				.times(expectedDatumCount);

		// when
		replayAll();

		// @formatter:off
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(ts)
				.withContext(ReadingContext.Trigger)
				.withLocation(Location.Inlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		SampledValue r2 = SampledValue.builder()
				.withTimestamp(ts)
				.withContext(ReadingContext.Trigger)
				.withLocation(Location.Inlet)
				.withMeasurand(Measurand.PowerActiveImport)
				.withUnit(UnitOfMeasure.W)
				.withValue("500")
				.build();
		
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1, r2));

		// then
		assertThat("Persisted readings same as passed in", readingsCaptor.getValue(), contains(r1, r2));

		List<GeneralNodeDatum> persistedDatum = datumCaptor.getValues();
		assertThat("Consolidated readings into 1 datum based on date", persistedDatum,
				hasSize(expectedDatumCount));

		GeneralNodeDatum d = persistedDatum.get(0);
		assertThat("Datum source ID", d.getSourceId(), equalTo("/ocpp/cp/" + identifier + "/0/Inlet"));
		assertThat("Datum session ID", d.getSamples().getStatusSampleString("sessionId"),
				is(nullValue()));
		assertThat("Datum @ trigger", d.getCreated(), equalTo(r1.getTimestamp()));
		assertThat("Datum consolidated properties", d.getSampleData(),
				allOf(hasEntry("wattHours", new BigDecimal(r1.getValue())),
						hasEntry("watts", new BigDecimal(r2.getValue()))));

		List<Identity<GeneralNodeDatumPK>> fluxDatum = fluxPublishCaptor.getValues();
		assertThat("Same number datum published to SolarFlux as SolarIn", fluxDatum.size(),
				equalTo(persistedDatum.size()));
		for ( int i = 0; i < fluxDatum.size(); i++ ) {
			assertThat("Same datum published to SolarFlux as SolarIn", fluxDatum.get(i),
					sameInstance(persistedDatum.get(i)));
		}
	}

	@Test
	public void addReadings_noTransaction_multiLocation() {
		// given
		String identifier = UUID.randomUUID().toString();
		CentralChargePoint cp = new CentralChargePoint(UUID.randomUUID().getMostSignificantBits(),
				UUID.randomUUID().getMostSignificantBits(), UUID.randomUUID().getMostSignificantBits(),
				Instant.now(), new ChargePointInfo(identifier));

		// get ChargePoint
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get ChargePointSettings
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate(UserSettings.DEFAULT_SOURCE_ID_TEMPLATE);
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		final int expectedDatumCount = 2;

		// save readings
		Capture<Iterable<SampledValue>> readingsCaptor = new Capture<>();
		chargeSessionDao.addReadings(capture(readingsCaptor));

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, null))
				.times(expectedDatumCount);

		// publish to SolarFlux
		Capture<Identity<GeneralNodeDatumPK>> fluxPublishCaptor = new Capture<>(CaptureType.ALL);
		expect(fluxPublisher.isConfigured()).andReturn(true).times(expectedDatumCount);
		expect(fluxPublisher.processDatum(capture(fluxPublishCaptor))).andReturn(true)
				.times(expectedDatumCount);

		// when
		replayAll();

		// @formatter:off
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		SampledValue r1 = SampledValue.builder()
				.withTimestamp(ts)
				.withContext(ReadingContext.Trigger)
				.withLocation(Location.Inlet)
				.withMeasurand(Measurand.EnergyActiveImportRegister)
				.withUnit(UnitOfMeasure.Wh)
				.withValue("1234")
				.build();
		SampledValue r2 = SampledValue.builder()
				.withTimestamp(ts)
				.withContext(ReadingContext.Trigger)
				.withLocation(Location.Outlet)
				.withMeasurand(Measurand.PowerActiveImport)
				.withUnit(UnitOfMeasure.W)
				.withValue("500")
				.build();
		
		// @formatter:on
		manager.addChargingSessionReadings(cp.chargePointIdentity(), asList(r1, r2));

		// then
		assertThat("Persisted readings same as passed in (sorted)", readingsCaptor.getValue(),
				contains(r2, r1));

		List<GeneralNodeDatum> persistedDatum = datumCaptor.getValues();
		assertThat("Consolidated readings into 2 datum based on date", persistedDatum,
				hasSize(expectedDatumCount));

		GeneralNodeDatum d;

		d = persistedDatum.get(0);
		assertThat("Datum source ID", d.getSourceId(), equalTo("/ocpp/cp/" + identifier + "/0/Outlet"));
		assertThat("Datum session ID", d.getSamples().getStatusSampleString("sessionId"),
				is(nullValue()));
		assertThat("Datum @ trigger", d.getCreated(), equalTo(r2.getTimestamp()));
		assertThat("Datum consolidated properties", d.getSampleData(),
				hasEntry("watts", new BigDecimal(r2.getValue())));

		d = persistedDatum.get(1);
		assertThat("Datum source ID", d.getSourceId(), equalTo("/ocpp/cp/" + identifier + "/0/Inlet"));
		assertThat("Datum session ID", d.getSamples().getStatusSampleString("sessionId"),
				is(nullValue()));
		assertThat("Datum @ trigger", d.getCreated(), equalTo(r1.getTimestamp()));
		assertThat("Datum consolidated properties", d.getSampleData(),
				hasEntry("wattHours", new BigDecimal(r1.getValue())));

		List<Identity<GeneralNodeDatumPK>> fluxDatum = fluxPublishCaptor.getValues();
		assertThat("Same number datum published to SolarFlux as SolarIn", fluxDatum.size(),
				equalTo(persistedDatum.size()));
		for ( int i = 0; i < fluxDatum.size(); i++ ) {
			assertThat("Same datum published to SolarFlux as SolarIn", fluxDatum.get(i),
					sameInstance(persistedDatum.get(i)));
		}
	}

}
