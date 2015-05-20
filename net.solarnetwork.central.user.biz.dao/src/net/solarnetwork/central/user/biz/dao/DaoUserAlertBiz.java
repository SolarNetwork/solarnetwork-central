/* ==================================================================
 * DaoUserAlertBiz.java - 19/05/2015 8:41:29 pm
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

package net.solarnetwork.central.user.biz.dao;

import java.util.List;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link UserAlertBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserAlertBiz implements UserAlertBiz {

	private final UserAlertDao userAlertDao;
	private final UserAlertSituationDao userAlertSituationDao;

	public DaoUserAlertBiz(UserAlertDao userAlertDao, UserAlertSituationDao userAlertSituationDao) {
		super();
		this.userAlertDao = userAlertDao;
		this.userAlertSituationDao = userAlertSituationDao;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserAlert> userAlertsForUser(Long userId) {
		return userAlertDao.findAlertsForUser(userId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long saveAlert(UserAlert alert) {
		return userAlertDao.store(alert);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserAlert alertSituation(Long alertId) {
		return userAlertDao.getAlertSituation(alertId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserAlert updateSituationStatus(Long alertId, UserAlertSituationStatus status) {
		UserAlert alert = alertSituation(alertId);
		if ( alert != null && alert.getSituation() != null
				&& !alert.getSituation().getStatus().equals(status) ) {
			alert.getSituation().setStatus(status);
			userAlertSituationDao.store(alert.getSituation());
		}
		return alert;
	}

}
