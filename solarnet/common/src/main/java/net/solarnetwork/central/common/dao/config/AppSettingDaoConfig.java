/* ==================================================================
 * AppSettingDaoConfig.java - 10/11/2021 11:35:43 AM
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

package net.solarnetwork.central.common.dao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.JdbcAppSettingDao;
import net.solarnetwork.central.dao.AppSettingDao;

/**
 * App setting DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class AppSettingDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Bean
	public AppSettingDao appSettingDao() {
		return new JdbcAppSettingDao(jdbcOperations);
	}

}
