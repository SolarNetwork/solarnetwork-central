/* ==================================================================
 * AppWarmupConfig.java - 3/07/2024 5:16:36 pm
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

package net.solarnetwork.central.common.config;

import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.biz.AppWarmUpManager;
import net.solarnetwork.central.biz.AppWarmUpTask;

/**
 * Configuration for application warm-up tasks.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(AppWarmUpTask.WARMUP)
public class AppWarmupConfig {

	/**
	 * The warm-up manager service.
	 * 
	 * @param tasks
	 *        the tasks to execute
	 * @return the manager
	 */
	@Bean(initMethod = "serviceDidStartup")
	public AppWarmUpManager warmUpManager(Collection<AppWarmUpTask> tasks) {
		return new AppWarmUpManager(tasks);
	}

}
