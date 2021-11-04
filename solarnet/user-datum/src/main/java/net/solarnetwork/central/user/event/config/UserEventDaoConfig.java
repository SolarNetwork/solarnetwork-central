/* ==================================================================
 * UserEventDaoConfig.java - 3/11/2021 3:33:52 PM
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

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.user.event.dao.UserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.dao.UserNodeEventTaskDao;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisDatumAppEventAcceptor;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisUserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisUserNodeEventTaskDao;

/**
 * Configuration for user event DAO.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserEventDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public DatumAppEventAcceptor datumAppEventAcceptor() {
		MyBatisDatumAppEventAcceptor dao = new MyBatisDatumAppEventAcceptor();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserNodeEventTaskDao userNodeEventTaskDao() {
		MyBatisUserNodeEventTaskDao dao = new MyBatisUserNodeEventTaskDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserNodeEventHookConfigurationDao userNodeEventHookConfigurationDao() {
		MyBatisUserNodeEventHookConfigurationDao dao = new MyBatisUserNodeEventHookConfigurationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
