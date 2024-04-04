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

import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserMetadataDao;
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
public class InstructionInputServiceConfig implements InstructionInputConfiguration {

	@Autowired
	private InstructorBiz instructor;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Qualifier(CACHING)
	@Autowired
	private EndpointConfigurationDao endpointDao;

	@Qualifier(CACHING)
	@Autowired
	private TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;

	@Qualifier(CACHING)
	@Autowired
	private TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;

	@Qualifier(CACHING)
	@Autowired
	private UserMetadataDao userMetadataDao;

	@Autowired
	private Collection<RequestTransformService> requestTransformServices;

	@Autowired
	private Collection<ResponseTransformService> responseTransformServices;

	@Bean
	public DaoInstructionInputEndpointBiz instructionInputEndpointBiz() {
		var biz = new DaoInstructionInputEndpointBiz(instructor, nodeOwnershipDao, endpointDao,
				requestTransformDao, responseTransformDao, userMetadataDao, requestTransformServices,
				responseTransformServices);
		biz.setUserEventAppenderBiz(userEventAppenderBiz);
		return biz;
	}

}
