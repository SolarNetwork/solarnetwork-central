/* ==================================================================
 * UserExportSecurityAspect.java - 28/03/2018 4:14:55 PM
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

package net.solarnetwork.central.user.export.aop;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserRelatedEntity;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link UserExportBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class UserExportSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public UserExportSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.export.biz.UserExportBiz.*ForUser(..)) && args(userId,..)")
	public void actionForUser(Long userId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.export.biz.UserExportBiz.datumExportConfiguration(..))")
	public void readDatumExportConfiguration() {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.user.export.biz.UserExportBiz.save*(..)) && args(config,..)")
	public void saveConfiguration(UserRelatedEntity<?> config) {
	}

	@Before("actionForUser(userId)")
	public void actionForUserCheck(Long userId) {
		requireUserReadAccess(userId);
	}

	@AfterReturning(pointcut = "readDatumExportConfiguration()", returning = "result")
	public void readDatumExportConfigurationCheck(UserDatumExportConfiguration result) {
		final Long userId = (result != null ? result.getUserId() : null);
		if ( userId == null ) {
			log.warn("Access DENIED to UserDatumExportConfiguration {}; userId not provided", result);
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, userId);
		}
		requireUserReadAccess(result.getUserId());
	}

	@Before("saveConfiguration(config)")
	public void saveConfigurationCheck(UserRelatedEntity<?> config) {
		final Long userId = (config != null ? config.getUserId() : null);
		requireUserWriteAccess(userId);
	}

}
