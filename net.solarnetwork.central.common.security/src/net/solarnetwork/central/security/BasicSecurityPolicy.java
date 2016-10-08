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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.LocationPrecision;

/**
 * Basic implementation of {@link SecurityPolicy}.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = net.solarnetwork.central.security.BasicSecurityPolicy.Builder.class)
public class BasicSecurityPolicy implements SecurityPolicy {

	/**
	 * A builder for {@link BasicSecurityPolicy} instances.
	 * 
	 * Configure properties on instances of this class, then call
	 * {@link #build()} to get a {@link BasicSecurityPolicy} instance.
	 * 
	 * @author matt
	 * @version 1.0
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
		private boolean minAggregation = true;
		private boolean minLocationPrecision = true;

		public Builder withNodeIds(Set<Long> nodeIds) {
			this.nodeIds = (nodeIds == null || nodeIds.isEmpty() ? null
					: Collections.unmodifiableSet(nodeIds));
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

		/**
		 * Treat the configured {@code aggregations} set as a single minimum
		 * value or a list of exact values.
		 * 
		 * By default if {@code aggregations} is configured with a single value
		 * it will be treated as a <em>minimum</em> value, and any
		 * {@link Aggregation} with a {@link Aggregation#getLevel()} equal to or
		 * higher than that value's level will be included in the generated
		 * {@link BasicSecurityPolicy#getAggregations()} set. Set this to
		 * {@code false} to disable that behavior and treat {@code aggregations}
		 * as the exact values to include in the generated
		 * {@link BasicSecurityPolicy#getAggregations()} set.
		 * 
		 * @param minAggregation
		 *        {@code false} to treat configured aggregation values as-is,
		 *        {@code true} to treat as a minimum threshold
		 * @return The builder.
		 */
		public Builder withMinAggregation(boolean minAggregation) {
			this.minAggregation = minAggregation;
			return this;
		}

		private Set<Aggregation> buildAggregations() {
			if ( aggregations == null || aggregations.isEmpty() ) {
				return null;
			}
			if ( !minAggregation || aggregations.size() > 1 ) {
				return Collections.unmodifiableSet(aggregations);
			}
			Aggregation min = aggregations.iterator().next();
			Set<Aggregation> result = MAX_AGGREGATION_CACHE.get(min);
			if ( result != null ) {
				return result;
			}
			result = new HashSet<Aggregation>(16);
			for ( Aggregation agg : Aggregation.values() ) {
				if ( agg.compareLevel(min) > -1 ) {
					result.add(agg);
				}
			}
			result = Collections.unmodifiableSet(EnumSet.copyOf(result));
			MAX_AGGREGATION_CACHE.put(min, result);
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
		public Builder withMinLocationPrecision(boolean minLocationPrecision) {
			this.minLocationPrecision = minLocationPrecision;
			return this;
		}

		private Set<LocationPrecision> buildLocationPrecisions() {
			if ( locationPrecisions == null || locationPrecisions.isEmpty() ) {
				return null;
			}
			if ( !minLocationPrecision || locationPrecisions.size() > 1 ) {
				return Collections.unmodifiableSet(locationPrecisions);
			}
			LocationPrecision min = locationPrecisions.iterator().next();
			Set<LocationPrecision> result = MAX_LOCATION_PRECISION_CACHE.get(min);
			if ( result != null ) {
				return result;
			}
			result = new HashSet<LocationPrecision>(16);
			for ( LocationPrecision agg : LocationPrecision.values() ) {
				if ( agg.comparePrecision(min) > -1 ) {
					result.add(agg);
				}
			}
			result = Collections.unmodifiableSet(EnumSet.copyOf(result));
			MAX_LOCATION_PRECISION_CACHE.put(min, result);
			return result;
		}

		public BasicSecurityPolicy build() {
			return new BasicSecurityPolicy(nodeIds, sourceIds, buildAggregations(),
					buildLocationPrecisions());
		}

	}

	private final Set<Long> nodeIds;
	private final Set<String> sourceIds;
	private final Set<Aggregation> aggregations;
	private final Set<LocationPrecision> locationPrecisions;

	/**
	 * Constructor.
	 * 
	 * @param nodeIds
	 *        The node IDs to restrict to, or {@code null} for no restriction.
	 * @param sourceIds
	 *        The source ID to restrict to, or {@code null} for no restriction.
	 * @param aggregations
	 *        The aggregations to restrict to, or {@code null} for no
	 *        restriction.
	 * @param locationPrecisions
	 *        The location precisions to restrict to, or {@code null} for no
	 *        restriction.
	 */
	public BasicSecurityPolicy(Set<Long> nodeIds, Set<String> sourceIds, Set<Aggregation> aggregations,
			Set<LocationPrecision> locationPrecisions) {
		super();
		this.nodeIds = nodeIds;
		this.sourceIds = sourceIds;
		this.aggregations = aggregations;
		this.locationPrecisions = locationPrecisions;
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

}
