/* ==================================================================
 * VehicleMacDataTransferDatumPublisherTests.java - 21/11/2023 9:02:54 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.v16.vendor.hiconics.test;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import net.solarnetwork.central.ocpp.v16.vendor.hiconics.VehicleMacDataTransferDatumPublisher;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.Identity;
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
 * Test cases for the {@link VehicleMacDataTransferDatumPublisher} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class VehicleMacDataTransferDatumPublisherTests {

	@Mock
	private CentralChargePointDao chargePointDao;

	@Mock
	private ChargePointSettingsDao chargePointSettingsDao;

	@Mock
	private CentralChargePointConnectorDao chargePointConnectorDao;

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private DatumProcessor fluxPublisher;

	@Captor
	private ArgumentCaptor<GeneralNodeDatum> datumCaptor;

	@Captor
	private ArgumentCaptor<Identity<GeneralNodeDatumPK>> fluxDatumCaptor;

	private VehicleMacDataTransferDatumPublisher publisher;

	@BeforeEach
	public void setup() {
		publisher = new VehicleMacDataTransferDatumPublisher(chargePointDao, chargePointSettingsDao,
				chargePointConnectorDao, datumDao, JsonUtils.newObjectMapper());
		publisher.setFluxPublisher(fluxPublisher);
	}

	private CentralChargePoint cp() {
		return new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));
	}

	private ChargePointIdentity cpi(CentralChargePoint cp) {
		return new ChargePointIdentity(cp.getInfo().getId(), cp.getUserId());
	}

	private ChargePointSettings cps(CentralChargePoint cp) {
		ChargePointSettings cps = new ChargePointSettings(cp.getId(), cp.getUserId(), Instant.now());
		cps.setSourceIdTemplate("/foo/{chargerIdentifier}/{connectorId}/{something}/{else}");
		return cps;
	}

	private String sourceId(CentralChargePoint cp, Integer connectorId) {
		return "/foo/" + cp.getInfo().getId() + (connectorId != null ? "/" + connectorId : "")
				+ VehicleMacDataTransferDatumPublisher.DEFAULT_SOURCE_ID_SUFFIX;
	}

	private void givenPublishDatumAndFlux(CentralChargePoint cp, ChargePointIdentity cpi,
			ChargePointSettings cps, UUID streamId, boolean datum, boolean flux) {
		given(chargePointDao.getForIdentity(cpi)).willReturn(cp);
		given(chargePointSettingsDao.resolveSettings(cp.getUserId(), cp.getId())).willReturn(cps);
		if ( datum ) {
			given(datumDao.store(any(GeneralNodeDatum.class))).willAnswer(i -> {
				GeneralNodeDatum d = i.getArgument(0);
				return new DatumPK(streamId, d.getCreated());
			});
		}
		if ( flux ) {
			given(fluxPublisher.isConfigured()).willReturn(true);
			given(fluxPublisher.processDatum(any())).willReturn(true);
		}
	}

	private void thenPublishedDatumAndFlux(boolean datum, boolean flux) {
		if ( datum ) {
			then(datumDao).should().store(datumCaptor.capture());
		}
		if ( flux ) {
			then(fluxPublisher).should().processDatum(fluxDatumCaptor.capture());
		}
	}

	private void processDataTransfer(ChargePointIdentity cpi, DataTransferRequest req) {
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

	}

	private DataTransferRequest req(String data) {
		DataTransferRequest req = new DataTransferRequest();
		req.setVendorId(VehicleMacDataTransferDatumPublisher.VENDOR_ID);
		req.setMessageId(VehicleMacDataTransferDatumPublisher.MESSAGE_ID);
		req.setData(data);
		return req;
	}

	@SuppressWarnings("static-access")
	@Test
	public void isSupported() {
		// GIVEN
		final CentralChargePoint cp = cp();
		final ChargePointIdentity cpi = cpi(cp);
		final DataTransferRequest req = req("");

		// WHEN
		boolean supported = publisher.isMessageSupported(
				new BasicActionMessage<>(cpi, CentralSystemAction.DataTransfer, req));

		// THEN
		and.then(supported).as("Message is supported").isTrue();
	}

	@SuppressWarnings("static-access")
	@Test
	public void isNotSupported_wrongVendor() {
		// GIVEN
		final CentralChargePoint cp = cp();
		final ChargePointIdentity cpi = cpi(cp);
		final DataTransferRequest req = req("");
		req.setVendorId("foo");

		// WHEN
		boolean supported = publisher.isMessageSupported(
				new BasicActionMessage<>(cpi, CentralSystemAction.DataTransfer, req));

		// THEN
		and.then(supported).as("Message from other vendor is not supported").isFalse();
	}

	@SuppressWarnings("static-access")
	@Test
	public void isNotSupported_wrongMessageId() {
		// GIVEN
		final CentralChargePoint cp = cp();
		final ChargePointIdentity cpi = cpi(cp);
		final DataTransferRequest req = req("");
		req.setMessageId("foo");

		// WHEN
		boolean supported = publisher.isMessageSupported(
				new BasicActionMessage<>(cpi, CentralSystemAction.DataTransfer, req));

		// THEN
		and.then(supported).as("Message with other message ID is not supported").isFalse();
	}

	@SuppressWarnings("static-access")
	@Test
	public void publishDatum() {
		// GIVEN
		final String vid = "141FBA1035F2";
		final String dataTs = "2023-10-13T03:07:43Z";
		final String data = """
				{
					"vehicleId":"%s",
					"connectorId":1,
					"timestamp":"%s"
				}
				""".formatted(vid, dataTs);

		final CentralChargePoint cp = cp();
		final ChargePointIdentity cpi = cpi(cp);
		final ChargePointSettings cps = cps(cp);

		final String sourceId = sourceId(cp, 1);
		final UUID streamId = UUID.randomUUID();

		givenPublishDatumAndFlux(cp, cpi, cps, streamId, true, true);

		// WHEN
		DataTransferRequest req = req(data);
		processDataTransfer(cpi, req);

		// THEN
		thenPublishedDatumAndFlux(true, true);

		GeneralNodeDatum d = datumCaptor.getValue();
		// @formatter:off
		and.then(d)
				.as("Datum published")
				.isNotNull()
				.as("Published datum node ID")
				.returns(cp.getNodeId(), from(GeneralNodeDatum::getNodeId))
				.as("Published datum source ID")
				.returns(sourceId, from(GeneralNodeDatum::getSourceId))
				.as("Published datum ts")
				.returns(ISO_INSTANT.parse(dataTs, Instant::from), from(GeneralNodeDatum::getCreated))
				;
		
		and.then(d.getSamples())
				.as("Published vid")
				.returns(vid, s -> s.getStatusSampleString("vid"))
				;

		and.then(fluxDatumCaptor.getValue())
				.as("Published same datum to SolarFlux")
				.isSameAs(d)
				;
		// @formatter:on
	}

	@SuppressWarnings("static-access")
	@Test
	public void publishDatum2() {
		// GIVEN
		final String vid = "9CF6DD918B5C";
		final String dataTs = "2023-11-20T03:17:09Z";
		final String data = """
				{
				    "vehicleId": "%s",
				    "connectorId": 1,
				    "timestamp": "%s"
				}
				""".formatted(vid, dataTs);

		final CentralChargePoint cp = cp();
		final ChargePointIdentity cpi = cpi(cp);
		final ChargePointSettings cps = cps(cp);

		final String sourceId = sourceId(cp, 1);
		final UUID streamId = UUID.randomUUID();

		givenPublishDatumAndFlux(cp, cpi, cps, streamId, true, true);

		// WHEN
		DataTransferRequest req = req(data);
		processDataTransfer(cpi, req);

		// THEN
		thenPublishedDatumAndFlux(true, true);

		GeneralNodeDatum d = datumCaptor.getValue();
		// @formatter:off
		and.then(d)
				.as("Datum published")
				.isNotNull()
				.as("Published datum node ID")
				.returns(cp.getNodeId(), from(GeneralNodeDatum::getNodeId))
				.as("Published datum source ID")
				.returns(sourceId, from(GeneralNodeDatum::getSourceId))
				.as("Published datum ts")
				.returns(ISO_INSTANT.parse(dataTs, Instant::from), from(GeneralNodeDatum::getCreated))
				;
		
		and.then(d.getSamples())
				.as("Published vid")
				.returns(vid, s -> s.getStatusSampleString("vid"))
				;

		and.then(fluxDatumCaptor.getValue())
				.as("Published same datum to SolarFlux")
				.isSameAs(d)
				;
		// @formatter:on
	}

}
