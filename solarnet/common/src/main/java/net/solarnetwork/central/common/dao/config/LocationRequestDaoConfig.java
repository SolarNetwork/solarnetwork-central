/* ==================================================================
 * LocationRequestDaoConfig.java - 19/05/2022 4:59:25 pm
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

package net.solarnetwork.central.common.dao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.LocationRequestDao;
import net.solarnetwork.central.common.dao.jdbc.JdbcLocationRequestDao;

/**
 * Configuration for the {@link LocationRequestDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
@Configuration
public class LocationRequestDaoConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Bean
	public LocationRequestDao locationRequestDao() {
		return new JdbcLocationRequestDao(jdbcOperations);
	}

}
