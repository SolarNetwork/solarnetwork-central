/* ==================================================================
 * NodeMetadataSecurityAspect.java - 16/12/2025 7:03:41 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityPolicyMetadataType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Security AOP support for {@link SolarNodeMetadataBiz}.
 * 
 * @author matt
 * @version 1.2
 */
@Aspect
@Component
public class NodeMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 */
	public NodeMetadataSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao) {
		super(nodeOwnershipDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	/**
	 * Match add/store/remove metadata methods.
	 * 
	 * @param nodeId
	 *        the node ID to modify metadata on
	 */
	@Pointcut("execution(* net.solarnetwork.central.biz.SolarNodeMetadataBiz.*(..)) && args(nodeId,..)")
	public void modifyNodeMetadata(Long nodeId) {
	}

	/**
	 * Match add/store/remove metadata methods.
	 * 
	 * @param filter
	 *        the search criteria
	 */
	@Pointcut("execution(* net.solarnetwork.central.biz.SolarNodeMetadataBiz.find*(..)) && args(filter,..)")
	public void findNodeMetadata(SolarNodeMetadataFilter filter) {
	}

	/**
	 * Check access to modify node metadata.
	 *
	 * @param nodeId
	 *        the node ID
	 */
	@Before(value = "modifyNodeMetadata(nodeId)", argNames = "nodeId")
	public void modifyNodeMetadataAccessCheck(Long nodeId) {
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Check access to find node metadata.
	 *
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to check
	 * @return the results
	 * @throws Throwable
	 *         if any error occurs
	 */
	@SuppressWarnings("ReferenceEquality")
	@Around(value = "findNodeMetadata(filter)")
	public FilterResults<SolarNodeMetadata, Long> findNodeMetadataAccessCheck(ProceedingJoinPoint pjp,
			SolarNodeMetadataFilter filter) throws Throwable {
		if ( filter == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		final @Nullable Object[] args = pjp.getArgs();

		SolarNodeMetadataFilter f = policyEnforcerCheck(filter);
		if ( f != filter ) {
			args[0] = f;
		}

		if ( filter.getNodeId() == null ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		@SuppressWarnings("unchecked")
		var result = (FilterResults<SolarNodeMetadata, Long>) pjp.proceed(args);
		return restrictMetadataPaths(result);
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
	private FilterResults<SolarNodeMetadata, Long> restrictMetadataPaths(
			FilterResults<SolarNodeMetadata, Long> results) {
		final SecurityPolicy policy = getActiveSecurityPolicy();
		final Set<String> paths = (policy != null ? policy.getNodeMetadataPaths() : null);
		if ( paths == null || paths.isEmpty() ) {
			return results;
		}
		final Authentication authentication = SecurityUtils.getCurrentAuthentication();
		final Object principal = (authentication != null ? authentication.getPrincipal() : null);
		final var enforcer = new SecurityPolicyEnforcer(policy, principal, null, getPathMatcher(),
				SecurityPolicyMetadataType.Node);
		final List<SolarNodeMetadata> restricted = new ArrayList<>(results.getReturnedResultCount());
		for ( SolarNodeMetadata meta : results ) {
			try {
				meta.setMeta(enforcer.verifyMetadata(meta.getMeta()));
				restricted.add(meta);
			} catch ( AuthorizationException e ) {
				// no metadata allowed by the policy, so remove the result
			}
		}
		return new BasicFilterResults<>(restricted, results.getTotalResults(),
				results.getStartingOffset(), restricted.size());
	}

}
