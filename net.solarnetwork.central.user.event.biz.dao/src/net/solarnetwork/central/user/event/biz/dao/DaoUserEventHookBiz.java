/* ==================================================================
 * DaoUserEventHookBiz.java - 11/06/2020 9:34:34 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.biz.dao;

import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.domain.UserRelatedIdentifiableConfiguration;
import net.solarnetwork.central.user.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.event.dao.UserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;

/**
 * DAO implementation of {@link UserEventHookBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserEventHookBiz implements UserEventHookBiz {

	private final UserNodeEventHookConfigurationDao nodeEventHookConfigurationDao;

	/**
	 * Constructor.
	 * 
	 * @param nodeEventHookConfigurationDao
	 *        the node event hook DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserEventHookBiz(UserNodeEventHookConfigurationDao nodeEventHookConfigurationDao) {
		super();
		if ( nodeEventHookConfigurationDao == null ) {
			throw new IllegalArgumentException(
					"The nodeEventHookConfigurationDao argument must not be null.");
		}
		this.nodeEventHookConfigurationDao = nodeEventHookConfigurationDao;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserRelatedIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		if ( userId == null ) {
			throw new IllegalArgumentException("The userId argument must not be null.");
		}
		if ( id == null ) {
			throw new IllegalArgumentException("The id argument must not be null.");
		}
		if ( UserNodeEventHookConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) nodeEventHookConfigurationDao.get(new UserLongPK(userId, id));
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserLongPK saveConfiguration(UserRelatedIdentifiableConfiguration configuration) {
		if ( configuration instanceof UserNodeEventHookConfiguration ) {
			return nodeEventHookConfigurationDao.save((UserNodeEventHookConfiguration) configuration);
		}
		throw new IllegalArgumentException("Unsupported configuration type: " + configuration);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteConfiguration(UserRelatedIdentifiableConfiguration configuration) {
		if ( configuration instanceof UserNodeEventHookConfiguration ) {
			nodeEventHookConfigurationDao.delete((UserNodeEventHookConfiguration) configuration);
		} else {
			throw new IllegalArgumentException("Unsupported configuration type: " + configuration);
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserRelatedIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass) {
		if ( UserNodeEventHookConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) nodeEventHookConfigurationDao.findConfigurationsForUser(userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

}
