/* ==================================================================
 * DatumExportDaoConfig.java - 5/11/2021 9:41:42 AM
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

package net.solarnetwork.central.datum.export.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.dao.mybatis.MyBatisDatumExportTaskInfoDao;

/**
 * Datum export DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DatumExportDaoConfig {

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	@Bean
	public DatumExportTaskInfoDao datumExportTaskInfoDao() {
		MyBatisDatumExportTaskInfoDao dao = new MyBatisDatumExportTaskInfoDao();
		dao.setSqlSessionTemplate(sqlSessionTemplate);
		return dao;
	}

}
