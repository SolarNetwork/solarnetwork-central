/* ==================================================================
 * QuerySecurityAspect.java - Dec 18, 2012 4:32:34 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.aop;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.NodeDatumFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.support.AuthorizationSupport;

/**
 * Security enforcing AOP aspect for {@link QueryBiz}.
 * 
 * @author matt
 * @version 1.5
 */
@Aspect
public class QuerySecurityAspect extends AuthorizationSupport {

	public static final String FILTER_KEY_NODE_ID = "nodeId";
	public static final String FILTER_KEY_NODE_IDS = "nodeIds";

	private Set<String> nodeIdNotRequiredSet;

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao
	 */
	public QuerySecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setSourceIdPathMatcher(antMatch);
		setMetadataPathMatcher(antMatch);
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getReportableInterval(..)) && args(nodeId,sourceId,..)")
	public void nodeReportableInterval(Long nodeId, String sourceId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getAvailableSources(..)) && args(nodeId,..)")
	public void nodeReportableSources(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.getMostRecentWeatherConditions(..)) && args(nodeId,..)")
	public void nodeMostRecentWeatherConditions(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* net.solarnetwork.central.query.biz.*.findFiltered*(..)) && args(filter,..)")
	public void nodeDatumFilter(Filter filter) {
	}

	@Around(value = "nodeDatumFilter(filter)")
	public Object userNodeFilterAccessCheck(ProceedingJoinPoint pjp, Filter filter) throws Throwable {
		Filter f = userNodeAccessCheck(filter);
		if ( f == filter ) {
			return pjp.proceed();
		}

		// if an aggregate was injected (enforced) on the filter, then the join point method
		// might need to change to an aggregate one, e.g. from findFilteredGeneralNodeDatum
		// to findFilteredAggregateGeneralNodeDatum. This _could_ break the calling code if
		// it is expecting a specific result type, but in many cases it is simply returning
		// the result as JSON to some HTTP client and the difference does not matter.
		if ( pjp.getTarget() instanceof QueryBiz && f instanceof AggregateGeneralNodeDatumFilter
				&& ((AggregateGeneralNodeDatumFilter) f).getAggregation() != null
				&& pjp.getSignature().getName().equals("findFilteredGeneralNodeDatum") ) {
			// redirect this to findFilteredAggregateGeneralNodeDatum
			QueryBiz target = (QueryBiz) pjp.getTarget();
			Object[] args = pjp.getArgs();
			@SuppressWarnings("unchecked")
			List<SortDescriptor> sorts = (List<SortDescriptor>) args[1];
			return target.findFilteredAggregateGeneralNodeDatum((AggregateGeneralNodeDatumFilter) f,
					sorts, (Integer) args[2], (Integer) args[3]);
		}
		Object[] args = pjp.getArgs();
		args[0] = f;
		return pjp.proceed(args);
	}

	/**
	 * Enforce node ID and source ID policy restrictions when requesting the
	 * available sources of a node.
	 * 
	 * First the node ID is verified. Then, for all returned source ID values,
	 * if the active policy has no source ID restrictions return all values,
	 * otherwise remove any value not included in the policy.
	 * 
	 * @param pjp
	 *        The join point.
	 * @param nodeId
	 *        The node ID.
	 * @return The set of String source IDs.
	 * @throws Throwable
	 */
	@Around("nodeReportableSources(nodeId)")
	public Object reportableSourcesAccessCheck(ProceedingJoinPoint pjp, Long nodeId) throws Throwable {
		// verify node ID
		requireNodeReadAccess(nodeId);

		// verify source IDs in result
		@SuppressWarnings("unchecked")
		Set<String> result = (Set<String>) pjp.proceed();
		if ( result == null || result.isEmpty() ) {
			return result;
		}
		SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null ) {
			return result;
		}
		Set<String> allowedSourceIds = policy.getSourceIds();
		if ( allowedSourceIds == null || allowedSourceIds.isEmpty() ) {
			return result;
		}
		Authentication authentication = SecurityUtils.getCurrentAuthentication();
		Object principal = (authentication != null ? authentication.getPrincipal() : null);
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, principal, null,
				getSourceIdPathMatcher());
		try {
			String[] resultSourceIds = enforcer
					.verifySourceIds(result.toArray(new String[result.size()]));
			result = new LinkedHashSet<String>(Arrays.asList(resultSourceIds));
		} catch ( AuthorizationException e ) {
			// ignore, and just  map to empty set
			result = Collections.emptySet();
		}
		return result;
	}

	/**
	 * Enforce node ID and source ID policy restrictions when requesting a
	 * reportable interval.
	 * 
	 * If the active policy has source ID restrictions, then if no
	 * {@code sourceId} is provided fill in the first available value from the
	 * policy. Otherwise, if {@code sourceId} is provided, check that value is
	 * allowed by the policy.
	 * 
	 * @param pjp
	 *        The join point.
	 * @param nodeId
	 *        The node ID.
	 * @param sourceId
	 *        The source ID, or {@code null}.
	 * @return The reportable interval.
	 * @throws Throwable
	 *         If any error occurs.
	 */
	@Around("nodeReportableInterval(nodeId, sourceId)")
	public Object reportableIntervalAccessCheck(ProceedingJoinPoint pjp, Long nodeId, String sourceId)
			throws Throwable {
		// verify node ID
		requireNodeReadAccess(nodeId);

		// now verify source ID
		SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null ) {
			return pjp.proceed();
		}

		Set<String> allowedSourceIds = policy.getSourceIds();
		if ( allowedSourceIds != null && !allowedSourceIds.isEmpty() ) {
			Authentication authentication = SecurityUtils.getCurrentAuthentication();
			Object principal = (authentication != null ? authentication.getPrincipal() : null);
			if ( sourceId == null ) {
				// force the first allowed source ID
				sourceId = allowedSourceIds.iterator().next();
				log.info("Access RESTRICTED to source {} for {}", sourceId, principal);
				Object[] args = pjp.getArgs();
				args[1] = sourceId;
				return pjp.proceed(args);
			} else if ( !allowedSourceIds.contains(sourceId) ) {
				log.warn("Access DENIED to source {} for {}", sourceId, principal);
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, sourceId);
			}
		}

		return pjp.proceed();
	}

	/**
	 * Allow the current user (or current node) access to node data.
	 * 
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before("nodeMostRecentWeatherConditions(nodeId)")
	public void userNodeAccessCheck(Long nodeId) {
		if ( nodeId == null ) {
			return;
		}
		requireNodeReadAccess(nodeId);
	}

	/**
	 * Enforce security policies on a {@link Filter}.
	 * 
	 * @param filter
	 *        The filter to verify.
	 * @return A possibly modified filter based on security policies.
	 * @throws AuthorizationException
	 *         if any authorization error occurs
	 */
	public <T extends Filter> T userNodeAccessCheck(T filter) {
		Long[] nodeIds = null;
		boolean nodeIdRequired = true;
		if ( filter instanceof NodeDatumFilter ) {
			NodeDatumFilter cmd = (NodeDatumFilter) filter;
			nodeIdRequired = isNodeIdRequired(cmd);
			if ( nodeIdRequired ) {
				nodeIds = cmd.getNodeIds();
			}
		} else {
			nodeIdRequired = false;
			Map<String, ?> f = filter.getFilter();
			if ( f.containsKey(FILTER_KEY_NODE_IDS) ) {
				nodeIds = getLongArrayParameter(f, FILTER_KEY_NODE_IDS);
			} else if ( f.containsKey(FILTER_KEY_NODE_ID) ) {
				nodeIds = getLongArrayParameter(f, FILTER_KEY_NODE_ID);
			}
		}
		if ( !nodeIdRequired ) {
			return filter;
		}
		if ( nodeIds == null || nodeIds.length < 1 ) {
			log.warn("Access DENIED; no node ID provided");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long nodeId : nodeIds ) {
			userNodeAccessCheck(nodeId);
		}

		return policyEnforcerCheck(filter);
	}

	/**
	 * Check if a node ID is required of a filter instance. This will return
	 * <em>true</em> unless the {@link #getNodeIdNotRequiredSet()} set contains
	 * the value returned by {@link DatumFilter#getType()}.
	 * 
	 * @param filter
	 *        the filter
	 * @return <em>true</em> if a node ID is required for the given filter
	 */
	private boolean isNodeIdRequired(DatumFilter filter) {
		final String type = (filter == null || filter.getType() == null ? null
				: filter.getType().toLowerCase());
		return (nodeIdNotRequiredSet == null || !nodeIdNotRequiredSet.contains(type));
	}

	private Long[] getLongArrayParameter(final Map<String, ?> map, final String key) {
		Long[] result = null;
		if ( map.containsKey(key) ) {
			Object o = map.get(key);
			if ( o instanceof Long[] ) {
				result = (Long[]) o;
			} else if ( o instanceof Long ) {
				result = new Long[] { (Long) o };
			}
		}
		return result;
	}

	public Set<String> getNodeIdNotRequiredSet() {
		return nodeIdNotRequiredSet;
	}

	public void setNodeIdNotRequiredSet(Set<String> nodeIdNotRequiredSet) {
		this.nodeIdNotRequiredSet = nodeIdNotRequiredSet;
	}

}
