/* ==================================================================
 * CloudIntegrationTestUtils.java - 2/06/2026 11:16:21 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService.DEFAULT_TIME_GAP_VALIDATION_THRESHOLD;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.common.http.HttpUserEvents;
import net.solarnetwork.central.datum.domain.DatumValidationType;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;

/**
 * Helper methods for Cloud Integration tests.
 *
 * @author matt
 * @version 1.0
 */
public final class CloudIntegrationTestUtils implements CloudIntegrationsUserEvents, HttpUserEvents {

	private CloudIntegrationTestUtils() {
		super();
		// not available
	}

	/**
	 * Create an expected metadata map for a time-gap validation record.
	 *
	 * @return the metadata map
	 */
	public static Map<String, Object> energySpikeValidationMetadata() {
		// @formatter:off
		return new HashMap<String, Object>(Map.of(
				  DatumAuxiliary.GENERATED_BY_META_KEY, DatumAuxiliary.GENERATED_BY_SOLARNETWORK
				, DatumAuxiliary.TYPE_META_KEY, DatumAuxiliary.DATA_VALIDATION_TYPE
				, DatumAuxiliary.SUB_TYPES_META_KEY, List.of(DatumValidationType.ENERGY_SPIKE_VALIDATION_TYPE)
				));
		// @formatter:on
	}

	/**
	 * Create an expected property metadata map for an energy-spike validation.
	 *
	 * @param sourceRef
	 *        the source reference
	 * @param requestUri
	 *        the request URI
	 * @param requestBody
	 *        the optional request body
	 * @param energyValue
	 *        the failed energy value
	 * @param duration
	 *        the amount of time associated with the energy value
	 * @param dataValueThreshold
	 *        the data value threshold
	 * @param ratedPower
	 *        the rated device power, if known
	 * @return the metadata map
	 */
	public static Map<String, Object> energySpikeValidationPropertyMetadata(String sourceRef,
			URI requestUri, @Nullable Object requestBody, Number energyValue, Number duration,
			Number dataValueThreshold, @Nullable Number ratedPower) {
		// @formatter:off
		final var sourceMap = new HashMap<String, Object>(Map.of(
				DatumAuxiliary.REFERENCE_META_KEY, sourceRef,
				DatumAuxiliary.URI_META_KEY, requestUri,
				DatumAuxiliary.VALUE_META_KEY, energyValue
				));
		// @formatter:on
		if ( requestBody != null ) {
			sourceMap.put(DatumAuxiliary.PARAMETERS_META_KEY, requestBody);
		}
		// @formatter:off
		final Map<String, Object> result = new HashMap<>( Map.of(
				DURATION_DATA_KEY, duration,
				DatumAuxiliary.DATA_VALUE_THRESHOLD_META_KEY, dataValueThreshold,
				SOURCE_DATA_KEY, sourceMap
			));
		if (ratedPower != null ) {
			result.put("ratedPower", ratedPower);
		}
		return result;
		// @formatter:on
	}

	/**
	 * Create an expected metadata map for a time-gap validation record.
	 *
	 * @return the metadata map
	 */
	public static Map<String, Object> timeGapValidationMetadata() {
		// @formatter:off
		return new HashMap<String, Object>(Map.of(
				  DatumAuxiliary.GENERATED_BY_META_KEY, DatumAuxiliary.GENERATED_BY_SOLARNETWORK
				, DatumAuxiliary.TYPE_META_KEY, DatumAuxiliary.DATA_VALIDATION_TYPE
				, DatumAuxiliary.SUB_TYPES_META_KEY, List.of(DatumValidationType.TIME_GAP_VALIDATION_TYPE)
				));
		// @formatter:on
	}

	/**
	 * Create an expected property metadata map for a time-gap validation
	 * record.
	 *
	 * @param sourceRef
	 *        the source reference
	 * @param requestUri
	 *        the request URI
	 * @param requestBody
	 *        the optional request body
	 * @param prevDatumTs
	 *        the previous datum timestamp
	 * @param datumTs
	 *        the current datum timestamp
	 * @param startPosition
	 *        {@code true} for the start position record, {@code false} for the
	 *        end position
	 * @param correlationId
	 *        the correlation ID to include, if known
	 * @return the metadata map
	 */
	public static Map<String, Object> timeGapValidationPropertyMetadata(String sourceRef, URI requestUri,
			@Nullable Object requestBody, Instant prevDatumTs, Instant datumTs, boolean startPosition,
			@Nullable String correlationId) {
		final var sourceMap = new HashMap<String, Object>(Map.of(DatumAuxiliary.REFERENCE_META_KEY,
				sourceRef, DatumAuxiliary.URI_META_KEY, requestUri, DatumAuxiliary.VALUE_META_KEY,
				ChronoUnit.MILLIS.between(prevDatumTs, datumTs)));
		if ( requestBody != null ) {
			sourceMap.put(DatumAuxiliary.PARAMETERS_META_KEY, requestBody);
		}

		// @formatter:off
		final var result = new HashMap<String, Object>(Map.of(
				  DatumAuxiliary.POSITION_META_KEY, (startPosition
						? DatumAuxiliary.START_POSITION
						: DatumAuxiliary.END_POSITION)
				, DURATION_DATA_KEY, ChronoUnit.MILLIS.between(prevDatumTs, datumTs)
				, DatumAuxiliary.TIMESTAMP_META_KEY, (startPosition
						? datumTs
						: prevDatumTs)
				, DatumAuxiliary.DATA_VALUE_THRESHOLD_META_KEY, DEFAULT_TIME_GAP_VALIDATION_THRESHOLD.toString()
				, SOURCE_DATA_KEY, sourceMap
				));
		// @formatter:on
		if ( correlationId != null ) {
			result.put(CORRELATION_ID_DATA_KEY, correlationId);
		}
		return result;
	}

}
