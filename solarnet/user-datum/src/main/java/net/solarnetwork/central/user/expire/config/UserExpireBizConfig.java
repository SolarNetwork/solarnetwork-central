/* ==================================================================
 * UserExpireBizConfig.java - 21/10/2021 9:02:10 AM
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

package net.solarnetwork.central.user.expire.config;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.expire.aop.UserExpireAopConfiguration;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.expire.biz.dao.DaoUserDatumDeleteBiz;
import net.solarnetwork.central.user.expire.biz.dao.DaoUserExpireBiz;
import net.solarnetwork.central.user.expire.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.support.PrefixedMessageSource;

/**
 * Configuration for user datum expire services.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = { UserExpireAopConfiguration.class })
public class UserExpireBizConfig {

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private DatumMaintenanceDao datumDao;

	@Autowired
	private UserDatumDeleteJobInfoDao jobInfoDao;

	@Autowired
	private UserDataConfigurationDao userDataConfigurationDao;

	@Value("${app.user.datum.delete.parallelism:1}")
	private int deleteTaskParallelism = 1;

	@Bean
	@ConfigurationProperties(prefix = "app.user.datum.delete")
	public DaoUserDatumDeleteBiz userDatumDeleteBiz() {
		// we don't care about thread-reuse here as delete tasks are infrequent
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("UserDatumDelete-");
		if ( deleteTaskParallelism > 0 ) {
			taskExecutor.setConcurrencyLimit(deleteTaskParallelism);
		}

		DaoUserDatumDeleteBiz biz = new DaoUserDatumDeleteBiz(taskExecutor, userNodeDao, datumDao,
				jobInfoDao);
		biz.setScheduler(taskScheduler);
		// TODO biz.setEventAdmin(null);
		return biz;
	}

	@Bean
	public UserExpireBiz userExpireBiz() {
		DaoUserExpireBiz biz = new DaoUserExpireBiz(userDataConfigurationDao);

		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasenames("net.solarnetwork.central.domain.Aggregation");
		PrefixedMessageSource prefixedMessageSource = new PrefixedMessageSource();
		prefixedMessageSource.setDelegates(Collections.singletonMap("aggregation.", messageSource));
		biz.setMessageSource(prefixedMessageSource);

		return biz;
	}

}
