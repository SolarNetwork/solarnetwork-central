/* ==================================================================
 * OcppV16VendorActionConfig.java - 2/08/2022 6:53:32 am
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

package net.solarnetwork.central.ocpp.config;

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.v16.vendor.abb.MeterTransferDataTransferDatumPublisher;
import net.solarnetwork.central.ocpp.v16.vendor.hiconics.VehicleMacDataTransferDatumPublisher;
import net.solarnetwork.central.ocpp.v16.vendor.zjbeny.DlbMeterDataTransferDatumPublisher;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import ocpp.v16.jakarta.cs.DataTransferRequest;
import ocpp.v16.jakarta.cs.DataTransferResponse;

/**
 * OCPP v1.6 vendor-specific configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class OcppV16VendorActionConfig {

	@Autowired(required = false)
	@Qualifier("solarflux")
	private DatumProcessor fluxPublisher;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Autowired
	private CentralChargePointConnectorDao ocppCentralChargePointConnectorDao;

	@Autowired
	private ChargePointSettingsDao ocppChargePointSettingsDao;

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	@Order(0)
	public ActionMessageProcessor<DataTransferRequest, DataTransferResponse> ocppVendorZjbeny_DlbMeterDataTransferDatumPublisher_v16() {
		DlbMeterDataTransferDatumPublisher publisher = new DlbMeterDataTransferDatumPublisher(
				ocppCentralChargePointDao, ocppChargePointSettingsDao,
				ocppCentralChargePointConnectorDao, datumDao);
		publisher.setFluxPublisher(fluxPublisher);
		return publisher;
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	@Order(0)
	public ActionMessageProcessor<DataTransferRequest, DataTransferResponse> ocppVendorAbb_MeterTransferDataTransferDatumPublisher_v16(
			ObjectMapper mapper) {
		MeterTransferDataTransferDatumPublisher publisher = new MeterTransferDataTransferDatumPublisher(
				ocppCentralChargePointDao, ocppChargePointSettingsDao,
				ocppCentralChargePointConnectorDao, datumDao, mapper);
		publisher.setFluxPublisher(fluxPublisher);
		return publisher;
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	@Order(0)
	public ActionMessageProcessor<DataTransferRequest, DataTransferResponse> ocppVendorHiconics_VehicleMacDataTransferDatumPublisher_v16(
			ObjectMapper mapper) {
		VehicleMacDataTransferDatumPublisher publisher = new VehicleMacDataTransferDatumPublisher(
				ocppCentralChargePointDao, ocppChargePointSettingsDao,
				ocppCentralChargePointConnectorDao, datumDao, mapper);
		publisher.setFluxPublisher(fluxPublisher);
		return publisher;
	}

}
