/* ==================================================================
 * SecurityPolicySerializer.java - 9/10/2016 12:45:17 PM
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.LocationPrecision;

/**
 * JSON serializer for {@link SecurityPolicy}.
 * 
 * @author matt
 * @version 1.1
 */
public class SecurityPolicySerializer extends StdSerializer<SecurityPolicy> {

	public SecurityPolicySerializer() {
		super(SecurityPolicy.class);
	}

	@Override
	public void serialize(SecurityPolicy policy, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		if ( policy == null ) {
			generator.writeNull();
			return;
		}
		generator.writeStartObject();
		if ( policy.getNodeIds() != null && !policy.getNodeIds().isEmpty() ) {
			generator.writeArrayFieldStart("nodeIds");

			// maintain node IDs in natural sort order
			Long[] ids = policy.getNodeIds().toArray(new Long[policy.getNodeIds().size()]);
			Arrays.sort(ids);
			for ( Long id : ids ) {
				generator.writeNumber(id);
			}

			generator.writeEndArray();
		}
		if ( policy.getSourceIds() != null && !policy.getSourceIds().isEmpty() ) {
			generator.writeArrayFieldStart("sourceIds");
			for ( String id : policy.getSourceIds() ) {
				generator.writeString(id);
			}
			generator.writeEndArray();
		}

		Set<Aggregation> aggregations = policy.getAggregations();
		if ( policy.getMinAggregation() != null ) {
			generator.writeStringField("minAggregation", policy.getMinAggregation().name());
		} else if ( aggregations != null && !aggregations.isEmpty() ) {
			generator.writeArrayFieldStart("aggregations");
			for ( Aggregation val : aggregations ) {
				generator.writeString(val.name());
			}
			generator.writeEndArray();
		}

		Set<LocationPrecision> locationPrecisions = policy.getLocationPrecisions();
		if ( policy.getMinLocationPrecision() != null ) {
			generator.writeStringField("minLocationPrecision", policy.getMinLocationPrecision().name());
		} else if ( locationPrecisions != null && !locationPrecisions.isEmpty() ) {
			generator.writeArrayFieldStart("locationPrecisions");
			for ( LocationPrecision val : locationPrecisions ) {
				generator.writeString(val.name());
			}
			generator.writeEndArray();
		}

		Set<String> nodeMetadataPaths = policy.getNodeMetadataPaths();
		if ( nodeMetadataPaths != null && !nodeMetadataPaths.isEmpty() ) {
			generator.writeArrayFieldStart("nodeMetadataPaths");
			for ( String path : nodeMetadataPaths ) {
				generator.writeString(path);
			}
		}

		Set<String> userMetadataPaths = policy.getUserMetadataPaths();
		if ( userMetadataPaths != null && !userMetadataPaths.isEmpty() ) {
			generator.writeArrayFieldStart("userMetadataPaths");
			for ( String path : userMetadataPaths ) {
				generator.writeString(path);
			}
		}

		generator.writeEndObject();

	}

}
