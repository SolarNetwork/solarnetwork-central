/* ==================================================================
 * UserDatumStreamAliasBizConfig.java - 1/04/2026 7:44:27 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.stream.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.Validator;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.user.datum.stream.biz.impl.DaoUserDatumStreamAliasBiz;

/**
 * JDBC object datum stream alias entity DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class UserDatumStreamAliasBizConfig {

	@Autowired
	private Validator validator;

	@Bean
	public DaoUserDatumStreamAliasBiz userDatumStreamAliasBiz(ObjectDatumStreamAliasEntityDao aliasDao) {
		var biz = new DaoUserDatumStreamAliasBiz(aliasDao);
		biz.setValidator(validator);
		return biz;
	}

}
