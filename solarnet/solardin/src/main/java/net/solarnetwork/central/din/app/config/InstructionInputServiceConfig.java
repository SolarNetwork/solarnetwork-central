/* ==================================================================
 * InstructionInputServiceConfig.java - 30/03/2024 4:17:40 pm
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

package net.solarnetwork.central.din.app.config;

import static net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT_REQUEST;
import static net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT_RESPONSE;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.inin.biz.RequestTransformService;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.inin.biz.impl.DaoInstructionInputEndpointBiz;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.instructor.biz.InstructorBiz;

/**
 * Core instruction service configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class InstructionInputServiceConfig {

	@Autowired
	private InstructorBiz instructor;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private EndpointConfigurationDao endpointDao;

	@Autowired
	@Qualifier(INSTRUCTION_INPUT_REQUEST)
	private TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;

	@Autowired
	@Qualifier(INSTRUCTION_INPUT_RESPONSE)
	private TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;

	@Autowired
	private Collection<RequestTransformService> requestTransformServices;

	@Autowired
	private Collection<ResponseTransformService> responseTransformServices;

	@Bean
	public DaoInstructionInputEndpointBiz instructionInputEndpointBiz() {
		var biz = new DaoInstructionInputEndpointBiz(instructor, nodeOwnershipDao, endpointDao,
				requestTransformDao, responseTransformDao, requestTransformServices,
				responseTransformServices);
		biz.setUserEventAppenderBiz(userEventAppenderBiz);
		return biz;
	}

}
