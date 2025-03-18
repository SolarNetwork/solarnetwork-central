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

import java.util.Collection;
import java.util.List;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.dao.mybatis.BaseMyBatisUserRelatedGenericDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link UserOutputConfigurationDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class MyBatisUserOutputConfigurationDao
		extends BaseMyBatisUserRelatedGenericDao<UserOutputConfiguration, UserLongCompositePK>
		implements UserOutputConfigurationDao {

	/** The query name used for {@link #findConfigurationsForUser(Long)}. */
	public static final String QUERY_CONFIGURATIONS_FOR_USER = "find-UserOutputConfiguration-for-user";

	/**
	 * Default constructor.
	 */
	public MyBatisUserOutputConfigurationDao() {
		super(UserOutputConfiguration.class, UserLongCompositePK.class);
	}

	@Override
	public List<UserOutputConfiguration> findConfigurationsForUser(Long userId) {
		return selectList(QUERY_CONFIGURATIONS_FOR_USER, userId, null, null);
	}

	@Override
	public UserLongCompositePK create(Long userId, UserOutputConfiguration entity) {
		if ( !userId.equals(entity.getUserId()) ) {
			entity = entity.copyWithId(new UserLongCompositePK(userId, entity.getConfigId()));
		}
		return save(entity);
	}

	@Override
	protected UserLongCompositePK handleInsert(UserOutputConfiguration datum) {
		UserLongCompositePK id = super.handleInsert(datum);
		if ( !id.entityIdIsAssigned() && datum.getConfigId() != null ) {
			id = new UserLongCompositePK(id.getUserId(), datum.getConfigId());
		}
		return id;
	}

	@Override
	public Collection<UserOutputConfiguration> findAll(Long userId, List<SortDescriptor> sorts) {
		return findConfigurationsForUser(userId);
	}

}
