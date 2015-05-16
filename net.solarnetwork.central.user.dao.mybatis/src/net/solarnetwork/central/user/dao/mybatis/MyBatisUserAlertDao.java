/* ==================================================================
 * MyBatisUserAlertDao.java - 16/05/2015 4:23:44 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.mybatis;

import java.util.List;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertType;

/**
 * MyBatis implementation of {@link UserAlertDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserAlertDao extends BaseMyBatisGenericDao<UserAlert, Long> implements UserAlertDao {

	/**
	 * Default constructor.
	 */
	public MyBatisUserAlertDao() {
		super(net.solarnetwork.central.user.domain.UserAlert.class, Long.class);
	}

	@Override
	public List<UserAlert> findAlertsToProcess(UserAlertType type, Long startingId, Integer max) {
		// TODO Auto-generated method stub
		return null;
	}

}
