/* ==================================================================
 * OcppDaoConfig.java - 12/11/2021 1:36:53 PM
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

package net.solarnetwork.central.ocpp.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisUserSettingsDao;

/**
 * OCPP DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class OcppDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public CentralAuthorizationDao ocppCentralAuthorizationDao() {
		MyBatisCentralAuthorizationDao dao = new MyBatisCentralAuthorizationDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public CentralChargePointConnectorDao ocppCentralChargePointConnectorDao() {
		MyBatisCentralChargePointConnectorDao dao = new MyBatisCentralChargePointConnectorDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public CentralChargePointDao ocppCentralChargePointDao() {
		MyBatisCentralChargePointDao dao = new MyBatisCentralChargePointDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public CentralChargeSessionDao ocppCentralChargeSessionDao() {
		MyBatisCentralChargeSessionDao dao = new MyBatisCentralChargeSessionDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public CentralSystemUserDao ocppCentralSystemUserDao() {
		MyBatisCentralSystemUserDao dao = new MyBatisCentralSystemUserDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public ChargePointSettingsDao ocppChargePointSettingsDao() {
		MyBatisChargePointSettingsDao dao = new MyBatisChargePointSettingsDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

	@Bean
	public UserSettingsDao ocppUserSettingsDao() {
		MyBatisUserSettingsDao dao = new MyBatisUserSettingsDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
