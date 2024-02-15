/* ==================================================================
 * ConnectorStatusDatumPublisherTests.java - 2/04/2020 9:30:03 am
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

package net.solarnetwork.central.ocpp.v16.controller.test;

import static java.util.UUID.randomUUID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralChargeSession;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.v16.controller.ConnectorStatusDatumPublisher;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.ChargePointStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;

/**
 * Test cases for the {@link ConnectorStatusDatumPublisher} class.
 * 
 * @author matt
 * @version 2.1
 */
public class ConnectorStatusDatumPublisherTests {

	private CentralChargePointDao chargePointDao;
	private ChargePointSettingsDao chargePointSettingsDao;
	private CentralChargePointConnectorDao chargePointConnectorDao;
	private CentralChargeSessionDao chargeSessionDao;
	private DatumEntityDao datumDao;
	private DatumProcessor fluxPublisher;
	private ConnectorStatusDatumPublisher publisher;

	@Before
	public void setup() {
		chargePointDao = EasyMock.createMock(CentralChargePointDao.class);
		chargePointSettingsDao = EasyMock.createMock(ChargePointSettingsDao.class);
		chargePointConnectorDao = EasyMock.createMock(CentralChargePointConnectorDao.class);
		chargeSessionDao = EasyMock.createMock(CentralChargeSessionDao.class);
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		fluxPublisher = EasyMock.createMock(DatumProcessor.class);
		publisher = new ConnectorStatusDatumPublisher(chargePointDao, chargePointSettingsDao,
				chargePointConnectorDao, chargeSessionDao, datumDao);
		publisher.setFluxPublisher(fluxPublisher);
	}

	@After
	public void teardown() {
		EasyMock.verify(chargePointSettingsDao, chargePointConnectorDao, chargeSessionDao, datumDao,
				fluxPublisher);
	}

	private void replayAll() {
		EasyMock.replay(chargePointSettingsDao, chargePointConnectorDao, chargeSessionDao, datumDao,
				fluxPublisher);
	}

	@Test
	public void publishDatum() {
		// GIVEN
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));

		// @formatter:off
		StatusNotification info = StatusNotification.builder()
				.withConnectorId(1)
				.withStatus(ChargePointStatus.Charging)
				.withErrorCode(ChargePointErrorCode.GroundFailure)
				.withTimestamp(Instant.now())
				.withInfo("Hello.")
				.withVendorId("ACME")
				.withVendorErrorCode("Sweet as.")
				.build();
		// @formatter:on

		CentralChargePointConnector cpc = new CentralChargePointConnector(cp.getId(), 1, cp.getUserId(),
				Instant.now());
		cpc.setInfo(info);
		expect(chargePointConnectorDao.get(cp.getUserId(), cpc.getId())).andReturn(cpc);

		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate("/foo/{chargerIdentifier}/{connectorId}/{something}/{else}");
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(), 0,
				info.getConnectorId())).andReturn(null);

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		String sourceId = "/foo/" + cp.getInfo().getId() + "/1/status";
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor)))
				.andReturn(new DatumPK(streamId, info.getTimestamp()));

		expect(fluxPublisher.isConfigured()).andReturn(true);
		Capture<Identity<GeneralNodeDatumPK>> fluxDatumCaptor = new Capture<>();
		expect(fluxPublisher.processDatum(capture(fluxDatumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		publisher.processStatusNotification(cp, info);

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Published datum node ID", d.getNodeId(), equalTo(cp.getNodeId()));
		assertThat("Published datum source ID", d.getSourceId(), equalTo(sourceId));
		assertThat("Published datum ts", d.getCreated(), equalTo(info.getTimestamp()));
		DatumSamples s = d.getSamples();
		assertThat("Published status", s.getStatusSampleString("status"),
				equalTo(ChargePointStatus.Charging.toString()));
		assertThat("Published error code", s.getStatusSampleString("errorCode"),
				equalTo(ChargePointErrorCode.GroundFailure.toString()));
		assertThat("Published info", s.getStatusSampleString("info"), equalTo(info.getInfo()));
		assertThat("Published vendor ID", s.getStatusSampleString("vendorId"),
				equalTo(info.getVendorId()));
		assertThat("Published vendor error code", s.getStatusSampleString("vendorErrorCode"),
				equalTo(info.getVendorErrorCode()));

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValue(), sameInstance(d));
	}

	@Test
	public void publishDatum_withChargeSession() {
		// GIVEN
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));

		// @formatter:off
		StatusNotification info = StatusNotification.builder()
				.withConnectorId(1)
				.withStatus(ChargePointStatus.Charging)
				.withErrorCode(ChargePointErrorCode.GroundFailure)
				.withTimestamp(Instant.now())
				.withInfo("Hello.")
				.withVendorId("ACME")
				.withVendorErrorCode("Sweet as.")
				.build();
		// @formatter:on

		CentralChargePointConnector cpc = new CentralChargePointConnector(cp.getId(), 1, cp.getUserId(),
				Instant.now());
		cpc.setInfo(info);
		expect(chargePointConnectorDao.get(cp.getUserId(), cpc.getId())).andReturn(cpc);

		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate("/foo/{chargerIdentifier}/{connectorId}/{something}/{else}");
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		CentralChargeSession session = new CentralChargeSession(randomUUID(),
				info.getTimestamp().minusSeconds(60), "foo", cp.getId(), info.getEvseId(),
				info.getConnectorId(), "1");
		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(), 0,
				info.getConnectorId())).andReturn(session);

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		String sourceId = "/foo/" + cp.getInfo().getId() + "/1/status";
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor)))
				.andReturn(new DatumPK(streamId, info.getTimestamp()));

		expect(fluxPublisher.isConfigured()).andReturn(true);
		Capture<Identity<GeneralNodeDatumPK>> fluxDatumCaptor = new Capture<>();
		expect(fluxPublisher.processDatum(capture(fluxDatumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		publisher.processStatusNotification(cp, info);

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Published datum node ID", d.getNodeId(), equalTo(cp.getNodeId()));
		assertThat("Published datum source ID", d.getSourceId(), equalTo(sourceId));
		assertThat("Published datum ts", d.getCreated(), equalTo(info.getTimestamp()));
		DatumSamples s = d.getSamples();
		assertThat("Published status", s.getStatusSampleString("status"),
				equalTo(ChargePointStatus.Charging.toString()));
		assertThat("Published error code", s.getStatusSampleString("errorCode"),
				equalTo(ChargePointErrorCode.GroundFailure.toString()));
		assertThat("Published info", s.getStatusSampleString("info"), equalTo(info.getInfo()));
		assertThat("Published vendor ID", s.getStatusSampleString("vendorId"),
				equalTo(info.getVendorId()));
		assertThat("Published vendor error code", s.getStatusSampleString("vendorErrorCode"),
				equalTo(info.getVendorErrorCode()));
		assertThat("Published session ID", s.getStatusSampleString("sessionId"),
				equalTo(session.getId().toString()));
		assertThat("Published transaction ID", s.getStatusSampleString("transactionId"),
				equalTo(String.valueOf(session.getTransactionId())));

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValue(), sameInstance(d));
	}

	@Test
	public void publishDatum_conn0() {
		// GIVEN
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));

		// @formatter:off
		StatusNotification info0 = StatusNotification.builder()
				.withConnectorId(0)
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now())
				.build();
		// @formatter:on

		CentralChargePointConnector cpc0 = new CentralChargePointConnector(cp.getId(), 0, cp.getUserId(),
				Instant.now());
		cpc0.setInfo(info0);
		expect(chargePointConnectorDao.get(cp.getUserId(), cpc0.getId())).andReturn(cpc0);

		// @formatter:off
		StatusNotification info1 = StatusNotification.builder()
				.withConnectorId(1)
				.withStatus(ChargePointStatus.Charging)
				.withErrorCode(ChargePointErrorCode.GroundFailure)
				.withTimestamp(Instant.now())
				.withInfo("Hello.")
				.withVendorId("ACME")
				.withVendorErrorCode("Sweet as.")
				.build();
		// @formatter:on

		CentralChargePointConnector cpc1 = new CentralChargePointConnector(cp.getId(), 1, cp.getUserId(),
				Instant.now());
		cpc1.setInfo(info1);

		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate("/foo/{chargerIdentifier}/{connectorId}/{something}/{else}");
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		expect(chargeSessionDao.getIncompleteChargeSessionForConnector(cp.getId(), 0,
				info0.getConnectorId())).andReturn(null);

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		String sourceId0 = "/foo/" + cp.getInfo().getId() + "/0/status";
		UUID streamId0 = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor)))
				.andReturn(new DatumPK(streamId0, info0.getTimestamp()));

		expect(fluxPublisher.isConfigured()).andReturn(true);
		Capture<Identity<GeneralNodeDatumPK>> fluxDatumCaptor = new Capture<>();
		expect(fluxPublisher.processDatum(capture(fluxDatumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		// @formatter:off
		StatusNotification info = StatusNotification.builder()
				.withConnectorId(0)
				.withStatus(ChargePointStatus.Unknown)
				.build();
		// @formatter:on
		publisher.processStatusNotification(cp, info);

		// THEN
		GeneralNodeDatum d = datumCaptor.getValues().get(0);
		assertThat("Published datum node ID", d.getNodeId(), equalTo(cp.getNodeId()));
		assertThat("Published datum source ID", d.getSourceId(), equalTo(sourceId0));
		assertThat("Published datum ts", d.getCreated(), equalTo(info0.getTimestamp()));
		DatumSamples s = d.getSamples();
		assertThat("Published status", s.getStatusSampleString("status"),
				equalTo(ChargePointStatus.Available.toString()));
		assertThat("Published error code", s.getStatusSampleString("errorCode"), nullValue());
		assertThat("Published info", s.getStatusSampleString("info"), nullValue());
		assertThat("Published vendor ID", s.getStatusSampleString("vendorId"), nullValue());
		assertThat("Published vendor error code", s.getStatusSampleString("vendorErrorCode"),
				nullValue());

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValue(), sameInstance(d));
	}
}
