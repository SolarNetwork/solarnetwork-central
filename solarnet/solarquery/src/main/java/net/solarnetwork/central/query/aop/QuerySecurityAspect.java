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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.NodeDatumFilter;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.ArrayUtils;

/**
 * Security enforcing AOP aspect for {@link QueryBiz}.
 *
 * @author matt
 * @version 2.2
 */
@Aspect
@Component
@Profile(AopServices.WITH_AOP_SECURITY)
public class QuerySecurityAspect extends AuthorizationSupport {

	public static final String FILTER_KEY_NODE_ID = "nodeId";
	public static final String FILTER_KEY_NODE_IDS = "nodeIds";

	private final DatumStreamMetadataDao streamMetadataDao;
	private Set<String> nodeIdNotRequiredSet;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 * @param streamMetadataDao
	 *        the stream metadata DAO
	 */
	public QuerySecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao,
			DatumStreamMetadataDao streamMetadataDao) {
		super(nodeOwnershipDao);
		this.streamMetadataDao = requireNonNullArgument(streamMetadataDao, "streamMetadataDao");
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut(
			value = "execution(* net.solarnetwork.central.query.biz.*.getReportableInterval(..)) && args(nodeId,sourceId,..) && @target(net.solarnetwork.central.domain.Securable)",
			argNames = "nodeId,sourceId")
	public void nodeReportableInterval(Long nodeId, String sourceId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.query.biz.*.getAvailableSources(..)) && args(nodeId,..) && @target(net.solarnetwork.central.domain.Securable)")
	public void nodeReportableSources(Long nodeId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.query.biz.*.getAvailableSources(..)) && args(filter,..) && @target(net.solarnetwork.central.domain.Securable)")
	public void nodesReportableSources(GeneralNodeDatumFilter filter) {
	}

	@Pointcut("execution(* net.solarnetwork.central.query.biz.*.findAvailableSources(..)) && args(filter) && @target(net.solarnetwork.central.domain.Securable)")
	public void nodesAvailableSources(GeneralNodeDatumFilter filter) {
	}

	@Pointcut("execution(* net.solarnetwork.central.query.biz.*.getMostRecentWeatherConditions(..)) && args(nodeId,..) && @target(net.solarnetwork.central.domain.Securable)")
	public void nodeMostRecentWeatherConditions(Long nodeId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.query.biz.*.findFiltered*(..)) && args(filter,..) && @target(net.solarnetwork.central.domain.Securable)")
	public void nodeDatumFilter(Filter filter) {
	}

	@Around(value = "nodeDatumFilter(filter)", argNames = "pjp,filter")
	public Object userNodeFilterAccessCheck(ProceedingJoinPoint pjp, Filter filter) throws Throwable {
		final boolean isQueryBiz = (pjp.getTarget() instanceof QueryBiz);
		final SecurityPolicy policy = getActiveSecurityPolicy();

		if ( policy != null && policy.getSourceIds() != null && !policy.getSourceIds().isEmpty()
				&& filter instanceof GeneralNodeDatumFilter g && g.getSourceId() == null ) {
			// no source IDs provided, but policy restricts source IDs.
			// restrict the filter to the available source IDs if using a DatumFilterCommand,
			// and let call to userNodeAccessCheck later on filter out restricted values
			if ( isQueryBiz && filter instanceof DatumFilterCommand f ) {
				QueryBiz target = (QueryBiz) pjp.getTarget();
				Set<String> availableSources = target.getAvailableSources(f);
				if ( availableSources != null && !availableSources.isEmpty() ) {
					f.setSourceIds(availableSources.toArray(String[]::new));
				}
			}
		}

		Filter f = userNodeAccessCheck(filter);
		if ( f == filter ) {
			return pjp.proceed();
		}

		// if an aggregate was injected (enforced) on the filter, then the join point method
		// might need to change to an aggregate one, e.g. from findFilteredGeneralNodeDatum
		// to findFilteredAggregateGeneralNodeDatum. This _could_ break the calling code if
		// it is expecting a specific result type, but in many cases it is simply returning
		// the result as JSON to some HTTP client and the difference does not matter.
		if ( isQueryBiz && f instanceof AggregateGeneralNodeDatumFilter g && g.getAggregation() != null
				&& pjp.getSignature().getName().equals("findFilteredGeneralNodeDatum") ) {
			// redirect this to findFilteredAggregateGeneralNodeDatum
			QueryBiz target = (QueryBiz) pjp.getTarget();
			Object[] args = pjp.getArgs();
			@SuppressWarnings("unchecked")
			List<SortDescriptor> sorts = (List<SortDescriptor>) args[1];
			return target.findFilteredAggregateGeneralNodeDatum(g, sorts, (Long) args[2],
					(Integer) args[3]);
		}
		Object[] args = pjp.getArgs();
		args[0] = f;
		return pjp.proceed(args);
	}

	/**
	 * Enforce node ID and source ID policy restrictions when requesting the
	 * available sources of nodes.
	 *
	 * <p>
	 * First the node IDs are verified. Then, for all returned source ID values,
	 * if the active policy has no source ID restrictions return all values,
	 * otherwise remove any value not included in the policy.
	 * </p>
	 *
	 * @param pjp
	 *        The join point.
	 * @param filter
	 *        The filter.
	 * @return The set of String source IDs.
	 * @throws Throwable
	 *         if anything goes wrong
	 */
	@Around(value = "nodesReportableSources(filter)", argNames = "pjp,filter")
	public Object reportableSourcesFilterAccessCheck(ProceedingJoinPoint pjp,
			GeneralNodeDatumFilter filter) throws Throwable {
		if ( !ArrayUtils.isOnlyNull(filter.getNodeIds()) ) {
			for ( Long nodeId : filter.getNodeIds() ) {
				requireNodeReadAccess(nodeId);
			}
		}

		// verify source IDs in result
		@SuppressWarnings("unchecked")
		Set<String> result = (Set<String>) pjp.proceed();
		return verifySourceIdSet(result);
	}

	/**
	 * Enforce node ID and source ID policy restrictions when requesting the
	 * available sources of nodes.
	 *
	 * <p>
	 * First the node IDs are verified. Then, for all returned source ID values,
	 * if the active policy has no source ID restrictions return all values,
	 * otherwise remove any value not included in the policy.
	 * </p>
	 *
	 * @param pjp
	 *        The join point.
	 * @param filter
	 *        The filter.
	 * @return The set of String source IDs.
	 * @throws Throwable
	 *         if anything goes wrong
	 * @since 1.6
	 */
	@Around(value = "nodesAvailableSources(filter)", argNames = "pjp,filter")
	public Object availableSourcesFilterAccessCheck(ProceedingJoinPoint pjp,
			GeneralNodeDatumFilter filter) throws Throwable {
		if ( !ArrayUtils.isOnlyNull(filter.getNodeIds()) ) {
			for ( Long nodeId : filter.getNodeIds() ) {
				requireNodeReadAccess(nodeId);
			}
		}

		// verify source IDs in result
		@SuppressWarnings("unchecked")
		Set<NodeSourcePK> result = (Set<NodeSourcePK>) pjp.proceed();
		return verifyNodeSourcePkSet(result);
	}

	/**
	 * Enforce node ID and source ID policy restrictions when requesting the
	 * available sources of a node.
	 *
	 * <p>
	 * First the node ID is verified. Then, for all returned source ID values,
	 * if the active policy has no source ID restrictions return all values,
	 * otherwise remove any value not included in the policy.
	 * </p>
	 *
	 * @param pjp
	 *        The join point.
	 * @param nodeId
	 *        The node ID.
	 * @return The set of String source IDs.
	 * @throws Throwable
	 *         if anything goes wrong
	 */
	@Around(value = "nodeReportableSources(nodeId)", argNames = "pjp,nodeId")
	public Object reportableSourcesAccessCheck(ProceedingJoinPoint pjp, Long nodeId) throws Throwable {
		// verify node ID
		requireNodeReadAccess(nodeId);

		// verify source IDs in result
		@SuppressWarnings("unchecked")
		Set<String> result = (Set<String>) pjp.proceed();
		return verifySourceIdSet(result);
	}

	private Set<String> verifySourceIdSet(Set<String> result) {
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
				getPathMatcher());
		try {
			String[] resultSourceIds = enforcer.verifySourceIds(result.toArray(String[]::new));
			result = new LinkedHashSet<>(Arrays.asList(resultSourceIds));
		} catch ( AuthorizationException e ) {
			// ignore, and just  map to empty set
			result = Collections.emptySet();
		}
		return result;
	}

	private Set<NodeSourcePK> verifyNodeSourcePkSet(Set<NodeSourcePK> result) {
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
				getPathMatcher());

		Map<String, Set<NodeSourcePK>> allSourceIds = new LinkedHashMap<>(result.size());
		for ( NodeSourcePK pk : result ) {
			Set<NodeSourcePK> pkSet = allSourceIds.computeIfAbsent(pk.getSourceId(),
					k -> new LinkedHashSet<>(8));
			pkSet.add(pk);
		}

		try {
			String[] resultSourceIds = enforcer
					.verifySourceIds(allSourceIds.keySet().toArray(String[]::new));
			if ( resultSourceIds.length != allowedSourceIds.size() ) {
				result = new LinkedHashSet<>(resultSourceIds.length);
				for ( String sourceId : resultSourceIds ) {
					Set<NodeSourcePK> pks = allSourceIds.get(sourceId);
					if ( pks != null ) {
						result.addAll(pks);
					}
				}
			}
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
	 * <p>
	 * If the active policy has source ID restrictions, then if no
	 * {@code sourceId} is provided fill in the first available value from the
	 * policy. Otherwise, if {@code sourceId} is provided, check that value is
	 * allowed by the policy.
	 * </p>
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
	@Around(value = "nodeReportableInterval(nodeId, sourceId)", argNames = "pjp,nodeId,sourceId")
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
	@Before(value = "nodeMostRecentWeatherConditions(nodeId)", argNames = "nodeId")
	public void userNodeAccessCheck(Long nodeId) {
		if ( nodeId == null ) {
			return;
		}
		requireNodeReadAccess(nodeId);
	}

	/**
	 * Enforce security policies on a {@link Filter}.
	 *
	 * @param <T>
	 *        the filter type
	 * @param filter
	 *        The filter to verify.
	 * @return A possibly modified filter based on security policies.
	 * @throws AuthorizationException
	 *         if any authorization error occurs
	 */
	public <T extends Filter> T userNodeAccessCheck(T filter) {
		Long[] nodeIds = null;
		boolean nodeIdRequired = true;
		if ( filter instanceof NodeDatumFilter cmd ) {
			nodeIdRequired = isNodeIdRequired(cmd);
			if ( nodeIdRequired ) {
				nodeIds = cmd.getNodeIds();
			}
		} else if ( filter instanceof StreamDatumFilter cmd ) {
			if ( cmd.getStreamIds() != null ) {
				Map<UUID, ObjectDatumStreamMetadataId> ids = streamMetadataDao
						.getDatumStreamMetadataIds(cmd.getStreamIds());
				nodeIds = ids.values().stream().filter(e -> e.getKind() == ObjectDatumKind.Node)
						.map(net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId::getObjectId)
						.toArray(Long[]::new);
			} else if ( cmd.getKind() == ObjectDatumKind.Node && cmd.getObjectIds() != null ) {
				nodeIds = cmd.getObjectIds();
			}
		} else {
			Map<String, ?> f = filter.getFilter();
			if ( f.containsKey(FILTER_KEY_NODE_IDS) ) {
				nodeIds = getLongArrayParameter(f, FILTER_KEY_NODE_IDS);
			} else if ( f.containsKey(FILTER_KEY_NODE_ID) ) {
				nodeIds = getLongArrayParameter(f, FILTER_KEY_NODE_ID);
			} else {
				nodeIdRequired = false;
			}
		}
		if ( !nodeIdRequired ) {
			return filter;
		}
		if ( nodeIds == null || nodeIds.length < 1 || ArrayUtils.isOnlyNull(nodeIds) ) {
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
				: filter.getType().toLowerCase(Locale.ENGLISH));
		return (nodeIdNotRequiredSet == null || !nodeIdNotRequiredSet.contains(type));
	}

	private Long[] getLongArrayParameter(final Map<String, ?> map, final String key) {
		Long[] result = null;
		if ( map.containsKey(key) ) {
			Object o = map.get(key);
			if ( o instanceof Long[] a ) {
				result = a;
			} else if ( o instanceof Long n ) {
				result = new Long[] { n };
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
