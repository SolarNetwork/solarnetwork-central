/* ==================================================================
 * MyBatisSolarNodeDaoConfig.java - 5/10/2021 7:19:12 AM
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

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarLocationDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeMetadataDao;

/**
 * SolarNode DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class MyBatisSolarNodeDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public SolarNodeDao solarNodeDao() {
		MyBatisSolarNodeDao dao = new MyBatisSolarNodeDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public SolarLocationDao solarLocationDao() {
		MyBatisSolarLocationDao dao = new MyBatisSolarLocationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public SolarNodeMetadataDao solarNodeMetadataDao() {
		MyBatisSolarNodeMetadataDao dao = new MyBatisSolarNodeMetadataDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
