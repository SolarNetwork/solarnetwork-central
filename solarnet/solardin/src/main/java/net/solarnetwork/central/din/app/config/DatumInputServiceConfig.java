/* ==================================================================
 * DatumInputServiceConfig.java - 24/02/2024 8:26:42 am
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

import static net.solarnetwork.central.din.app.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.biz.impl.DaoDatumInputEndpointBiz;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.InputDataEntityDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;

/**
 * Core service configuration.
 *
 * @author matt
 * @version 1.2
 */
@Configuration(proxyBeanMethods = false)
public class DatumInputServiceConfig implements DatumInputConfiguration {

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Qualifier(CACHING)
	@Autowired
	private TransformConfigurationDao transformDao;

	@Qualifier(CACHING)
	@Autowired
	private EndpointConfigurationDao endpointDao;

	@Autowired
	private InputDataEntityDao inputDataDao;

	@Autowired
	private Collection<TransformService> transformServices;

	@Autowired(required = false)
	@Qualifier(SOLARFLUX)
	private DatumProcessor fluxPublisher;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Bean
	public DaoDatumInputEndpointBiz datumInputEndpointBiz(DatumWriteOnlyDao datumDao) {
		var biz = new DaoDatumInputEndpointBiz(nodeOwnershipDao, endpointDao, transformDao, datumDao,
				inputDataDao, transformServices);
		biz.setFluxPublisher(fluxPublisher);
		biz.setUserEventAppenderBiz(userEventAppenderBiz);
		return biz;
	}

}
