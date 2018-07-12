/* ==================================================================
 * AuditDatumSecurityAspect.java - 12/07/2018 4:20:34 PM
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

package net.solarnetwork.central.datum.aop;

import java.util.Arrays;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security AOP support for {@link AuditDatumBiz}.
 * 
 * @author matt
 * @version 1.0
 */
@Aspect
public class AuditDatumSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 */
	public AuditDatumSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.datum.biz.AuditDatumBiz.findFilteredAuditRecordCounts(..)) && args(filter,..)")
	public void findRecordCounts(GeneralNodeDatumFilter filter) {
	}

	private Long requireCurrentActorHasUserId() {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		if ( actor instanceof SecurityToken ) {
			// require a User token
			String tokenType = ((SecurityToken) actor).getTokenType();
			if ( !UserAuthTokenType.User.toString().equals(tokenType) ) {
				log.warn("Access DENIED for non-user token actor: {}",
						((SecurityToken) actor).getToken());
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
		}
		try {
			// the next method will return the user ID from the User token, or the User actor
			return SecurityUtils.getCurrentActorUserId();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED for actor without user ID");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

	}

	/**
	 * Check access to reading audit datum.
	 * 
	 * <p>
	 * The current actor must have a user ID, and that same user ID must be
	 * specified as the only user ID in the filter.
	 * </p>
	 * 
	 * @param filter
	 *        the filter verify
	 */
	@Before("findRecordCounts(filter)")
	public void findForFilterCheck(GeneralNodeDatumFilter filter) {
		Long userId = requireCurrentActorHasUserId();
		Long[] userIds = filter.getUserIds();
		if ( userIds == null || userIds.length != 1 || !userId.equals(userIds[0]) ) {
			log.warn("Access DENIED for user {} on audit filter without identical user ID: {}", userId,
					Arrays.toString(userIds));
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
	}

}
