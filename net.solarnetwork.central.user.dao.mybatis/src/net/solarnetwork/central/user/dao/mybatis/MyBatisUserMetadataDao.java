/* ==================================================================
 * MyBatisUserMetadataDao.java - 11/11/2016 5:42:00 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDao;
import net.solarnetwork.central.user.dao.UserMetadataDao;
import net.solarnetwork.central.user.domain.UserMetadataEntity;
import net.solarnetwork.central.user.domain.UserMetadataFilter;
import net.solarnetwork.central.user.domain.UserMetadataFilterMatch;
import net.solarnetwork.central.user.domain.UserMetadataMatch;

/**
 * MyBatis implementation of {@link UserMetadataDao}.
 * 
 * @author matt
 * @version 1.1
 * @since 1.8
 */
public class MyBatisUserMetadataDao extends
		BaseMyBatisFilterableDao<UserMetadataEntity, UserMetadataFilterMatch, UserMetadataFilter, Long>
		implements UserMetadataDao {

	/**
	 * The query parameter for a general {@link Filter} object value.
	 * 
	 * @deprecated use {@link BaseMyBatisFilterableDao#FILTER_PROPERTY}
	 */
	@Deprecated
	public static final String PARAM_FILTER = BaseMyBatisFilterableDao.FILTER_PROPERTY;

	/**
	 * Default constructor.
	 */
	public MyBatisUserMetadataDao() {
		super(UserMetadataEntity.class, Long.class, UserMetadataMatch.class);
	}

}
