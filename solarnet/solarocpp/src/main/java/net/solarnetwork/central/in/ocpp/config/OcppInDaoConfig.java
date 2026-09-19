/* ==================================================================
 * OcppInDaoConfig.java - 2/07/2024 3:42:25 pm
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

package net.solarnetwork.central.in.ocpp.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.central.ocpp.dao.AsyncChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusDao;

/**
 * OCPP DAO configuration.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class OcppInDaoConfig {

	@Autowired
	private DataSource dataSource;

	@ConfigurationProperties(prefix = "app.ocpp.async-status-updater")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	@Primary
	public AsyncChargePointStatusDao ocppAsyncChargePointStatusDao(TaskScheduler taskScheduler,
			ChargePointStatusDao delegate) {
		return new AsyncChargePointStatusDao(taskScheduler, delegate);
	}

	@ConfigurationProperties(prefix = "app.ocpp.async-action-status-updater")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public AsyncJdbcChargePointActionStatusDao ocppChargePointActionStatusUpdateDao() {
		return new AsyncJdbcChargePointActionStatusDao(dataSource);
	}

}
