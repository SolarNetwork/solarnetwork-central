/* ==================================================================
 * UserMetadataSecurityAspect.java - 5/04/2024 10:58:35 am
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

package net.solarnetwork.central.aop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityPolicyMetadataType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Security AOP support for {@link UserMetadataBiz}.
 *
 * @author matt
 * @version 1.2
 */
@Aspect
@Component
public class UserMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserMetadataSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut("""
			(execution(* net.solarnetwork.central.biz.UserMetadataBiz.add*(..))
			|| execution(* net.solarnetwork.central.biz.UserMetadataBiz.store*(..))
			|| execution(* net.solarnetwork.central.biz.UserMetadataBiz.remove*(..)))
			&& args(userId,..)
			""")
	public void modifyMetadata(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.biz.UserMetadataBiz.findUserMetadata(..)) && args(filter,..)")
	public void listMetadata(UserMetadataFilter filter) {
	}

	/**
	 * Check access to modify metadata.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Before(value = "modifyMetadata(userId)", argNames = "userId")
	public void modifyMetadataCheck(Long userId) {
		requireUserWriteAccess(userId);
	}

	/**
	 * Check access to find metadata.
	 *
	 * <p>
	 * Read access is required for every user in the filter, and the metadata
	 * of the results is restricted to the metadata paths of the active
	 * security policy.
	 * </p>
	 *
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to check
	 * @return the results
	 * @throws Throwable
	 *         if any error occurs
	 */
	@Around(value = "listMetadata(filter)", argNames = "pjp,filter")
	public Object listMetadataAccessCheck(ProceedingJoinPoint pjp, UserMetadataFilter filter)
			throws Throwable {
		if ( filter == null || !filter.hasUserCriteria() ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long userId : filter.userIds() ) {
			requireUserReadAccess(userId);
		}
		final Object result = pjp.proceed();
		if ( result instanceof FilterResults<?, ?> results ) {
			return restrictMetadataPaths(results);
		}
		return result;
	}

	/**
	 * Restrict the metadata of results to the metadata paths of the active
	 * security policy.
	 *
	 * <p>
	 * The metadata of each result is restricted in place. Results whose
	 * metadata is denied by the policy are removed.
	 * </p>
	 *
	 * @param results
	 *        the results to restrict
	 * @return the restricted results
	 */
	private Object restrictMetadataPaths(FilterResults<?, ?> results) {
		final SecurityPolicy policy = getActiveSecurityPolicy();
		final Set<String> paths = (policy != null ? policy.getUserMetadataPaths() : null);
		if ( paths == null || paths.isEmpty() ) {
			return results;
		}
		final Authentication authentication = SecurityUtils.getCurrentAuthentication();
		final Object principal = (authentication != null ? authentication.getPrincipal() : null);
		final var enforcer = new SecurityPolicyEnforcer(policy, principal, null, getPathMatcher(),
				SecurityPolicyMetadataType.User);
		final List<UserMetadataEntity> restricted = new ArrayList<>(results.getReturnedResultCount());
		for ( Object result : results ) {
			if ( !(result instanceof UserMetadataEntity meta) ) {
				// cannot restrict the metadata of this result, so remove it
				log.warn("Access DENIED to user metadata for {}: cannot restrict {} to policy paths",
						principal, (result != null ? result.getClass().getName() : null));
				continue;
			}
			try {
				meta.setMeta(enforcer.verifyMetadata(meta.getMeta()));
				restricted.add(meta);
			} catch ( AuthorizationException e ) {
				// no metadata allowed by the policy, so remove the result
			}
		}
		return new BasicFilterResults<UserMetadataEntity, Long>(restricted, results.getTotalResults(),
				results.getStartingOffset(), restricted.size());
	}

}
