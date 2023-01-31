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

package net.solarnetwork.central.security;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Support for enforcing a {@link SecurityPolicy} on domain objects.
 * 
 * @author matt
 * @version 2.2
 * @since 1.12
 */
public class SecurityPolicyEnforcer implements InvocationHandler {

	private final Object delegate;
	private final SecurityPolicy policy;
	private final Object principal;
	private final PathMatcher pathMatcher;
	private final SecurityPolicyMetadataType metadataType;

	private Long[] cachedNodeIds;
	private String[] cachedSourceIds;
	private GeneralDatumMetadata cachedMetadata;

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
		this(policy, principal, delegate, (PathMatcher) null);
	}

	/**
	 * Construct a new enforcer with patch matching support.
	 * 
	 * @param policy
	 *        The policy to enforce.
	 * @param principal
	 *        The active principal.
	 * @param delegate
	 *        The domain object to enforce the policy on.
	 * @param pathMatcher
	 *        The path matcher to use.
	 * @since 1.1
	 */
	public SecurityPolicyEnforcer(SecurityPolicy policy, Object principal, Object delegate,
			PathMatcher pathMatcher) {
		this(policy, principal, delegate, pathMatcher, null);
	}

	/**
	 * Construct a new enforcer with patch matching support.
	 * 
	 * @param policy
	 *        The policy to enforce.
	 * @param principal
	 *        The active principal.
	 * @param delegate
	 *        The domain object to enforce the policy on.
	 * @param pathMatcher
	 *        The path matcher to use.
	 * @param metadataType
	 *        The type of metadata associated with {@code delegate}, or
	 *        {@code null}.
	 * @since 1.2
	 */
	public SecurityPolicyEnforcer(SecurityPolicy policy, Object principal, Object delegate,
			PathMatcher pathMatcher, SecurityPolicyMetadataType metadataType) {
		super();
		this.delegate = delegate;
		this.policy = policy;
		this.principal = principal;
		this.pathMatcher = pathMatcher;
		this.metadataType = metadataType;
	}

	/**
	 * Wrap an object with a {@link SecurityPolicyEnforcer}, enforcing policy
	 * properties.
	 * 
	 * This will return a proxy object that implements all interfaces on the
	 * provided enforder's {@code delegate} property.
	 * 
	 * @param <T>
	 *        the return object type
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
		String[] getters = new String[] { "getNodeIds", "getSourceIds", "getAggregation",
				"getMetadata" };
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
			String[] result = verifySourceIds(sourceIds, true);
			if ( result == null || result.length < 1 || methodName.endsWith("s") ) {
				return result;
			}
			return result[0];
		} else if ( "getAggregation".equals(methodName) ) {
			Aggregation agg = (Aggregation) delegateResult;
			return verifyAggregation(agg);
		} else if ( "getMetadata".equals(methodName) ) {
			GeneralDatumMetadata meta = (GeneralDatumMetadata) delegateResult;
			return verifyMetadata(meta, true);
		}
		return delegateResult;
	}

	/**
	 * Verify an arbitrary list of node IDs against the configured policy.
	 * 
	 * @param nodeIds
	 *        The node IDs to verify.
	 * @return The allowed node IDs.
	 * @throws AuthorizationException
	 *         if no node IDs are allowed
	 */
	public Long[] verifyNodeIds(Long[] nodeIds) {
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

	private boolean matchesPattern(Set<String> patterns, String value) {
		for ( String pattern : patterns ) {
			if ( pathMatcher.match(pattern, value) ) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesPatternStart(Set<String> patterns, String value) {
		for ( String pattern : patterns ) {
			if ( pathMatcher.matchStart(pattern, value) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verify an arbitrary list of source IDs against the configured policy.
	 * 
	 * @param sourceIds
	 *        The source IDs to verify.
	 * @return The allowed source IDs.
	 * @throws AuthorizationException
	 *         if no source IDs are allowed
	 */
	public String[] verifySourceIds(String[] sourceIds) {
		return verifySourceIds(sourceIds, false);
	}

	private String[] verifySourceIds(String[] sourceIds, final boolean cacheResults) {
		Set<String> policySourceIds = policy.getSourceIds();

		// verify source IDs
		if ( policySourceIds == null || policySourceIds.isEmpty() ) {
			return sourceIds;
		}
		if ( cacheResults && cachedSourceIds != null ) {
			return (cachedSourceIds.length == 0 ? null : cachedSourceIds);
		}

		if ( sourceIds != null && sourceIds.length > 0 ) {
			Set<String> sourceIdsSet = new LinkedHashSet<>(Arrays.asList(sourceIds));

			// extract policy source ID patterns
			Set<String> policySourceIdPatterns = null;

			// extract input source ID patterns
			Set<String> sourceIdPatterns = null;

			if ( pathMatcher != null ) {
				for ( String policySourceId : policySourceIds ) {
					if ( pathMatcher.isPattern(policySourceId) ) {
						if ( policySourceIdPatterns == null ) {
							policySourceIdPatterns = new LinkedHashSet<>(policySourceIds.size());
						}
						policySourceIdPatterns.add(policySourceId);
					}
				}
				if ( policySourceIdPatterns != null ) {
					Set<String> mutablePolicySourceIds = new LinkedHashSet<>(policySourceIds);
					mutablePolicySourceIds.removeAll(policySourceIdPatterns);
					policySourceIds = mutablePolicySourceIds;
				}

				for ( String sourceId : sourceIds ) {
					if ( pathMatcher.isPattern(sourceId) ) {
						if ( sourceIdPatterns == null ) {
							sourceIdPatterns = new LinkedHashSet<>(sourceIds.length);
						}
						sourceIdPatterns.add(sourceId);
						sourceIdsSet.remove(sourceId);
					}
				}
			}

			// remove any source IDs not in the policy (or not matching a pattern)
			for ( Iterator<String> itr = sourceIdsSet.iterator(); itr.hasNext(); ) {
				final String sourceId = itr.next();
				if ( policySourceIds.contains(sourceId) ) {
					continue;
				}
				if ( policySourceIdPatterns != null
						&& matchesPattern(policySourceIdPatterns, sourceId) ) {
					continue;
				}
				LOG.warn("Access DENIED to source {} for {}: policy restriction", sourceId, principal);
				itr.remove();
			}

			// resolve source ID patterns against policy
			if ( sourceIdPatterns != null ) {
				// if a source ID pattern exactly matches a policy source ID pattern, allow
				if ( policySourceIdPatterns != null ) {
					for ( String sourceIdPattern : sourceIdPatterns ) {
						if ( policySourceIdPatterns.contains(sourceIdPattern) ) {
							sourceIdsSet.add(sourceIdPattern);
							continue;
						}
						LOG.warn("Access DENIED to source {} for {}: policy restriction",
								sourceIdPattern, principal);
					}
				}
				// if a source ID pattern matches a policy source ID, fill in that policy ID
				for ( String policySourceId : policySourceIds ) {
					if ( matchesPattern(sourceIdPatterns, policySourceId) ) {
						sourceIdsSet.add(policySourceId);
					}
				}
			}

			if ( sourceIdsSet.isEmpty() ) {
				LOG.warn("Access DENIED to sources {} for {}", sourceIds, principal);
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, sourceIds);
			} else if ( !sourceIdsSet.equals(new HashSet<>(Arrays.asList(sourceIds))) ) {
				sourceIds = sourceIdsSet.toArray(new String[sourceIdsSet.size()]);
			}
		} else if ( sourceIds == null || sourceIds.length < 1 ) {
			// no source IDs provided, set to policy source IDs
			LOG.info("Access RESTRICTED to sources {} for {}", policySourceIds, principal);
			sourceIds = policySourceIds.toArray(new String[policySourceIds.size()]);
		}
		if ( cacheResults ) {
			cachedSourceIds = (sourceIds == null ? new String[0] : sourceIds);
		}
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

	/**
	 * Verify an arbitrary metadata instance against the configured policy.
	 * 
	 * @param metadata
	 *        The metadata to verify.
	 * @return The allowed metadata.
	 * @throws AuthorizationException
	 *         if no metadata access is allowed
	 */
	public GeneralDatumMetadata verifyMetadata(GeneralDatumMetadata metadata) {
		return verifyMetadata(metadata, false);
	}

	private GeneralDatumMetadata verifyMetadata(final GeneralDatumMetadata meta,
			final boolean cacheResults) {
		final Set<String> policyMetadataPaths;
		switch (metadataType) {
			case Node:
				policyMetadataPaths = policy.getNodeMetadataPaths();
				break;

			case User:
				policyMetadataPaths = policy.getUserMetadataPaths();
				break;

			default:
				policyMetadataPaths = Collections.emptySet();
				break;
		}

		// verify metadata
		if ( meta == null || policyMetadataPaths == null || policyMetadataPaths.isEmpty() ) {
			return meta;
		}
		if ( cacheResults && cachedMetadata != null ) {
			return cachedMetadata;
		}

		Map<String, Object> infoMap = null;
		if ( meta.getInfo() != null ) {
			infoMap = enforceMetadataPaths(policyMetadataPaths, meta.getInfo(), "/m");
		}

		Map<String, Object> propMap = null;
		if ( meta.getPropertyInfo() != null ) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Map<String, Object> pm = (Map) meta.getPropertyInfo();
			propMap = enforceMetadataPaths(policyMetadataPaths, pm, "/pm");
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, Map<String, Object>> pm = (Map) propMap;
		GeneralDatumMetadata result = new GeneralDatumMetadata(infoMap, pm);
		result.setTags(meta.getTags());
		if ( result.equals(meta) ) {
			return meta;
		}

		if ( infoMap == null && propMap == null ) {
			// no metadata matches any path, so throw exception
			LOG.warn("Access DENIED to metadata {} on {} for {}", meta, delegate, principal);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, meta);
		}

		if ( cacheResults ) {
			cachedMetadata = result;
		}
		return result;
	}

	private Map<String, Object> enforceMetadataPaths(Set<String> policyPaths, Map<String, Object> meta,
			String path) {
		if ( meta == null ) {
			return null;
		}
		Map<String, Object> result = null;
		for ( Map.Entry<String, Object> me : meta.entrySet() ) {
			String entryPath = path + "/" + me.getKey();
			Object val = me.getValue();
			if ( val instanceof Map ) {
				// object node; try to remove entire trees from checking if the path start doesn't match
				if ( !matchesPatternStart(policyPaths, entryPath) ) {
					continue;
				}
				// descend into map path for verification
				@SuppressWarnings("unchecked")
				Map<String, Object> mapVal = (Map<String, Object>) val;
				mapVal = enforceMetadataPaths(policyPaths, mapVal, entryPath);
				if ( mapVal != null ) {
					if ( result == null ) {
						result = new LinkedHashMap<String, Object>(meta.size());
					}
					result.put(me.getKey(), mapVal);
				}
			} else {
				// leaf node
				if ( matchesPattern(policyPaths, entryPath) ) {
					if ( result == null ) {
						result = new LinkedHashMap<String, Object>(meta.size());
					}
					result.put(me.getKey(), val);
				}
			}
		}
		if ( result == null || result.isEmpty() ) {
			return null;
		}
		return result;
	}

	public Object getDelgate() {
		return delegate;
	}

}
