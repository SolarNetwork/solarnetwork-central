/* ==================================================================
 * UserInstructionInputBizConfig.java - 29/03/2024 12:28:21 pm
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

package net.solarnetwork.central.user.inin.config;

import static net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT;
import static net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT_REQUEST;
import static net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT_RESPONSE;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import jakarta.validation.Validator;
import net.solarnetwork.central.inin.biz.RequestTransformService;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.inin.dao.CredentialConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointAuthConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.user.inin.biz.impl.DaoUserInstructionInputBiz;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Configuration for User datum input services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(INSTRUCTION_INPUT)
public class UserInstructionInputBizConfig {

	@Autowired
	private CredentialConfigurationDao credentialDao;

	@Autowired
	@Qualifier(INSTRUCTION_INPUT_REQUEST)
	private TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;

	@Autowired
	@Qualifier(INSTRUCTION_INPUT_RESPONSE)
	private TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;

	@Autowired
	private EndpointConfigurationDao endpointDao;

	@Autowired
	private EndpointAuthConfigurationDao endpointAuthDao;

	@Autowired
	private Collection<RequestTransformService> requestTransformServices;

	@Autowired
	private Collection<ResponseTransformService> responseTransformServices;

	@Autowired
	private Validator validator;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Bean
	public DaoUserInstructionInputBiz userInstructionInputBiz() {
		DaoUserInstructionInputBiz biz = new DaoUserInstructionInputBiz(credentialDao,
				requestTransformDao, responseTransformDao, endpointDao, endpointAuthDao,
				requestTransformServices, responseTransformServices);
		biz.setValidator(validator);
		biz.setPasswordEncoder(passwordEncoder);
		return biz;
	}

}
