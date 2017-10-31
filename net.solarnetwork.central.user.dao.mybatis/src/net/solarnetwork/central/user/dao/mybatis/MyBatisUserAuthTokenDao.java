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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.web.security.AuthorizationV2Builder;

/**
 * MyBatis implementation of {@link UserAuthTokenDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class MyBatisUserAuthTokenDao extends BaseMyBatisGenericDao<UserAuthToken, String>
		implements UserAuthTokenDao {

	/** The query name used for {@link #findUserAuthTokensForUser(Long)}. */
	public static final String QUERY_FOR_USER_ID = "find-UserAuthToken-for-UserID";

	/**
	 * The query name used for
	 * {@link #createAuthorizationV2Builder(String, DateTime)}.
	 */
	public static final String QUERY_FOR_SIGNING_KEY = "get-snws2-signingkey-for-tokenid";

	/**
	 * Default constructor.
	 */
	public MyBatisUserAuthTokenDao() {
		super(UserAuthToken.class, String.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserAuthToken> findUserAuthTokensForUser(Long userId) {
		return getSqlSession().selectList(QUERY_FOR_USER_ID, userId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String store(final UserAuthToken datum) {
		final String pk = handleAssignedPrimaryKeyStore(datum);
		return pk;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public AuthorizationV2Builder createAuthorizationV2Builder(String tokenId, DateTime signingDate) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("id", tokenId);
		long date = signingDate.withZone(DateTimeZone.UTC).toLocalDate()
				.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis();
		params.put("date", new java.sql.Date(date));
		Byte[] data = selectFirst(QUERY_FOR_SIGNING_KEY, params);
		if ( data == null || data.length < 1 ) {
			return null;
		}
		byte[] key = new byte[data.length];
		for ( int i = 0, len = data.length; i < len; i++ ) {
			key[i] = data[i].byteValue();
		}
		AuthorizationV2Builder builder = new AuthorizationV2Builder(tokenId).date(signingDate.toDate())
				.signingKey(key);
		return builder;
	}

}
