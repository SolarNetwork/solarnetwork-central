/* ==================================================================
 * FirmwareStatusDatumPublisherTests.java - 29/07/2022 10:19:33 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.v16.controller.FirmwareStatusDatumPublisher;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.v16.jakarta.CentralSystemAction;
import ocpp.v16.jakarta.cs.FirmwareStatus;
import ocpp.v16.jakarta.cs.FirmwareStatusNotificationRequest;
import ocpp.v16.jakarta.cs.FirmwareStatusNotificationResponse;

/**
 * Test cases for the {@link FirmwaresStatusDatumPublisher} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FirmwareStatusDatumPublisherTests {

	private CentralChargePointDao chargePointDao;
	private ChargePointSettingsDao chargePointSettingsDao;
	private CentralChargePointConnectorDao chargePointConnectorDao;
	private DatumEntityDao datumDao;
	private DatumProcessor fluxPublisher;
	private FirmwareStatusDatumPublisher publisher;

	@BeforeEach
	public void setup() {
		chargePointDao = EasyMock.createMock(CentralChargePointDao.class);
		chargePointSettingsDao = EasyMock.createMock(ChargePointSettingsDao.class);
		chargePointConnectorDao = EasyMock.createMock(CentralChargePointConnectorDao.class);
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		fluxPublisher = EasyMock.createMock(DatumProcessor.class);
		publisher = new FirmwareStatusDatumPublisher(chargePointDao, chargePointSettingsDao,
				chargePointConnectorDao, datumDao);
		publisher.setFluxPublisher(fluxPublisher);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(chargePointDao, chargePointSettingsDao, chargePointConnectorDao, datumDao,
				fluxPublisher);
	}

	private void replayAll() {
		EasyMock.replay(chargePointDao, chargePointSettingsDao, chargePointConnectorDao, datumDao,
				fluxPublisher);
	}

	@Test
	public void publishDatum() {
		// GIVEN
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));

		ChargePointIdentity cpi = new ChargePointIdentity(cp.getInfo().getId(), cp.getUserId());
		expect(chargePointDao.getForIdentity(cpi)).andReturn(cp);

		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate("/foo/{chargerIdentifier}/{connectorId}/{something}/{else}");
		expect(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).andReturn(cps);

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		String sourceId = "/foo/" + cp.getInfo().getId() + "/firmware-status";
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, Instant.now()));

		expect(fluxPublisher.isConfigured()).andReturn(true);
		Capture<Identity<GeneralNodeDatumPK>> fluxDatumCaptor = new Capture<>();
		expect(fluxPublisher.processDatum(capture(fluxDatumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		FirmwareStatusNotificationRequest req = new FirmwareStatusNotificationRequest();
		req.setStatus(FirmwareStatus.INSTALLED);
		ActionMessage<FirmwareStatusNotificationRequest> msg = new BasicActionMessage<>(cpi,
				CentralSystemAction.FirmwareStatusNotification, req);
		publisher.processActionMessage(msg, new ActionMessageResultHandler<>() {

			@Override
			public boolean handleActionMessageResult(
					ActionMessage<FirmwareStatusNotificationRequest> message,
					FirmwareStatusNotificationResponse result, Throwable error) {
				assertThat("Result returned", result, is(notNullValue()));
				assertThat("No error thrown", error, is(nullValue()));
				return false;
			}

		});

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Published datum node ID", d.getNodeId(), is(equalTo(cp.getNodeId())));
		assertThat("Published datum source ID", d.getSourceId(), is(equalTo(sourceId)));
		assertThat("Published datum ts", d.getCreated(), is(notNullValue()));
		DatumSamples s = d.getSamples();
		assertThat("Published status", s.getStatusSampleString("status"),
				equalTo(req.getStatus().toString()));

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValue(), sameInstance(d));
	}

}
