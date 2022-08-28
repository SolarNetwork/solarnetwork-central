/* ==================================================================
 * DlbMeterDataTransferDatumPublisherTests.java - 2/08/2022 7:04:54 am
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

package net.solarnetwork.central.ocpp.v16.vendor.zjbeny.test;

import static java.util.UUID.randomUUID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.math.BigDecimal;
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
import net.solarnetwork.central.ocpp.v16.vendor.zjbeny.DlbMeterDataTransferDatumPublisher;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import ocpp.v16.CentralSystemAction;
import ocpp.v16.cs.DataTransferRequest;
import ocpp.v16.cs.DataTransferResponse;
import ocpp.v16.cs.DataTransferStatus;

/**
 * Test cases for the {@link DlbMeterDataTransferDatumPublisher} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DlbMeterDataTransferDatumPublisherTests {

	private CentralChargePointDao chargePointDao;
	private ChargePointSettingsDao chargePointSettingsDao;
	private CentralChargePointConnectorDao chargePointConnectorDao;
	private DatumEntityDao datumDao;
	private DatumProcessor fluxPublisher;
	private DlbMeterDataTransferDatumPublisher publisher;

	@BeforeEach
	public void setup() {
		chargePointDao = EasyMock.createMock(CentralChargePointDao.class);
		chargePointSettingsDao = EasyMock.createMock(ChargePointSettingsDao.class);
		chargePointConnectorDao = EasyMock.createMock(CentralChargePointConnectorDao.class);
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		fluxPublisher = EasyMock.createMock(DatumProcessor.class);
		publisher = new DlbMeterDataTransferDatumPublisher(chargePointDao, chargePointSettingsDao,
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
	public void publishDatum_singlePhase_withSolar() {
		do_publishDatum_singlePhase_withSolar("DLBMode:Solar,Current.HomeLoad:1A,Current.Solar:2A");
	}

	@Test
	public void publishDatum_singlePhase_withSolar_json() {
		do_publishDatum_singlePhase_withSolar("""
				{
					"DLBMode":"Solar",
					"Current.HomeLoad":"1A",
					"Current.Solar":"2A"
				}
				""");
	}

	private void do_publishDatum_singlePhase_withSolar(String data) {
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
		String sourceId = "/foo/" + cp.getInfo().getId() + "/dlb";
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, Instant.now()));

		expect(fluxPublisher.isConfigured()).andReturn(true);
		Capture<Identity<GeneralNodeDatumPK>> fluxDatumCaptor = new Capture<>();
		expect(fluxPublisher.processDatum(capture(fluxDatumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		DataTransferRequest req = new DataTransferRequest();
		req.setVendorId(DlbMeterDataTransferDatumPublisher.VENDOR_ID);
		req.setMessageId(DlbMeterDataTransferDatumPublisher.MESSAGE_ID);
		req.setData(data);
		ActionMessage<DataTransferRequest> msg = new BasicActionMessage<>(cpi,
				CentralSystemAction.DataTransfer, req);
		publisher.processActionMessage(msg, new ActionMessageResultHandler<>() {

			@Override
			public boolean handleActionMessageResult(ActionMessage<DataTransferRequest> message,
					DataTransferResponse result, Throwable error) {
				assertThat("Result returned", result, is(notNullValue()));
				assertThat("No error thrown", error, is(nullValue()));
				assertThat("Status accepted", result.getStatus(),
						is(equalTo(DataTransferStatus.ACCEPTED)));
				return false;
			}

		});

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Published datum node ID", d.getNodeId(), is(equalTo(cp.getNodeId())));
		assertThat("Published datum source ID", d.getSourceId(), is(equalTo(sourceId)));
		assertThat("Published datum ts", d.getCreated(), is(notNullValue()));
		DatumSamples s = d.getSamples();
		assertThat("Published mode", s.getStatusSampleString("mode"), is(equalTo("Solar")));
		assertThat("Published load", s.getInstantaneousSampleInteger("load"), is(equalTo(1)));
		assertThat("Published solar", s.getInstantaneousSampleInteger("solar"), equalTo(2));

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValue(), sameInstance(d));
	}

	@Test
	public void publishDatum_multiPhase_withSolar() {
		do_publishDatum_multiPhase_withSolar(
				"DLBMode:Solar,Current.HomeLoad.L1:1.1A,Current.HomeLoad.L2:1.2A,Current.HomeLoad.L3:1.3A,Current.Solar.L1:2.1A,Current.Solar.L2:2.2A,Current.Solar.L3:2.3A");
	}

	@Test
	public void publishDatum_multiPhase_withSolar_json() {
		do_publishDatum_multiPhase_withSolar("""
				{
					"DLBMode":"Solar",
					"Current.HomeLoad.L1":"1.1A",
					"Current.HomeLoad.L2":"1.2A",
					"Current.HomeLoad.L3":"1.3A",
					"Current.Solar.L1":"2.1A",
					"Current.Solar.L2":"2.2A",
					"Current.Solar.L3":"2.3A"
				}
				""");
	}

	private void do_publishDatum_multiPhase_withSolar(String data) {
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
		String sourceId = "/foo/" + cp.getInfo().getId() + "/dlb";
		UUID streamId = UUID.randomUUID();
		expect(datumDao.store(capture(datumCaptor))).andReturn(new DatumPK(streamId, Instant.now()));

		expect(fluxPublisher.isConfigured()).andReturn(true);
		Capture<Identity<GeneralNodeDatumPK>> fluxDatumCaptor = new Capture<>();
		expect(fluxPublisher.processDatum(capture(fluxDatumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		DataTransferRequest req = new DataTransferRequest();
		req.setVendorId(DlbMeterDataTransferDatumPublisher.VENDOR_ID);
		req.setMessageId(DlbMeterDataTransferDatumPublisher.MESSAGE_ID);
		req.setData(data);
		ActionMessage<DataTransferRequest> msg = new BasicActionMessage<>(cpi,
				CentralSystemAction.DataTransfer, req);
		publisher.processActionMessage(msg, new ActionMessageResultHandler<>() {

			@Override
			public boolean handleActionMessageResult(ActionMessage<DataTransferRequest> message,
					DataTransferResponse result, Throwable error) {
				assertThat("Result returned", result, is(notNullValue()));
				assertThat("No error thrown", error, is(nullValue()));
				assertThat("Status accepted", result.getStatus(),
						is(equalTo(DataTransferStatus.ACCEPTED)));
				return false;
			}

		});

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Published datum node ID", d.getNodeId(), is(equalTo(cp.getNodeId())));
		assertThat("Published datum source ID", d.getSourceId(), is(equalTo(sourceId)));
		assertThat("Published datum ts", d.getCreated(), is(notNullValue()));
		DatumSamples s = d.getSamples();
		assertThat("Published mode", s.getStatusSampleString("mode"), is(equalTo("Solar")));
		assertThat("Published load phase A", s.getInstantaneousSampleBigDecimal("load_a"),
				is(equalTo(new BigDecimal("1.1"))));
		assertThat("Published load phase B", s.getInstantaneousSampleBigDecimal("load_b"),
				is(equalTo(new BigDecimal("1.2"))));
		assertThat("Published load phase C", s.getInstantaneousSampleBigDecimal("load_c"),
				is(equalTo(new BigDecimal("1.3"))));
		assertThat("Published solar phase a", s.getInstantaneousSampleBigDecimal("solar_a"),
				equalTo(new BigDecimal("2.1")));
		assertThat("Published solar phase b", s.getInstantaneousSampleBigDecimal("solar_b"),
				equalTo(new BigDecimal("2.2")));
		assertThat("Published solar phase c", s.getInstantaneousSampleBigDecimal("solar_c"),
				equalTo(new BigDecimal("2.3")));

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValue(), sameInstance(d));
	}

}
