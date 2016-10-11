/* ==================================================================
 * SecurityPolicyEnforcer.java - 10/10/2016 8:33:46 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;

/**
 * Support for enforcing a {@link SecurityPolicy} on domain objects.
 * 
 * @author matt
 * @version 1.0
 */
public class SecurityPolicyEnforcer implements InvocationHandler {

	private final Object delegate;
	private final SecurityPolicy policy;
	private final Object principal;

	private Long[] cachedNodeIds;
	private String[] cachedSourceIds;

	private static final Logger LOG = LoggerFactory.getLogger(SecurityPolicyEnforcer.class);

	/**
	 * Construct a new enforcer.
	 * 
	 * @param policy
	 *        The policy to enforce.
	 * @param principal
	 *        The active principal.
	 * @param delegate
	 *        The domain object to enforce the policy on.
	 */
	public SecurityPolicyEnforcer(SecurityPolicy policy, Object principal, Object delegate) {
		super();
		this.delegate = delegate;
		this.policy = policy;
		this.principal = principal;
	}

	/**
	 * Wrap an object with a {@link SecurityPolicyEnforcer}, enforcing policy
	 * properties.
	 * 
	 * This will return a proxy object that implements all interfaces on the
	 * provided enforder's {@code delegate} property.
	 * 
	 * @param enforcer
	 *        The policy enforcer.
	 * @return A new wrapped object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createSecurityPolicyProxy(SecurityPolicyEnforcer enforcer) {
		Class<?>[] interfaces = ClassUtils.getAllInterfaces(enforcer.getDelgate());
		return (T) Proxy.newProxyInstance(enforcer.getDelgate().getClass().getClassLoader(), interfaces,
				enforcer);
	}

	/**
	 * Verify the security policy on all supported properties immediately.
	 * 
	 * @throws AuthorizationException
	 *         if any policy fails
	 */
	public void verify() {
		String[] getters = new String[] { "getNodeIds", "getSourceIds", "getAggregation" };
		for ( String methodName : getters ) {
			try {
				Method m = delegate.getClass().getMethod(methodName);
				invoke(null, m, null);
			} catch ( AuthorizationException e ) {
				throw e;
			} catch ( Throwable e ) {
				// ignore this
			}
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();
		final Object delegateResult = method.invoke(delegate, args);
		if ( "getNodeIds".equals(methodName) || "getNodeId".equals(methodName) ) {
			Long[] nodeIds;
			if ( methodName.endsWith("s") ) {
				nodeIds = (Long[]) delegateResult;
			} else {
				nodeIds = (delegateResult != null ? new Long[] { (Long) delegateResult } : null);
			}
			Long[] result = verifyNodeIds(nodeIds);
			if ( result == null || result.length < 1 || methodName.endsWith("s") ) {
				return result;
			}
			return result[0];
		} else if ( "getSourceIds".equals(methodName) || "getSourceId".equals(methodName) ) {
			String[] sourceIds;
			if ( methodName.endsWith("s") ) {
				sourceIds = (String[]) delegateResult;
			} else {
				sourceIds = (delegateResult != null ? new String[] { (String) delegateResult } : null);
			}
			String[] result = verifySourceIds(sourceIds);
			if ( result == null || result.length < 1 || methodName.endsWith("s") ) {
				return result;
			}
			return result[0];
		} else if ( "getAggregation".equals(methodName) ) {
			Aggregation agg = (Aggregation) delegateResult;
			return verifyAggregation(agg);
		}
		return delegateResult;
	}

	private Long[] verifyNodeIds(Long[] nodeIds) {
		Set<Long> policyNodeIds = policy.getNodeIds();
		// verify source IDs
		if ( policyNodeIds == null || policyNodeIds.isEmpty() ) {
			return nodeIds;
		}
		if ( cachedNodeIds != null ) {
			return (cachedNodeIds.length == 0 ? null : cachedNodeIds);
		}
		if ( nodeIds != null && nodeIds.length > 0 ) {
			// remove any source IDs not in the policy
			Set<Long> nodeIdsSet = new LinkedHashSet<Long>(Arrays.asList(nodeIds));
			for ( Iterator<Long> itr = nodeIdsSet.iterator(); itr.hasNext(); ) {
				Long nodeId = itr.next();
				if ( !policyNodeIds.contains(nodeId) ) {
					LOG.warn("Access DENIED to node {} for {}: policy restriction", nodeId, principal);
					itr.remove();
				}
			}
			if ( nodeIdsSet.size() < 1 ) {
				LOG.warn("Access DENIED to nodes {} for {}", nodeIds, principal);
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeIds);
			} else if ( nodeIdsSet.size() < nodeIds.length ) {
				nodeIds = nodeIdsSet.toArray(new Long[nodeIdsSet.size()]);
			}
		} else if ( nodeIds == null || nodeIds.length < 1 ) {
			// no source IDs provided, set to policy source IDs
			LOG.info("Access RESTRICTED to nodes {} for {}", policyNodeIds, principal);
			nodeIds = policyNodeIds.toArray(new Long[policyNodeIds.size()]);
		}
		cachedNodeIds = (nodeIds == null ? new Long[0] : nodeIds);
		return nodeIds;
	}

	private String[] verifySourceIds(String[] sourceIds) {
		final Set<String> policySourceIds = policy.getSourceIds();
		// verify source IDs
		if ( policySourceIds == null || policySourceIds.isEmpty() ) {
			return sourceIds;
		}
		if ( cachedSourceIds != null ) {
			return (cachedSourceIds.length == 0 ? null : cachedSourceIds);
		}
		if ( sourceIds != null && sourceIds.length > 0 ) {
			// remove any source IDs not in the policy
			Set<String> sourceIdsSet = new LinkedHashSet<String>(Arrays.asList(sourceIds));
			for ( Iterator<String> itr = sourceIdsSet.iterator(); itr.hasNext(); ) {
				String sourceId = itr.next();
				if ( !policySourceIds.contains(sourceId) ) {
					LOG.warn("Access DENIED to source {} for {}: policy restriction", sourceId,
							principal);
					itr.remove();
				}
			}
			if ( sourceIdsSet.size() < 1 ) {
				LOG.warn("Access DENIED to sources {} for {}", sourceIds, principal);
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, sourceIds);
			} else if ( sourceIdsSet.size() < sourceIds.length ) {
				sourceIds = sourceIdsSet.toArray(new String[sourceIdsSet.size()]);
			}
		} else if ( sourceIds == null || sourceIds.length < 1 ) {
			// no source IDs provided, set to policy source IDs
			LOG.info("Access RESTRICTED to sources {} for {}", policySourceIds, principal);
			sourceIds = policySourceIds.toArray(new String[policySourceIds.size()]);
		}
		cachedSourceIds = (sourceIds == null ? new String[0] : sourceIds);
		return sourceIds;
	}

	private Aggregation verifyAggregation(Aggregation agg) {
		final Aggregation min = policy.getMinAggregation();
		if ( min != null ) {
			if ( agg == null || agg.compareLevel(min) < 0 ) {
				LOG.info("Access RESTRICTED from aggregation {} to {} for {}", agg, min, principal);
				return min;
			}
			return agg;
		}
		final Set<Aggregation> allowed = policy.getAggregations();
		if ( allowed == null || allowed.isEmpty() || allowed.contains(agg) ) {
			return agg;
		}
		LOG.warn("Access DENIED to aggregation {} for {}", agg, principal);
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, agg);
	}

	public Object getDelgate() {
		return delegate;
	}

}
