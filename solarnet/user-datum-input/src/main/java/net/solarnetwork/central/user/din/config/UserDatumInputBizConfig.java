/* ==================================================================
 * UserDatumInputBizConfig.java - 25/02/2024 11:26:41 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.din.config;

import static net.solarnetwork.central.din.config.SolarNetDatumInputConfiguration.DATUM_INPUT;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import jakarta.validation.Validator;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.dao.CredentialConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.user.din.biz.impl.DaoUserDatumInputBiz;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Configuration for User datum input services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(DATUM_INPUT)
public class UserDatumInputBizConfig {

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private CredentialConfigurationDao credentialDao;

	@Autowired
	private TransformConfigurationDao transformDao;

	@Autowired
	private EndpointConfigurationDao endpointDao;

	@Autowired
	private EndpointAuthConfigurationDao endpointAuthDao;

	@Autowired
	private Collection<TransformService> transformServices;

	@Autowired
	private Validator validator;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Bean
	public DaoUserDatumInputBiz userDatumInputBiz() {
		DaoUserDatumInputBiz biz = new DaoUserDatumInputBiz(nodeOwnershipDao, credentialDao,
				transformDao, endpointDao, endpointAuthDao, transformServices);
		biz.setValidator(validator);
		biz.setPasswordEncoder(passwordEncoder);
		return biz;
	}

}
