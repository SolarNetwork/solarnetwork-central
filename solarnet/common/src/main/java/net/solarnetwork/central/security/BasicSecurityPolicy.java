/* ==================================================================
 * BasicSecurityPolicy.java - 9/10/2016 8:01:18 AM
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

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.solarnetwork.central.domain.LocationPrecision;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Basic implementation of {@link SecurityPolicy}.
 * 
 * @author matt
 * @version 2.1
 */
@JsonDeserialize(builder = net.solarnetwork.central.security.BasicSecurityPolicy.Builder.class)
@JsonSerialize(using = SecurityPolicySerializer.class)
public class BasicSecurityPolicy implements SecurityPolicy, Serializable {

	private static final long serialVersionUID = 2178988304971356373L;

	/**
	 * A builder for {@link BasicSecurityPolicy} instances.
	 * 
	 * Configure properties on instances of this class, then call
	 * {@link #build()} to get a {@link BasicSecurityPolicy} instance.
	 */
	public static class Builder {

		private static final Map<Aggregation, Set<Aggregation>> MAX_AGGREGATION_CACHE = new HashMap<Aggregation, Set<Aggregation>>(
				16);
		private static final Map<LocationPrecision, Set<LocationPrecision>> MAX_LOCATION_PRECISION_CACHE = new HashMap<LocationPrecision, Set<LocationPrecision>>(
				16);

		private Set<Long> nodeIds;
		private Set<String> sourceIds;
		private Set<Aggregation> aggregations;
		private Set<LocationPrecision> locationPrecisions;
		private Aggregation minAggregation;
		private LocationPrecision minLocationPrecision;
		private Set<String> nodeMetadataPaths;
		private Set<String> userMetadataPaths;
		private Set<String> apiPaths;
		private Instant notAfter;
		private Boolean refreshAllowed;

		public Builder withPolicy(SecurityPolicy policy) {
			if ( policy != null ) {
				return this.withAggregations(policy.getAggregations())
						.withMinAggregation(policy.getMinAggregation())
						.withLocationPrecisions(policy.getLocationPrecisions())
						.withMinLocationPrecision(policy.getMinLocationPrecision())
						.withNodeIds(policy.getNodeIds()).withSourceIds(policy.getSourceIds())
						.withNodeMetadataPaths(policy.getNodeMetadataPaths())
						.withUserMetadataPaths(policy.getUserMetadataPaths())
						.withApiPaths(policy.getApiPaths()).withNotAfter(policy.getNotAfter())
						.withRefreshAllowed(policy.getRefreshAllowed());
			}
			return this;
		}

		public Builder withMergedPolicy(SecurityPolicy policy) {
			if ( policy != null ) {
				Builder b = this.withMergedAggregations(policy.getAggregations())
						.withMergedLocationPrecisions(policy.getLocationPrecisions())
						.withMergedNodeIds(policy.getNodeIds())
						.withMergedSourceIds(policy.getSourceIds())
						.withMergedNodeMetadataPaths(policy.getNodeMetadataPaths())
						.withMergedUserMetadataPaths(policy.getUserMetadataPaths())
						.withMergedApiPaths(policy.getApiPaths());
				if ( policy.getMinAggregation() != null ) {
					b = b.withMinAggregation(policy.getMinAggregation());
				}
				if ( policy.getMinLocationPrecision() != null ) {
					b = b.withMinLocationPrecision(policy.getMinLocationPrecision());
				}
				if ( policy.getNotAfter() != null ) {
					b = b.withNotAfter(policy.getNotAfter());
				}
				if ( policy.getRefreshAllowed() != null ) {
					b = b.withRefreshAllowed(policy.getRefreshAllowed());
				}
				return b;
			}
			return this;
		}

		public Builder withNodeIds(Set<Long> nodeIds) {
			this.nodeIds = (nodeIds == null || nodeIds.isEmpty() ? null
					: Collections.unmodifiableSet(nodeIds));
			return this;
		}

		public Builder withNodeMetadataPaths(Set<String> nodeMetadataPaths) {
			this.nodeMetadataPaths = (nodeMetadataPaths == null || nodeMetadataPaths.isEmpty() ? null
					: Collections.unmodifiableSet(nodeMetadataPaths));
			return this;
		}

		public Builder withUserMetadataPaths(Set<String> userMetadataPaths) {
			this.userMetadataPaths = (userMetadataPaths == null || userMetadataPaths.isEmpty() ? null
					: Collections.unmodifiableSet(userMetadataPaths));
			return this;
		}

		public Builder withApiPaths(Set<String> apiPaths) {
			this.apiPaths = (apiPaths == null || apiPaths.isEmpty() ? null
					: Collections.unmodifiableSet(apiPaths));
			return this;
		}

		public Builder withSourceIds(Set<String> sourceIds) {
			this.sourceIds = (sourceIds == null || sourceIds.isEmpty() ? null
					: Collections.unmodifiableSet(sourceIds));
			return this;
		}

		public Builder withAggregations(Set<Aggregation> aggregations) {
			this.aggregations = aggregations;
			return this;
		}

		public Builder withLocationPrecisions(Set<LocationPrecision> locationPrecisions) {
			this.locationPrecisions = locationPrecisions;
			return this;
		}

		public Builder withMergedNodeIds(Set<Long> nodeIds) {
			Set<Long> set = nodeIds;
			if ( this.nodeIds != null && !this.nodeIds.isEmpty() ) {
				set = new LinkedHashSet<Long>(this.nodeIds);
				if ( nodeIds != null ) {
					set.addAll(nodeIds);
				}
			}
			return withNodeIds(set);
		}

		public Builder withMergedNodeMetadataPaths(Set<String> nodeMetadataPaths) {
			Set<String> set = nodeMetadataPaths;
			if ( this.nodeMetadataPaths != null && !this.nodeMetadataPaths.isEmpty() ) {
				set = new LinkedHashSet<String>(this.nodeMetadataPaths);
				if ( nodeMetadataPaths != null ) {
					set.addAll(nodeMetadataPaths);
				}
			}
			return withNodeMetadataPaths(set);
		}

		public Builder withMergedUserMetadataPaths(Set<String> userMetadataPaths) {
			Set<String> set = userMetadataPaths;
			if ( this.userMetadataPaths != null && !this.userMetadataPaths.isEmpty() ) {
				set = new LinkedHashSet<String>(this.userMetadataPaths);
				if ( userMetadataPaths != null ) {
					set.addAll(userMetadataPaths);
				}
			}
			return withUserMetadataPaths(set);
		}

		public Builder withMergedApiPaths(Set<String> apiPaths) {
			Set<String> set = apiPaths;
			if ( this.apiPaths != null && !this.apiPaths.isEmpty() ) {
				set = new LinkedHashSet<String>(this.apiPaths);
				if ( apiPaths != null ) {
					set.addAll(apiPaths);
				}
			}
			return withApiPaths(set);
		}

		public Builder withMergedSourceIds(Set<String> sourceIds) {
			Set<String> set = sourceIds;
			if ( this.sourceIds != null && !this.sourceIds.isEmpty() ) {
				set = new LinkedHashSet<String>(this.sourceIds);
				if ( sourceIds != null ) {
					set.addAll(sourceIds);
				}
			}
			return withSourceIds(set);
		}

		public Builder withMergedAggregations(Set<Aggregation> aggregations) {
			Set<Aggregation> set = aggregations;
			if ( this.aggregations != null && !this.aggregations.isEmpty() ) {
				if ( aggregations != null ) {
					set = new LinkedHashSet<Aggregation>(this.aggregations);
					set.addAll(aggregations);
				} else {
					set = this.aggregations;
				}
			}
			return withAggregations(set);
		}

		public Builder withMergedLocationPrecisions(Set<LocationPrecision> locationPrecisions) {
			Set<LocationPrecision> set = locationPrecisions;
			if ( this.locationPrecisions != null && !this.locationPrecisions.isEmpty() ) {
				if ( locationPrecisions != null ) {
					set = new LinkedHashSet<LocationPrecision>(this.locationPrecisions);
					set.addAll(locationPrecisions);
				} else {
					set = this.locationPrecisions;
				}
			}
			return withLocationPrecisions(set);
		}

		public Builder withMinAggregation(Aggregation minAggregation) {
			this.minAggregation = minAggregation;
			return this;
		}

		private Set<Aggregation> buildAggregations() {
			if ( minAggregation == null && aggregations != null && !aggregations.isEmpty() ) {
				return Collections.unmodifiableSet(aggregations);
			} else if ( minAggregation == null ) {
				return null;
			}
			Set<Aggregation> result = MAX_AGGREGATION_CACHE.get(minAggregation);
			if ( result != null ) {
				return result;
			}
			result = new HashSet<Aggregation>(16);
			for ( Aggregation agg : Aggregation.values() ) {
				if ( agg.compareLevel(minAggregation) > -1 ) {
					result.add(agg);
				}
			}
			result = Collections.unmodifiableSet(EnumSet.copyOf(result));
			MAX_AGGREGATION_CACHE.put(minAggregation, result);
			return result;
		}

		/**
		 * Treat the configured {@code locationPrecisions} set as a single
		 * minimum value or a list of exact values.
		 * 
		 * By default if {@code locationPrecisions} is configured with a single
		 * value it will be treated as a <em>minimum</em> value, and any
		 * {@link LocationPrecision} with a
		 * {@link LocationPrecision#getPrecision()} equal to or higher than that
		 * value's level will be included in the generated
		 * {@link BasicSecurityPolicy#getLocationPrecisions()} set. Set this to
		 * {@code false} to disable that behavior and treat
		 * {@code locationPrecisions} as the exact values to include in the
		 * generated {@link BasicSecurityPolicy#getLocationPrecisions()} set.
		 * 
		 * @param minLocationPrecision
		 *        {@code false} to treat configured location precision values
		 *        as-is, {@code true} to treat as a minimum threshold
		 * @return The builder.
		 */
		public Builder withMinLocationPrecision(LocationPrecision minLocationPrecision) {
			this.minLocationPrecision = minLocationPrecision;
			return this;
		}

		private Set<LocationPrecision> buildLocationPrecisions() {
			if ( minLocationPrecision == null && locationPrecisions != null
					&& !locationPrecisions.isEmpty() ) {
				return Collections.unmodifiableSet(locationPrecisions);
			} else if ( minLocationPrecision == null ) {
				return null;
			}
			Set<LocationPrecision> result = MAX_LOCATION_PRECISION_CACHE.get(minLocationPrecision);
			if ( result != null ) {
				return result;
			}
			result = new HashSet<LocationPrecision>(16);
			for ( LocationPrecision agg : LocationPrecision.values() ) {
				if ( agg.comparePrecision(minLocationPrecision) > -1 ) {
					result.add(agg);
				}
			}
			result = Collections.unmodifiableSet(EnumSet.copyOf(result));
			MAX_LOCATION_PRECISION_CACHE.put(minLocationPrecision, result);
			return result;
		}

		public Builder withNotAfter(Instant date) {
			this.notAfter = date;
			return this;
		}

		public Builder withRefreshAllowed(Boolean refreshAllowed) {
			this.refreshAllowed = refreshAllowed;
			return this;
		}

		public BasicSecurityPolicy build() {
			return new BasicSecurityPolicy(nodeIds, sourceIds, buildAggregations(), minAggregation,
					buildLocationPrecisions(), minLocationPrecision, nodeMetadataPaths,
					userMetadataPaths, apiPaths, notAfter, refreshAllowed);
		}

	}

	/**
	 * Get a new builder instance.
	 * 
	 * @return the new builder
	 * @since 2.0
	 */
	public static Builder builder() {
		return new Builder();
	}

	private final Set<Long> nodeIds;
	private final Set<String> sourceIds;
	private final Set<Aggregation> aggregations;
	private final Set<LocationPrecision> locationPrecisions;
	private final Aggregation minAggregation;
	private final LocationPrecision minLocationPrecision;
	private final Set<String> nodeMetadataPaths;
	private final Set<String> userMetadataPaths;
	private final Set<String> apiPaths;
	private final Instant notAfter;
	private final Boolean refreshAllowed;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code notAfter} property will be set to {@literal null} (for no
	 * expiration date) and {@code refreshable} to {@literal false}.
	 * </p>
	 * 
	 * @param nodeIds
	 *        The node IDs to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param sourceIds
	 *        The source ID to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param aggregations
	 *        The aggregations to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param minAggregation
	 *        If specified, a minimum aggregation level that is allowed.
	 * @param locationPrecisions
	 *        The location precisions to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param minLocationPrecision
	 *        If specified, a minimum location precision that is allowed.
	 * @param nodeMetadataPaths
	 *        The {@code SolarNodeMetadata} paths to restrict to, or
	 *        {@literal null} for no restriction.
	 * @param userMetadataPaths
	 *        The {@code UserNodeMetadata} paths to restrict to, or
	 *        {@literal null} for no restriction.
	 */
	public BasicSecurityPolicy(Set<Long> nodeIds, Set<String> sourceIds, Set<Aggregation> aggregations,
			Aggregation minAggregation, Set<LocationPrecision> locationPrecisions,
			LocationPrecision minLocationPrecision, Set<String> nodeMetadataPaths,
			Set<String> userMetadataPaths) {
		this(nodeIds, sourceIds, aggregations, minAggregation, locationPrecisions, minLocationPrecision,
				nodeMetadataPaths, userMetadataPaths, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeIds
	 *        The node IDs to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param sourceIds
	 *        The source ID to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param aggregations
	 *        The aggregations to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param minAggregation
	 *        If specified, a minimum aggregation level that is allowed.
	 * @param locationPrecisions
	 *        The location precisions to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param minLocationPrecision
	 *        If specified, a minimum location precision that is allowed.
	 * @param nodeMetadataPaths
	 *        The {@code SolarNodeMetadata} paths to restrict to, or
	 *        {@literal null} for no restriction.
	 * @param userMetadataPaths
	 *        The {@code UserNodeMetadata} paths to restrict to, or
	 *        {@literal null} for no restriction.
	 * @param notAfter
	 *        A date after which the token is no longer valid, or
	 *        {@literal null} for no expiration.
	 * @param refreshAllowed
	 *        {@literal true} if the token can be refreshed
	 * @since 2.0
	 */
	public BasicSecurityPolicy(Set<Long> nodeIds, Set<String> sourceIds, Set<Aggregation> aggregations,
			Aggregation minAggregation, Set<LocationPrecision> locationPrecisions,
			LocationPrecision minLocationPrecision, Set<String> nodeMetadataPaths,
			Set<String> userMetadataPaths, Instant notAfter, Boolean refreshAllowed) {
		this(nodeIds, sourceIds, aggregations, minAggregation, locationPrecisions, minLocationPrecision,
				nodeMetadataPaths, userMetadataPaths, null, notAfter, refreshAllowed);
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeIds
	 *        The node IDs to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param sourceIds
	 *        The source ID to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param aggregations
	 *        The aggregations to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param minAggregation
	 *        If specified, a minimum aggregation level that is allowed.
	 * @param locationPrecisions
	 *        The location precisions to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param minLocationPrecision
	 *        If specified, a minimum location precision that is allowed.
	 * @param nodeMetadataPaths
	 *        The {@code SolarNodeMetadata} paths to restrict to, or
	 *        {@literal null} for no restriction.
	 * @param userMetadataPaths
	 *        The {@code UserNodeMetadata} paths to restrict to, or
	 *        {@literal null} for no restriction.
	 * @param apiPaths
	 *        The API paths to restrict to, or {@literal null} for no
	 *        restriction.
	 * @param notAfter
	 *        A date after which the token is no longer valid, or
	 *        {@literal null} for no expiration.
	 * @param refreshAllowed
	 *        {@literal true} if the token can be refreshed
	 * @since 2.0
	 */
	public BasicSecurityPolicy(Set<Long> nodeIds, Set<String> sourceIds, Set<Aggregation> aggregations,
			Aggregation minAggregation, Set<LocationPrecision> locationPrecisions,
			LocationPrecision minLocationPrecision, Set<String> nodeMetadataPaths,
			Set<String> userMetadataPaths, Set<String> apiPaths, Instant notAfter,
			Boolean refreshAllowed) {
		super();
		this.nodeIds = nodeIds;
		this.sourceIds = sourceIds;
		this.aggregations = aggregations;
		this.minAggregation = minAggregation;
		this.locationPrecisions = locationPrecisions;
		this.minLocationPrecision = minLocationPrecision;
		this.nodeMetadataPaths = nodeMetadataPaths;
		this.userMetadataPaths = userMetadataPaths;
		this.apiPaths = apiPaths;
		this.notAfter = notAfter;
		this.refreshAllowed = refreshAllowed;
	}

	@Override
	public Set<Long> getNodeIds() {
		return nodeIds;
	}

	@Override
	public Set<String> getSourceIds() {
		return sourceIds;
	}

	@Override
	public Set<Aggregation> getAggregations() {
		return aggregations;
	}

	@Override
	public Set<LocationPrecision> getLocationPrecisions() {
		return locationPrecisions;
	}

	@Override
	public Aggregation getMinAggregation() {
		return minAggregation;
	}

	@Override
	public LocationPrecision getMinLocationPrecision() {
		return minLocationPrecision;
	}

	@Override
	public Set<String> getNodeMetadataPaths() {
		return nodeMetadataPaths;
	}

	@Override
	public Set<String> getUserMetadataPaths() {
		return userMetadataPaths;
	}

	@Override
	public Set<String> getApiPaths() {
		return apiPaths;
	}

	@Override
	public Instant getNotAfter() {
		return notAfter;
	}

	@Override
	public boolean isValidAt(Instant timestamp) {
		return (notAfter == null || !notAfter.isBefore(timestamp != null ? timestamp : Instant.now()));
	}

	@Override
	public Boolean getRefreshAllowed() {
		return refreshAllowed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aggregations == null) ? 0 : aggregations.hashCode());
		result = prime * result + ((locationPrecisions == null) ? 0 : locationPrecisions.hashCode());
		result = prime * result + ((minAggregation == null) ? 0 : minAggregation.hashCode());
		result = prime * result + ((minLocationPrecision == null) ? 0 : minLocationPrecision.hashCode());
		result = prime * result + ((nodeIds == null) ? 0 : nodeIds.hashCode());
		result = prime * result + ((sourceIds == null) ? 0 : sourceIds.hashCode());
		result = prime * result + ((nodeMetadataPaths == null) ? 0 : nodeMetadataPaths.hashCode());
		result = prime * result + ((userMetadataPaths == null) ? 0 : userMetadataPaths.hashCode());
		result = prime * result + ((apiPaths == null) ? 0 : apiPaths.hashCode());
		result = prime * result + ((notAfter == null) ? 0 : notAfter.hashCode());
		result = prime * result + (refreshAllowed == null ? 0 : refreshAllowed.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof BasicSecurityPolicy) ) {
			return false;
		}
		BasicSecurityPolicy other = (BasicSecurityPolicy) obj;
		if ( aggregations == null ) {
			if ( other.aggregations != null ) {
				return false;
			}
		} else if ( !aggregations.equals(other.aggregations) ) {
			return false;
		}
		if ( locationPrecisions == null ) {
			if ( other.locationPrecisions != null ) {
				return false;
			}
		} else if ( !locationPrecisions.equals(other.locationPrecisions) ) {
			return false;
		}
		if ( minAggregation != other.minAggregation ) {
			return false;
		}
		if ( minLocationPrecision != other.minLocationPrecision ) {
			return false;
		}
		if ( nodeIds == null ) {
			if ( other.nodeIds != null ) {
				return false;
			}
		} else if ( !nodeIds.equals(other.nodeIds) ) {
			return false;
		}
		if ( sourceIds == null ) {
			if ( other.sourceIds != null ) {
				return false;
			}
		} else if ( !sourceIds.equals(other.sourceIds) ) {
			return false;
		}
		if ( nodeMetadataPaths == null ) {
			if ( other.nodeMetadataPaths != null ) {
				return false;
			}
		} else if ( !nodeMetadataPaths.equals(other.nodeMetadataPaths) ) {
			return false;
		}
		if ( userMetadataPaths == null ) {
			if ( other.userMetadataPaths != null ) {
				return false;
			}
		} else if ( !userMetadataPaths.equals(other.userMetadataPaths) ) {
			return false;
		}
		if ( apiPaths == null ) {
			if ( other.apiPaths != null ) {
				return false;
			}
		} else if ( !apiPaths.equals(other.apiPaths) ) {
			return false;
		}
		if ( notAfter == null ) {
			if ( other.notAfter != null ) {
				return false;
			}
		} else if ( !notAfter.equals(other.notAfter) ) {
			return false;
		}
		if ( refreshAllowed == null ) {
			if ( other.refreshAllowed != null ) {
				return false;
			}
		} else if ( !refreshAllowed.equals(other.refreshAllowed) ) {
			return false;
		}
		return true;
	}

}
