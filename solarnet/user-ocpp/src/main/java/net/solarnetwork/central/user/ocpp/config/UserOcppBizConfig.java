/* ==================================================================
 * UserOcppBizConfig.java - 12/05/2022 9:38:12 am
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

package net.solarnetwork.central.user.ocpp.config;

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.user.ocpp.biz.dao.DaoUserOcppBiz;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Configuration for User OCPP services.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OCPP_V16)
public class UserOcppBizConfig {

	@Autowired
	private CentralSystemUserDao systemUserDao;

	@Autowired
	private CentralChargePointDao chargePointDao;

	@Autowired
	private CentralChargePointConnectorDao connectorDao;

	@Autowired
	private CentralAuthorizationDao authorizationDao;

	@Autowired
	private CentralChargeSessionDao chargeSessionDao;

	@Autowired
	private UserSettingsDao userSettingsDao;

	@Autowired
	private ChargePointSettingsDao chargePointSettingsDao;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Bean
	public DaoUserOcppBiz userOcppBiz() {
		DaoUserOcppBiz biz = new DaoUserOcppBiz(systemUserDao, chargePointDao, connectorDao,
				authorizationDao, chargeSessionDao, userSettingsDao, chargePointSettingsDao,
				passwordEncoder);
		return biz;
	}

}
