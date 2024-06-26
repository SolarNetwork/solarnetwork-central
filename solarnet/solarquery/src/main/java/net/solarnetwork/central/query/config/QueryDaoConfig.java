/* ==================================================================
 * QueryDaoConfig.java - 9/10/2021 4:51:51 PM
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

package net.solarnetwork.central.query.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.dao.mybatis.MyBatisUserMetadataDao;

/**
 * Query DAO configutation;
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class QueryDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public UserMetadataDao userMetadataDao() {
		MyBatisUserMetadataDao dao = new MyBatisUserMetadataDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
