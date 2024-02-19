/* ==================================================================
 * OcppAuthorizationService.java - 19/02/2024 6:48:37 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.service;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.ocpp.domain.Authorization;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.service.support.BasicIdentifiable;

/**
 * Basic implementation of {@link AuthorizationService}.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppAuthorizationService extends BasicIdentifiable implements AuthorizationService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final CentralAuthorizationDao authorizationDao;
	private final CentralChargePointDao chargePointDao;

	/**
	 * Constructor.
	 * 
	 * @param authorizationDao
	 *        the {@link Authorization} DAO to use
	 * @param chargePointDao
	 *        the {@link ChargePoint} DAO to use
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public OcppAuthorizationService(CentralAuthorizationDao authorizationDao,
			CentralChargePointDao chargePointDao) {
		super();
		this.authorizationDao = requireNonNullArgument(authorizationDao, "authorizationDao");
		this.chargePointDao = requireNonNullArgument(chargePointDao, "chargePointDao");
	}

	@Override
	public AuthorizationInfo authorize(final ChargePointIdentity identity, final String idTag) {
		Authorization auth = null;
		if ( identity != null && idTag != null ) {
			CentralChargePoint cp = (CentralChargePoint) chargePointDao.getForIdentity(identity);
			if ( cp != null ) {
				auth = authorizationDao.getForToken(cp.getUserId(), idTag);
			}
		}
		AuthorizationInfo.Builder result = AuthorizationInfo.builder().withId(idTag);
		if ( auth != null ) {
			result.withExpiryDate(auth.getExpiryDate()).withParentId(auth.getParentId());
			if ( !auth.isEnabled() ) {
				result.withStatus(AuthorizationStatus.Blocked);
			} else if ( auth.isExpired() ) {
				result.withStatus(AuthorizationStatus.Expired);
			} else {
				result.withStatus(AuthorizationStatus.Accepted);
			}
		} else {
			log.info("Invliad IdTag received from charge point {}: [{}]", identity.getIdentifier(),
					idTag);
			result.withStatus(AuthorizationStatus.Invalid);
		}
		return result.build();
	}

}
