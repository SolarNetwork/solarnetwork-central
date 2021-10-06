/* ==================================================================
 * UserDaoConfig.java - 7/10/2021 11:29:01 AM
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

package net.solarnetwork.central.user.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserMetadataDao;
import net.solarnetwork.central.user.dao.UserNodeCertificateDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAlertDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAlertSituationDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAuthTokenDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserMetadataDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeCertificateDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeConfirmationDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeDao;

/**
 * Configuration for user DAO implementations.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public UserAlertDao userAlertDao() {
		MyBatisUserAlertDao dao = new MyBatisUserAlertDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserAlertSituationDao userAlertSituationDao() {
		MyBatisUserAlertSituationDao dao = new MyBatisUserAlertSituationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserAuthTokenDao userAuthTokenDao() {
		MyBatisUserAuthTokenDao dao = new MyBatisUserAuthTokenDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserDao userDao() {
		MyBatisUserDao dao = new MyBatisUserDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserMetadataDao userMetadataDao() {
		MyBatisUserMetadataDao dao = new MyBatisUserMetadataDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserNodeCertificateDao userNodeCertificateDao() {
		MyBatisUserNodeCertificateDao dao = new MyBatisUserNodeCertificateDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserNodeConfirmationDao userNodeConfirmationDao() {
		MyBatisUserNodeConfirmationDao dao = new MyBatisUserNodeConfirmationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserNodeDao userNodeDao() {
		MyBatisUserNodeDao dao = new MyBatisUserNodeDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
