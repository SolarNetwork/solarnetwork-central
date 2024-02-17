/* ==================================================================
 * OcppChargeSessionManagerConfig.java - 12/11/2021 3:41:12 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_CHARGE_SESSION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.session.OcppSessionDatumManager;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.ocpp.service.cs.ChargeSessionManager;

/**
 * OCPP charge session manager configuration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration
@Profile(OCPP_CHARGE_SESSION)
public class OcppChargeSessionManagerConfig {

	@Autowired
	private AuthorizationService ocppAuthorizationService;

	@Autowired
	private ChargePointDao ocppChargePointDao;

	@Autowired
	private ChargePointSettingsDao ocppChargePointSettingsDao;

	@Autowired
	private CentralChargeSessionDao ocppChargeSessionDao;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired(required = false)
	@Qualifier("solarflux")
	private DatumProcessor fluxPublisher;

	@ConfigurationProperties(prefix = "app.ocpp.session-datum")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public ChargeSessionManager ocppSessionDatumManager() {
		OcppSessionDatumManager manager = new OcppSessionDatumManager(ocppAuthorizationService,
				ocppChargePointDao, ocppChargeSessionDao, datumDao, ocppChargePointSettingsDao);
		manager.setFluxPublisher(fluxPublisher);
		manager.setTaskScheduler(taskScheduler);
		return manager;
	}

}
