/* ==================================================================
 * MyBatisUserAuthTokenDao.java - Nov 11, 2014 6:53:48 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.SecurityTokenDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDaoSupport;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserAuthTokenFilter;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link UserAuthTokenDao}.
 *
 * @author matt
 * @version 2.5
 */
public class MyBatisUserAuthTokenDao extends
		BaseMyBatisFilterableDaoSupport<UserAuthToken, String, UserAuthToken, UserAuthTokenFilter>
		implements UserAuthTokenDao, SecurityTokenDao {

	/** The query name used for {@link #findUserAuthTokensForUser(Long)}. */
	public static final String QUERY_FOR_USER_ID = "find-UserAuthToken-for-UserID";

	/**
	 * The query name used for {@link #findUserAuthTokensForUser(Long)}.
	 */
	public static final String QUERY_FOR_FILTER = "find-UserAuthToken-for-filter";

	/**
	 * Default constructor.
	 */
	public MyBatisUserAuthTokenDao() {
		super(UserAuthToken.class, String.class, UserAuthToken.class);
	}

	@Override
	protected boolean isAssignedPrimaryKeys() {
		return true;
	}

	@Override
	public List<UserAuthToken> findUserAuthTokensForUser(Long userId) {
		return getSqlSession().selectList(QUERY_FOR_USER_ID, userId);
	}

	@Override
	public FilterResults<UserAuthToken, String> findFiltered(UserAuthTokenFilter filter,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		return doFindFiltered(filter, sorts, offset, max);
	}

	@Override
	public @Nullable SecurityToken securityTokenForId(String tokenId) {
		return selectFirst(getQueryForId(), tokenId);
	}

}
