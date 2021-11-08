/* ==================================================================
 * UserEventHookBizConfig.java - 8/11/2021 5:09:50 PM
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

package net.solarnetwork.central.user.event.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import net.solarnetwork.central.datum.biz.DatumAppEventProducer;
import net.solarnetwork.central.user.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.biz.dao.DaoUserEventHookBiz;
import net.solarnetwork.central.user.event.dao.UserNodeEventHookConfigurationDao;

/**
 * User event hook service configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Service
public class UserEventHookBizConfig {

	@Autowired
	private UserNodeEventHookConfigurationDao userNodeEventHookConfiguationDao;

	@Autowired
	private List<DatumAppEventProducer> datumAppEventProducers;

	@Autowired
	private List<UserNodeEventHookService> userNodeEventHookServices;

	@Bean
	public UserEventHookBiz userEventHookBiz() {
		DaoUserEventHookBiz biz = new DaoUserEventHookBiz(userNodeEventHookConfiguationDao);
		biz.setDatumEventProducers(datumAppEventProducers);
		biz.setNodeEventHookServices(userNodeEventHookServices);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames("net.solarnetwork.central.user.event.biz.UserEventHook");
		biz.setMessageSource(msgSource);

		return biz;
	}

}
