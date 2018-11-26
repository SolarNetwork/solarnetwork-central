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
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserRelatedEntity;
import net.solarnetwork.central.user.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link UserExpireBiz}
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class UserExpireSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public UserExpireSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.expire.biz.*Biz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.expire.biz.UserExpireBiz.save*(..)) && args(config,..)")
	public void saveConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.expire.biz.UserExpireBiz.delete*(..)) && args(config,..)")
	public void deleteConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.expire.biz.UserExpireBiz.*ForConfiguration(..)) && args(config,..)")
	public void actionForConfiguration(UserRelatedEntity<?> config) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz.*(..)) && args(filter,..)")
	public void actionForDatumFilter(GeneralNodeDatumFilter filter) {

	}

	@Before("actionForUser(userId)")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@Before("saveConfiguration(config) || deleteConfiguration(config) || actionForConfiguration(config)")
	public void saveConfigurationCheck(UserRelatedEntity<?> config) {
		final Long userId = (config != null ? config.getUserId() : null);
		requireUserWriteAccess(userId);
	}

	@Before("actionForDatumFilter(filter)")
	public void datumFilterCheck(GeneralNodeDatumFilter filter) {
		final Long userId = (filter != null ? filter.getUserId() : null);
		requireUserWriteAccess(userId);

		final Long[] nodeIds = filter.getNodeIds();
		if ( nodeIds != null ) {
			for ( Long nodeId : nodeIds ) {
				requireNodeWriteAccess(nodeId);
			}
		}
	}

}
