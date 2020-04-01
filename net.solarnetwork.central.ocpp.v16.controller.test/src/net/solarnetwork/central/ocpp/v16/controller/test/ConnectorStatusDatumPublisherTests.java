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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Arrays;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.v16.controller.ConnectorStatusDatumPublisher;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.ChargePointStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link ConnectorStatusDatumPublisher} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ConnectorStatusDatumPublisherTests {

	private ChargePointSettingsDao chargePointSettingsDao;
	private CentralChargePointConnectorDao chargePointConnectorDao;
	private GeneralNodeDatumDao datumDao;
	private DatumProcessor fluxPublisher;
	private ConnectorStatusDatumPublisher publisher;

	@Before
	public void setup() {
		chargePointSettingsDao = EasyMock.createMock(ChargePointSettingsDao.class);
		chargePointConnectorDao = EasyMock.createMock(CentralChargePointConnectorDao.class);
		datumDao = EasyMock.createMock(GeneralNodeDatumDao.class);
		fluxPublisher = EasyMock.createMock(DatumProcessor.class);
		publisher = new ConnectorStatusDatumPublisher(chargePointSettingsDao, chargePointConnectorDao,
				datumDao, new StaticOptionalService<>(fluxPublisher));
	}

	@After
	public void teardown() {
		EasyMock.verify(chargePointSettingsDao, chargePointConnectorDao, datumDao, fluxPublisher);
	}

	private void replayAll() {
		EasyMock.replay(chargePointSettingsDao, chargePointConnectorDao, datumDao, fluxPublisher);
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

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		String sourceId = "/foo/" + cp.getInfo().getId() + "/1/status";
		expect(datumDao.store(capture(datumCaptor))).andReturn(new GeneralNodeDatumPK(cp.getNodeId(),
				new DateTime(info.getTimestamp().toEpochMilli()), sourceId));

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
		assertThat("Published datum ts", d.getCreated(),
				equalTo(new DateTime(info.getTimestamp().toEpochMilli())));
		GeneralNodeDatumSamples s = d.getSamples();
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
	public void publishDatum_entireChargePoint() {
		// GIVEN
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));

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

		// @formatter:off
		StatusNotification info2 = StatusNotification.builder()
				.withConnectorId(2)
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now())
				.build();
		// @formatter:on

		CentralChargePointConnector cpc2 = new CentralChargePointConnector(cp.getId(), 2, cp.getUserId(),
				Instant.now());
		cpc2.setInfo(info2);

		expect(chargePointConnectorDao.findByChargePointId(cp.getUserId(), cp.getId()))
				.andReturn(Arrays.asList(cpc1, cpc2));

		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate("/foo/{chargerIdentifier}/{connectorId}/{something}/{else}");
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		String sourceId1 = "/foo/" + cp.getInfo().getId() + "/1/status";
		String sourceId2 = "/foo/" + cp.getInfo().getId() + "/2/status";
		expect(datumDao.store(capture(datumCaptor))).andReturn(new GeneralNodeDatumPK(cp.getNodeId(),
				new DateTime(info1.getTimestamp().toEpochMilli()), sourceId1));
		expect(datumDao.store(capture(datumCaptor))).andReturn(new GeneralNodeDatumPK(cp.getNodeId(),
				new DateTime(info2.getTimestamp().toEpochMilli()), sourceId2));

		expect(fluxPublisher.isConfigured()).andReturn(true).times(2);
		Capture<Identity<GeneralNodeDatumPK>> fluxDatumCaptor = new Capture<>(CaptureType.ALL);
		expect(fluxPublisher.processDatum(capture(fluxDatumCaptor))).andReturn(true).times(2);

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
		assertThat("Published datum source ID", d.getSourceId(), equalTo(sourceId1));
		assertThat("Published datum ts", d.getCreated(),
				equalTo(new DateTime(info1.getTimestamp().toEpochMilli())));
		GeneralNodeDatumSamples s = d.getSamples();
		assertThat("Published status", s.getStatusSampleString("status"),
				equalTo(ChargePointStatus.Charging.toString()));
		assertThat("Published error code", s.getStatusSampleString("errorCode"),
				equalTo(ChargePointErrorCode.GroundFailure.toString()));
		assertThat("Published info", s.getStatusSampleString("info"), equalTo(info1.getInfo()));
		assertThat("Published vendor ID", s.getStatusSampleString("vendorId"),
				equalTo(info1.getVendorId()));
		assertThat("Published vendor error code", s.getStatusSampleString("vendorErrorCode"),
				equalTo(info1.getVendorErrorCode()));

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValues().get(0),
				sameInstance(d));

		d = datumCaptor.getValues().get(1);
		assertThat("Published datum node ID", d.getNodeId(), equalTo(cp.getNodeId()));
		assertThat("Published datum source ID", d.getSourceId(), equalTo(sourceId2));
		assertThat("Published datum ts", d.getCreated(),
				equalTo(new DateTime(info2.getTimestamp().toEpochMilli())));
		s = d.getSamples();
		assertThat("Published status", s.getStatusSampleString("status"),
				equalTo(ChargePointStatus.Available.toString()));
		assertThat("Published error code", s.getStatusSampleString("errorCode"), nullValue());
		assertThat("Published info", s.getStatusSampleString("info"), nullValue());
		assertThat("Published vendor ID", s.getStatusSampleString("vendorId"), nullValue());
		assertThat("Published vendor error code", s.getStatusSampleString("vendorErrorCode"),
				nullValue());

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValues().get(1),
				sameInstance(d));
	}
}
