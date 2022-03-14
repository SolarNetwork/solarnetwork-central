/* ==================================================================
 * UserExportDaoConfig.java - 5/11/2021 9:07:52 AM
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

package net.solarnetwork.central.user.export.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserOutputConfigurationDao;

/**
 * User export DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserExportDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public UserDatumExportConfigurationDao userDatumExportConfigurationDao() {
		MyBatisUserDatumExportConfigurationDao dao = new MyBatisUserDatumExportConfigurationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserDataConfigurationDao userDataConfigurationDao() {
		MyBatisUserDataConfigurationDao dao = new MyBatisUserDataConfigurationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserDestinationConfigurationDao userDestinationConfigurationDao() {
		MyBatisUserDestinationConfigurationDao dao = new MyBatisUserDestinationConfigurationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserOutputConfigurationDao userOutputConfigurationDao() {
		MyBatisUserOutputConfigurationDao dao = new MyBatisUserOutputConfigurationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserDatumExportTaskInfoDao userDatumExportTaskInfoDao() {
		MyBatisUserDatumExportTaskInfoDao dao = new MyBatisUserDatumExportTaskInfoDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserAdhocDatumExportTaskInfoDao userAdhocDatumExportTaskInfoDao() {
		MyBatisUserAdhocDatumExportTaskInfoDao dao = new MyBatisUserAdhocDatumExportTaskInfoDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
