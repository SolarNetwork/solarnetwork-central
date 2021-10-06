/* ==================================================================
 * SolarNodeOwnershipDaoConfig.java - 6/10/2021 7:27:25 AM
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

import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * JDBC datum support DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SolarNodeOwnershipDaoConfig {

	/**
	 * A qualifier to use for the node ownership {@link Cache}.
	 */
	public static final String NODE_OWNERSHIP_CACHE = "ownership-for-node";

	/**
	 * A qualifier to use for the {@link JdbcOperations}.
	 */
	public static final String CENTRAL_JDBC_OPERATIONS = "central";

	@Autowired
	@Qualifier(CENTRAL_JDBC_OPERATIONS)
	private JdbcOperations jdbcOperations;

	@Autowired
	@Qualifier(NODE_OWNERSHIP_CACHE)
	private Cache<Long, SolarNodeOwnership> nodeOwnershipCache;

	@Bean
	public SolarNodeOwnershipDao nodeOwnershipDao() {
		JdbcSolarNodeOwnershipDao dao = new JdbcSolarNodeOwnershipDao(jdbcOperations);
		dao.setUserNodeCache(nodeOwnershipCache);
		return dao;
	}

}
