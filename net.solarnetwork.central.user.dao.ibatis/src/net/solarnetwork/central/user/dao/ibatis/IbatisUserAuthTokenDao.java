/* ==================================================================
 * IbatisUserAuthTokenDao.java - Dec 12, 2012 4:10:06 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.ibatis;

import java.util.List;
import net.solarnetwork.central.dao.ibatis.IbatisBaseGenericDaoSupport;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.domain.UserAuthToken;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * iBATIS implementation of {@link UserAuthTokenDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisUserAuthTokenDao extends IbatisBaseGenericDaoSupport<UserAuthToken, String> implements
		UserAuthTokenDao {

	/** The query name used for {@link #findUserAuthTokensForUser(Long)}. */
	public static final String QUERY_FOR_USER_ID = "find-UserAuthToken-for-UserID";

	/**
	 * Default constructor.
	 */
	public IbatisUserAuthTokenDao() {
		super(UserAuthToken.class, String.class);
	}

	@Override
	public List<UserAuthToken> findUserAuthTokensForUser(Long userId) {
		@SuppressWarnings("unchecked")
		List<UserAuthToken> results = getSqlMapClientTemplate().queryForList(QUERY_FOR_USER_ID, userId);
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String store(UserAuthToken datum) {
		// try update, then insert if that fails
		if ( getSqlMapClientTemplate().update(getUpdate(), datum) == 0 ) {
			preprocessInsert(datum);
			getSqlMapClientTemplate().insert(getInsert(), datum);
		}
		return datum.getId();
	}
}
