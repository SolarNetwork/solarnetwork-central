/* ==================================================================
 * UserAuthTokenDao.java - Dec 12, 2012 2:05:11 PM
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

package net.solarnetwork.central.user.dao;

import java.util.List;
import org.joda.time.DateTime;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * DAO API for {@link UserAuthToken} entities.
 * 
 * @author matt
 * @version 2.0
 */
public interface UserAuthTokenDao extends GenericDao<UserAuthToken, String> {

	/**
	 * Find a list of all UserNodeAuthToken objects for a particular user.
	 * 
	 * @param userId
	 *        the user ID to get all tokens for
	 * @return list of {@link UserAuthToken} objects, or an empty list if none
	 *         found
	 */
	List<UserAuthToken> findUserAuthTokensForUser(Long userId);

	/**
	 * Create a new {@link Snws2AuthorizationBuilder} for a given token.
	 * 
	 * <p>
	 * The returned builder will have a signing key populated.
	 * </p>
	 * 
	 * @param tokenId
	 *        the token ID to get a builder for
	 * @param signingDate
	 *        the date to use in the signing key
	 * @return the builder, or {@literal null} if the given {@code tokenId} is
	 *         not found
	 * @since 2.0
	 */
	public Snws2AuthorizationBuilder createSnws2AuthorizationBuilder(String tokenId,
			DateTime signingDate);

}
