/* ==================================================================
 * MeterTransferDataTransferDatumPublisherTests.java - 17/06/2023 8:42:39 am
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

package net.solarnetwork.central.ocpp.v16.vendor.abb.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
import net.solarnetwork.central.ocpp.v16.vendor.abb.MeterTransferDataTransferDatumPublisher;
import net.solarnetwork.codec.JsonUtils;
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
 * Test cases for the {@link MeterTransferDataTransferDatumPublisher} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class MeterTransferDataTransferDatumPublisherTests {

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

	private MeterTransferDataTransferDatumPublisher publisher;

	@BeforeEach
	public void setup() {
		publisher = new MeterTransferDataTransferDatumPublisher(chargePointDao, chargePointSettingsDao,
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

	private String sourceId(CentralChargePoint cp) {
		return "/foo/" + cp.getInfo().getId() + "/meter";
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
		req.setVendorId(MeterTransferDataTransferDatumPublisher.VENDOR_ID);
		req.setMessageId(MeterTransferDataTransferDatumPublisher.MESSAGE_ID);
		req.setData(data);
		return req;
	}

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
		assertThat("Message is supported", supported, is(true));
	}

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
		assertThat("Message from other vendor is not supported", supported, is(false));
	}

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
		assertThat("Message with other message ID is not supported", supported, is(false));
	}

	@Test
	public void publishDatum_singlePhase() {
		// GIVEN
		final String dataTs = "2023-06-16T19:05:46.000Z";
		final String data = """
				{
				    "type": "MeterTransfer",
				    "timestamp": "%s",
				    "sampledValue": [
				        {
				            "measurand": "Voltage.L1",
				            "accuracy": "1",
				            "unit": "V",
				            "value": 2351
				        },
				        {
				            "measurand": "Current.L1",
				            "accuracy": "2",
				            "unit": "A",
				            "value": 7
				        },
				        {
				            "measurand": "Active.Power.ALL",
				            "accuracy": "2",
				            "unit": "W",
				            "value": 464
				        }
				    ]
				}
				""".formatted(dataTs);

		final CentralChargePoint cp = cp();
		final ChargePointIdentity cpi = cpi(cp);
		final ChargePointSettings cps = cps(cp);

		final String sourceId = sourceId(cp);
		final UUID streamId = UUID.randomUUID();

		givenPublishDatumAndFlux(cp, cpi, cps, streamId, true, true);

		// WHEN
		DataTransferRequest req = req(data);
		processDataTransfer(cpi, req);

		// THEN
		thenPublishedDatumAndFlux(true, true);
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Published datum node ID", d.getNodeId(), is(equalTo(cp.getNodeId())));
		assertThat("Published datum source ID", d.getSourceId(), is(equalTo(sourceId)));
		assertThat("Published datum ts", d.getCreated(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse(dataTs, Instant::from))));
		DatumSamples s = d.getSamples();
		assertThat("Published voltage_a", s.getInstantaneousSampleFloat("voltage_a"),
				is(equalTo(235.1f)));
		assertThat("Published current_a", s.getInstantaneousSampleFloat("current_a"),
				is(equalTo(0.07f)));
		assertThat("Published watts", s.getInstantaneousSampleFloat("watts"), equalTo(4.64f));

		assertThat("Published same datum to SolarFlux", fluxDatumCaptor.getValue(), sameInstance(d));
	}

}
