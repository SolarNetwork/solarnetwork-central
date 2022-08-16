/* ==================================================================
 * DaoFlexibilityProviderBiz.java - 16/08/2022 5:25:42 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;

/**
 * DAO based implementation of {@link FlexibilityProviderBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFlexibilityProviderBiz implements FlexibilityProviderBiz {

	private final FlexibilityProviderDao flexibilityProviderDao;

	/**
	 * Constructor.
	 * 
	 * @param flexibilityProviderDao
	 *        the flexibility provider DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoFlexibilityProviderBiz(FlexibilityProviderDao flexibilityProviderDao) {
		super();
		this.flexibilityProviderDao = requireNonNullArgument(flexibilityProviderDao,
				"flexibilityProviderDao");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String register(String token) throws AuthorizationException {
		UserLongCompositePK id = flexibilityProviderDao
				.idForToken(requireNonNullArgument(token, "token"));
		if ( id == null ) {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, token);
		}

		// TODO: the CapacityProvider/Optimizer Configuration RegistrationStatus must be updated

		// generate new token and return that
		return flexibilityProviderDao.createAuthToken(id);
	}

}
