/* ==================================================================
 * UserExpireSecurityAspect.java - 9/07/2018 11:01:02 AM
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

package net.solarnetwork.central.user.expire.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;

/**
 * Security enforcing AOP aspect for {@link UserExpireBiz}
 *
 * @author matt
 * @version 2.1
 */
@Aspect
@Component
public class UserExpireSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 */
	public UserExpireSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
	}

	@Pointcut("execution(* net.solarnetwork.central.user.expire.biz.*Biz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.expire.biz.UserExpireBiz.save*(..)) && args(config,..)")
	public void saveConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.expire.biz.UserExpireBiz.delete*(..)) && args(config,..)")
	public void deleteConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.expire.biz.UserExpireBiz.*ForConfiguration(..)) && args(config,..)")
	public void actionForConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("execution(* net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz.*(..)) && args(filter,..)")
	public void actionForDatumFilter(GeneralNodeDatumFilter filter) {

	}

	@Pointcut("execution(* net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz.deleteDatum(..)) && args(userId,..)")
	public void deleteDatum(Long userId) {
	}

	@Before(value = "actionForUser(userId)", argNames = "userId")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before(value = "deleteDatum(userId)", argNames = "userId")
	public void writeActionForUserCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	@Before(value = "saveConfiguration(config) || deleteConfiguration(config) || actionForConfiguration(config)",
			argNames = "config")
	public void saveConfigurationCheck(UserRelatedEntity<?> config) {
		final Long userId = (config != null ? config.getUserId() : null);
		requireUserWriteAccess(userId);
	}

	@Before(value = "actionForDatumFilter(filter)", argNames = "filter")
	public void datumFilterCheck(GeneralNodeDatumFilter filter) {
		final Long userId = (filter != null ? filter.getUserId() : null);
		requireUserWriteAccess(userId);

		final Long[] nodeIds = (filter != null ? filter.getNodeIds() : null);
		if ( nodeIds != null ) {
			for ( Long nodeId : nodeIds ) {
				requireNodeWriteAccess(nodeId);
			}
		}
	}

}
