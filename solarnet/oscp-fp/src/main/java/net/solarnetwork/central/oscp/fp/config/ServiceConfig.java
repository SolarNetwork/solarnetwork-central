/* ==================================================================
 * ServiceConfig.java - 17/08/2022 2:47:46 pm
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

package net.solarnetwork.central.oscp.fp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.fp.biz.dao.DaoFlexibilityProviderBiz;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.http.RestOpsExternalSystemClient;

/**
 * Service configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class ServiceConfig {

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private FlexibilityProviderDao flexibilityProviderDao;

	@Autowired
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Autowired
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Autowired
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Autowired
	private AsyncTaskExecutor executor;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private RestOperations restOps;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private OAuth2AuthorizedClientManager oauthClientManager;

	@Bean
	public RestOpsExternalSystemClient externalSystemClient() {
		var service = new RestOpsExternalSystemClient(restOps, userEventAppenderBiz);
		service.setOauthClientManager(oauthClientManager);
		return service;
	}

	/**
	 * Get the flexibility provider service.
	 * 
	 * @return the service
	 */
	@Bean
	public DaoFlexibilityProviderBiz flexibilityProviderBiz(ExternalSystemClient client) {
		var biz = new DaoFlexibilityProviderBiz(executor, client, userEventAppenderBiz,
				flexibilityProviderDao, capacityProviderDao, capacityOptimizerDao, capacityGroupDao);
		biz.setTxTemplate(transactionTemplate);
		biz.setTaskScheduler(taskScheduler);
		return biz;
	}

}
