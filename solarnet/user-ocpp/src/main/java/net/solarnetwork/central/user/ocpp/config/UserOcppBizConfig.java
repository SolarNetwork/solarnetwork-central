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
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.Validator;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.support.DelegatingValidator;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.central.user.ocpp.biz.dao.CentralAuthorizationValidator;
import net.solarnetwork.central.user.ocpp.biz.dao.CentralChargePointConnectorValidator;
import net.solarnetwork.central.user.ocpp.biz.dao.CentralChargePointValidator;
import net.solarnetwork.central.user.ocpp.biz.dao.CentralSystemUserValidator;
import net.solarnetwork.central.user.ocpp.biz.dao.ChargePointActionStatusFilterValidator;
import net.solarnetwork.central.user.ocpp.biz.dao.ChargePointSettingsValidator;
import net.solarnetwork.central.user.ocpp.biz.dao.ChargePointStatusFilterValidator;
import net.solarnetwork.central.user.ocpp.biz.dao.DaoUserOcppBiz;
import net.solarnetwork.central.user.ocpp.biz.dao.UserSettingsValidator;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Configuration for User OCPP services.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile(OCPP_V16)
public class UserOcppBizConfig {

	/** A qualifier used for charge point status filter components. */
	public static final String CHARGE_POINT_STATUS_FILTER = "charge-point-status-filter";

	/** A qualifier used for charge point action status filter components. */
	public static final String CHARGE_POINT_ACTION_STATUS_FILTER = "charge-point-action-status-filter";

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
	private ChargePointStatusDao chargePointStatusDao;

	@Autowired
	private ChargePointActionStatusDao chargePointActionStatusDao;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Bean
	@Qualifier(CHARGE_POINT_STATUS_FILTER)
	public Validator userOcppChargePointStatusFilterValidator() {
		return new ChargePointStatusFilterValidator();
	}

	@Bean
	@Qualifier(CHARGE_POINT_ACTION_STATUS_FILTER)
	public Validator userOcppChargePointActionStatusFilterValidator() {
		return new ChargePointActionStatusFilterValidator();
	}

	@Bean
	@Qualifier(OCPP_V16)
	public Validator userOcppValidator() {
		Map<String, Validator> vals = new HashMap<>(8);
		vals.put(CentralAuthorization.class.getName(), new CentralAuthorizationValidator());
		vals.put(CentralChargePoint.class.getName(), new CentralChargePointValidator());
		vals.put(CentralChargePointConnector.class.getName(),
				new CentralChargePointConnectorValidator());
		vals.put(CentralSystemUser.class.getName(), new CentralSystemUserValidator());
		vals.put(ChargePointSettings.class.getName(), new ChargePointSettingsValidator());
		vals.put(UserSettings.class.getName(), new UserSettingsValidator());
		return new DelegatingValidator(vals);
	}

	/**
	 * The {@link UserOcppBiz} implementation.
	 * 
	 * @param userOcppValidator
	 *        the user validator to use
	 * @param chargePointStatusFilterValidator
	 *        the charge point status filter validator to use
	 * @param chargePointActionStatusFilterValidator
	 *        the charge point action status filter to use
	 * @return the service
	 */
	@Bean
	public DaoUserOcppBiz userOcppBiz(@Qualifier(OCPP_V16) Validator userOcppValidator,
			@Qualifier(CHARGE_POINT_STATUS_FILTER) Validator chargePointStatusFilterValidator,
			@Qualifier(CHARGE_POINT_ACTION_STATUS_FILTER) Validator chargePointActionStatusFilterValidator) {
		DaoUserOcppBiz biz = new DaoUserOcppBiz(systemUserDao, chargePointDao, connectorDao,
				authorizationDao, chargeSessionDao, userSettingsDao, chargePointSettingsDao,
				chargePointStatusDao, chargePointActionStatusDao, passwordEncoder);
		biz.setValidator(userOcppValidator);
		biz.setChargePointStatusFilterValidator(chargePointStatusFilterValidator);
		biz.setChargePointActionStatusFilterValidator(chargePointActionStatusFilterValidator);
		return biz;
	}

}
