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

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
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
	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;

	/**
	 * Constructor.
	 * 
	 * @param flexibilityProviderDao
	 *        the flexibility provider DAO
	 * @param capacityProviderDao
	 *        the capacity provider configuration DAO
	 * @param capacityOptimizerDao
	 *        the capacity optimizer configuration DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoFlexibilityProviderBiz(FlexibilityProviderDao flexibilityProviderDao,
			CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao) {
		super();
		this.flexibilityProviderDao = requireNonNullArgument(flexibilityProviderDao,
				"flexibilityProviderDao");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String register(AuthRoleInfo authInfo, String externalSystemToken)
			throws AuthorizationException {
		if ( authInfo == null || authInfo.id() == null || authInfo.role() == null
				|| externalSystemToken == null ) {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, null);
		}

		OscpRole systemRole = authInfo.role();
		if ( systemRole == OscpRole.FlexibilityProvider ) {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, null);
		}

		// the flex provider ID for the CP/CO
		Long flexibilityProviderId = null;

		BasicConfigurationFilter filter = BasicConfigurationFilter
				.filterForUsers(authInfo.id().getUserId());
		filter.setConfigurationId(authInfo.id().getEntityId());

		if ( systemRole == OscpRole.CapacityProvider ) {
			var cpResults = capacityProviderDao.findFiltered(filter);
			if ( cpResults.getReturnedResultCount() > 0 ) {
				CapacityProviderConfiguration cp = stream(cpResults.spliterator(), false).findFirst()
						.orElse(null);
				if ( cp.getRegistrationStatus() != RegistrationStatus.Registered ) {
					cp.setRegistrationStatus(RegistrationStatus.Registered);
					capacityProviderDao.save(cp);
				}
				// TODO UserEvent
				capacityProviderDao.saveExternalSystemAuthToken(cp.getId(), externalSystemToken);
				flexibilityProviderId = cp.getFlexibilityProviderId();
			} else {
				// TODO UserEvent (in exception handler method)
				throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, authInfo);
			}
		} else if ( systemRole == OscpRole.CapacityOptimizer ) {
			var coResults = capacityOptimizerDao.findFiltered(filter);
			if ( coResults.getReturnedResultCount() > 0 ) {
				CapacityOptimizerConfiguration co = stream(coResults.spliterator(), false).findFirst()
						.orElse(null);
				if ( co.getRegistrationStatus() != RegistrationStatus.Registered ) {
					co.setRegistrationStatus(RegistrationStatus.Registered);
					capacityOptimizerDao.save(co);
				}
				// TODO UserEvent
				capacityOptimizerDao.saveExternalSystemAuthToken(co.getId(), externalSystemToken);
				flexibilityProviderId = co.getFlexibilityProviderId();
			} else {
				// TODO UserEvent (in exception handler method)
				throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, authInfo);
			}
		} else {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, null);
		}

		// generate new FP token for the system to use, and return it
		var fpId = new UserLongCompositePK(authInfo.userId(), flexibilityProviderId);
		return flexibilityProviderDao.createAuthToken(fpId);
	}

}
