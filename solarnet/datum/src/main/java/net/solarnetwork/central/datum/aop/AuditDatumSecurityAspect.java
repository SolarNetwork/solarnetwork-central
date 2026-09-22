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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.BasicSecurityException;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;

/**
 * Security AOP support for {@link AuditDatumBiz}.
 *
 * @author matt
 * @version 2.2
 */
@Aspect
@Component
public class AuditDatumSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param noeOwnershipDao
	 *        the ownership DAO to use
	 */
	public AuditDatumSecurityAspect(SolarNodeOwnershipDao noeOwnershipDao) {
		super(noeOwnershipDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut("execution(* net.solarnetwork.central.datum.biz.AuditDatumBiz.find*AuditDatumFiltered(..)) && args(filter,..)")
	public void findAuditDatum(AuditDatumCriteria filter) {
	}

	private Long requireCurrentActorHasUserId() {
		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( BasicSecurityException e ) {
			log.warn("Access DENIED for non-authenticated actor");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		if ( actor instanceof SecurityToken token ) {
			// require a User token
			SecurityTokenType tokenType = token.getTokenType();
			if ( !SecurityTokenType.User.equals(tokenType) ) {
				log.warn("Access DENIED for non-user token actor: {}", token.getToken());
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
		}
		try {
			// the next method will return the user ID from the User token, or the User actor
			return SecurityUtils.getCurrentActorUserId();
		} catch ( BasicSecurityException e ) {
			log.warn("Access DENIED for actor without user ID");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

	}

	private void requireUserId(Long userId, Long @Nullable [] userIds) {
		if ( userIds == null || userIds.length != 1 || !userId.equals(userIds[0]) ) {
			log.warn("Access DENIED for user {} on audit filter without identical user ID: {}", userId,
					Arrays.toString(userIds));
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
	}

	/**
	 * Check access to reading audit datum.
	 *
	 * <p>
	 * The current actor must have a user ID, and that same user ID must be
	 * specified as the only user ID in the filter. The filter is then
	 * restricted to the nodes and sources of the active security policy.
	 * </p>
	 *
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter verify
	 * @return the results
	 * @throws Throwable
	 *         if any error occurs
	 * @since 2.2
	 */
	@SuppressWarnings("ReferenceEquality")
	@Around(value = "findAuditDatum(filter)", argNames = "pjp,filter")
	public Object findAuditDatumForFilterAccessCheck(ProceedingJoinPoint pjp,
			AuditDatumCriteria filter) throws Throwable {
		final AuditDatumCriteria f = findAuditDatumForFilterCheck(filter);
		if ( f == filter ) {
			return pjp.proceed();
		}
		final @Nullable Object[] args = pjp.getArgs();
		args[0] = f;
		return pjp.proceed(args);
	}

	/**
	 * Check access to reading audit datum.
	 *
	 * @param filter
	 *        the filter verify
	 * @return the filter to use, restricted to the nodes and sources of the
	 *         active security policy
	 */
	public AuditDatumCriteria findAuditDatumForFilterCheck(AuditDatumCriteria filter) {
		Long userId = requireCurrentActorHasUserId();
		Long[] userIds = filter.getUserIds();
		requireUserId(userId, userIds);
		return nonnull(policyEnforcerCheck(filter), "Restricted filter");
	}

}
