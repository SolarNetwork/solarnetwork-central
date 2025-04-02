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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.SecurityTokenDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDaoSupport;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserAuthTokenFilter;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * MyBatis implementation of {@link UserAuthTokenDao}.
 *
 * @author matt
 * @version 2.4
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
	 * The query name used for
	 * {@link #createSnws2AuthorizationBuilder(String, Instant)}.
	 */
	public static final String QUERY_FOR_SIGNING_KEY = "get-snws2-signingkey-for-tokenid";

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
			List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		return doFindFiltered(filter, sorts, offset, max);
	}

	@Override
	public Snws2AuthorizationBuilder createSnws2AuthorizationBuilder(String tokenId,
			Instant signingDate) {
		Map<String, Object> params = new HashMap<>(2);
		params.put("id", tokenId);
		LocalDate date = signingDate.atZone(ZoneOffset.UTC).toLocalDate();
		java.sql.Date sqlDate = java.sql.Date.valueOf(date);
		params.put("date", sqlDate);
		log.debug("Requesting signing key for token {} with date {}", tokenId, sqlDate);
		Byte[] data = selectFirst(QUERY_FOR_SIGNING_KEY, params);
		if ( data == null || data.length < 1 ) {
			return null;
		}
		byte[] key = new byte[data.length];
		for ( int i = 0, len = data.length; i < len; i++ ) {
			key[i] = data[i];
		}
		return new Snws2AuthorizationBuilder(tokenId).date(signingDate).signingKey(key);
	}

	@Override
	public SecurityToken securityTokenForId(String tokenId) {
		return selectFirst(getQueryForId(), tokenId);
	}

}
