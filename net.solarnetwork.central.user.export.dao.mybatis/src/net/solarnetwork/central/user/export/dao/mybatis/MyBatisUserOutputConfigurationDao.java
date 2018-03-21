/* ==================================================================
 * MyBatisUserOutputConfigurationDao.java - 21/03/2018 4:55:32 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.dao.mybatis;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;

/**
 * MyBatis implementation of {@link UserOutputConfigurationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserOutputConfigurationDao extends
		BaseMyBatisGenericDao<UserOutputConfiguration, Long> implements UserOutputConfigurationDao {

	/**
	 * Default constructor.
	 */
	public MyBatisUserOutputConfigurationDao() {
		super(UserOutputConfiguration.class, Long.class);
	}

}
