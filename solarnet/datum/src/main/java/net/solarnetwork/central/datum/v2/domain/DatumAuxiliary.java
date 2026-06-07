/* ==================================================================
 * DatumAuxiliary.java - 4/11/2020 2:33:11 pm
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.BasicDatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * API for an auxiliary datum that is part of a datum stream but managed outside
 * the stream itself.
 *
 * @author matt
 * @version 2.1
 * @since 2.8
 */
public interface DatumAuxiliary extends Identity<DatumAuxiliaryPK> {

	/**
	 * A metadata key for a type classification.
	 *
	 * @since 2.1
	 */
	String TYPE_META_KEY = "type";

	/**
	 * A metadata key for a sub-type classification list.
	 *
	 * @since 2.1
	 */
	String SUB_TYPES_META_KEY = "subTypes";

	/**
	 * A metadata key for the identity of the system that created this record.
	 *
	 * @since 2.1
	 */
	String GENERATED_BY_META_KEY = "generatedBy";

	/**
	 * A metadata key for information about the source of the record.
	 *
	 * @since 2.1
	 */
	String SOURCE_META_KEY = "source";

	/**
	 * A metadata key for a reference to something, such as the source of a
	 * validation record.
	 *
	 * @since 2.1
	 */
	String REFERENCE_META_KEY = "ref";

	/**
	 * A metadata key for a URI.
	 *
	 * @since 2.1
	 */
	String URI_META_KEY = "uri";

	/**
	 * A metadata key for parameters map or list.
	 *
	 * @since 2.1
	 */
	String PARAMETERS_META_KEY = "params";

	/**
	 * A metadata key for a value, such as a datum property value.
	 *
	 * @since 2.1
	 */
	String VALUE_META_KEY = "value";

	/**
	 * A metadata key for a position, such as a "start" or "end".
	 *
	 * @since 2.1
	 */
	String POSITION_META_KEY = "position";

	/**
	 * A metadata key for a timestamp.
	 *
	 * @since 2.1
	 */
	String TIMESTAMP_META_KEY = "timestamp";

	/**
	 * A threshold that a data validation validates.
	 *
	 * @since 2.1
	 */
	String DATA_VALUE_THRESHOLD_META_KEY = "dataValueThreshold";

	/**
	 * A device rated power value.
	 *
	 * @since 2.1
	 */
	String RATED_POWER_META_KEY = "ratedPower";

	/**
	 * A type metadata value for data validation.
	 *
	 * @since 2.1
	 */
	String DATA_VALIDATION_TYPE = "data-validation";

	/**
	 * A generated-by value for SolarNetwork.
	 *
	 * @since 2.1
	 */
	String GENERATED_BY_SOLARNETWORK = "SolarNetwork";

	/**
	 * A position value for "start".
	 *
	 * @since 2.1
	 */
	String START_POSITION = "start";

	/**
	 * A position value for "end".
	 *
	 * @since 2.1
	 */
	String END_POSITION = "end";

	/**
	 * Get the primary key.
	 *
	 * @return the key
	 */
	@Override
	@NonNull
	DatumAuxiliaryPK getId();

	/**
	 * Get the unique ID of the stream this datum is a part of.
	 *
	 * <p>
	 * This is a shortcut for {@code getId().getStreamId()}.
	 * </p>
	 *
	 * @return the stream ID
	 */
	default UUID getStreamId() {
		return getId().getStreamId();
	}

	/**
	 * Get the associated timestamp of this datum.
	 *
	 * <p>
	 * This value represents the point in time the "start" sample properties
	 * associated with this datum take effect.
	 * </p>
	 *
	 * <p>
	 * This is a shortcut for {@code getId().getTimestamp()}.
	 * </p>
	 *
	 * @return the timestamp for this datum
	 */
	default Instant getTimestamp() {
		return getId().getTimestamp();
	}

	/**
	 * Get the type of auxiliary datum this instance represents.
	 *
	 * <p>
	 * This is a shortcut for {@code getId().getKind()}.
	 * </p>
	 *
	 * @return the type
	 */
	default DatumAuxiliaryType getType() {
		return getId().getKind();
	}

	/**
	 * Get a set of datum properties that represent a "final" values to assume
	 * the datum stream had before {@link #getTimestamp()}.
	 *
	 * @return the final sample values
	 */
	@Nullable
	DatumSamples getSamplesFinal();

	/**
	 * Get a set of datum properties that represent the "start" values to assume
	 * the datum stream has starting at {@link #getTimestamp()}.
	 *
	 * @return the start sample values
	 */
	@Nullable
	DatumSamples getSamplesStart();

	/**
	 * Get optional notes or comments about this auxiliary record.
	 *
	 * @return the notes
	 */
	@Nullable
	String getNotes();

	/**
	 * Get optional metadata about this auxiliary record.
	 *
	 * @return the metadata
	 */
	@Nullable
	GeneralDatumMetadata getMetadata();

	/**
	 * Create a data validation source metadata map.
	 *
	 * <p>
	 * Only non-{@code null} and non-empty values will be added to the map.
	 * </p>
	 *
	 * @param reference
	 *        the reference to populate on {@link #REFERENCE_META_KEY}
	 * @param uri
	 *        the URI to populate on {@link #URI_META_KEY}
	 * @param parameters
	 *        an object to populate on {@link #PARAMETERS_META_KEY}
	 * @param value
	 *        the value to populate on {@link #VALUE_META_KEY}
	 * @return the map, never {@code null}
	 * @since 2.1
	 */
	static Map<String, Object> dataValidationSourceMap(@Nullable String reference, @Nullable URI uri,
			@Nullable Object parameters, @Nullable Object value) {
		var result = new LinkedHashMap<String, Object>(3);
		if ( reference != null && !reference.isEmpty() ) {
			result.put(REFERENCE_META_KEY, reference);
		}
		if ( uri != null ) {
			result.put(URI_META_KEY, uri);
		}
		if ( parameters != null ) {
			result.put(PARAMETERS_META_KEY, parameters);
		}
		if ( value != null && !(value instanceof String s && s.isEmpty()) ) {
			result.put(VALUE_META_KEY, value);
		}
		return result;
	}

	/**
	 * Create a SolarNetwork data validation metadata instance.
	 *
	 * @param validationType
	 *        the sub-type metadata value
	 * @param source
	 *        the source data; see
	 *        {@link #dataValidationSourceMap(String, URI, Object, Object)}
	 * @return the metadata instance
	 * @since 2.1
	 */
	static GeneralDatumMetadata dataValidationMetadata(String validationType,
			@Nullable Map<String, Object> source) {
		return dataValidationMetadata(GENERATED_BY_SOLARNETWORK, validationType, source);
	}

	/**
	 * Create a data validation metadata instance.
	 *
	 * @param generatedBy
	 *        the generated-by metadata value
	 * @param validationType
	 *        the sub-type metadata value
	 * @param source
	 *        the source data; see
	 *        {@link #dataValidationSourceMap(String, URI, Object, Object)}
	 * @return the metadata instance
	 * @since 2.1
	 */
	static GeneralDatumMetadata dataValidationMetadata(String generatedBy, String validationType,
			@Nullable Map<String, Object> source) {
		var result = new GeneralDatumMetadata();
		result.putInfoValue(TYPE_META_KEY, DATA_VALIDATION_TYPE);
		result.putInfoValue(SUB_TYPES_META_KEY, List.of(validationType));
		result.putInfoValue(GENERATED_BY_META_KEY, generatedBy);
		if ( source != null ) {
			result.putInfoValue(validationType, SOURCE_META_KEY, source);
		}
		return result;
	}

	/**
	 * Create a {@code Mark} data validation auxiliary record for a data
	 * property value that went over a threshold based on a multiplication
	 * factor.
	 *
	 * @param description
	 *        a description of the type of data being validated
	 * @param validationType
	 *        the validation type to use, e.g.
	 *        {@code DatumValidationType.EnergySpike.getKey()}
	 * @param sourceRef
	 *        a reference to the source of the validation error, such as a Cloud
	 *        Datum Stream device reference
	 * @param requestUri
	 *        an optional URI to the source of the validation error
	 * @param requestParameters
	 *        an optional parameters object used with the request
	 * @param dataValue
	 *        the value that went over the threshold
	 * @param validationThresholdFactor
	 *        the validation threshold factor, e.g. 2 for 2x
	 * @param dataValueThreshold
	 *        the data value threshold that was breached
	 * @param ratedPower
	 *        an optional rated power to include
	 * @param duration
	 *        an optional time duration to include, in milliseconds
	 * @param datumIdent
	 *        the datum ID that includes the data value
	 * @return a collection of new record instances (will only be one)
	 * @since 2.1
	 */
	static List<DatumAuxiliaryRecord> createDataValueOverThresholdFactorValidationRecords(
			String description, String validationType, String sourceRef, @Nullable URI requestUri,
			@Nullable Object requestParameters, Number dataValue, double validationThresholdFactor,
			double dataValueThreshold, @Nullable Integer ratedPower, @Nullable Long duration,
			DatumIdentity datumIdent) {
		final StringBuilder note = new StringBuilder();
		note.append("%s [%.1f] more than %.1fx larger than expected max [%.1f]".formatted(description,
				dataValue.doubleValue(), validationThresholdFactor,
				dataValueThreshold / validationThresholdFactor));
		if ( ratedPower != null ) {
			note.append(" from device rating [%d]".formatted(ratedPower));
		}
		note.append(".");
		final var mark = BasicDatumAuxiliaryRecord.createMark(datumIdent, note.toString(),
				dataValidationMetadata(validationType,
						dataValidationSourceMap(sourceRef, requestUri, requestParameters, dataValue)));
		final GeneralDatumMetadata meta = nonnull(mark.getMetadata(), "Metadata");
		meta.putInfoValue(validationType, CommonUserEvents.DURATION_DATA_KEY, duration);
		meta.putInfoValue(validationType, DatumAuxiliary.RATED_POWER_META_KEY, ratedPower);
		meta.putInfoValue(validationType, DatumAuxiliary.DATA_VALUE_THRESHOLD_META_KEY,
				dataValueThreshold);
		return List.of(mark);
	}

	/**
	 * Create {@code Mark} data validation auxiliary records for a datum stream
	 * time gap.
	 *
	 * @param validationType
	 *        the validation type to use, e.g.
	 *        {@code DatumValidationType.TimeGap.getKey()}
	 * @param sourceRef
	 *        a reference to the source of the validation error, such as a Cloud
	 *        Datum Stream device reference
	 * @param requestUri
	 *        an optional URI to the source of the validation error
	 * @param requestParameters
	 *        an optional parameters object used with the request
	 * @param prevTs
	 *        the previous datum timestamp
	 * @param timeGapThreshold
	 *        the time gap validation threshold
	 * @param datumIdent
	 *        the datum ID that appeared after the time gap
	 * @return a collection of new record instances
	 * @since 2.1
	 */
	static List<DatumAuxiliaryRecord> createTimeGapValidationRecords(String validationType,
			String sourceRef, @Nullable URI requestUri, @Nullable Object requestParameters,
			Instant prevTs, Duration timeGapThreshold, DatumIdentity datumIdent) {
		final String correlationId = UUID.randomUUID().toString();
		final long timeDiffMs = ChronoUnit.MILLIS.between(prevTs, datumIdent.getTimestamp());
		final long timeDiffSecs = timeDiffMs / 1000;
		final Map<String, Object> sourceMap = dataValidationSourceMap(sourceRef, requestUri,
				requestParameters, timeDiffMs);

		final String markNote = "Time gap %ds not within threshold %s.".formatted(timeDiffSecs,
				timeGapThreshold.toString());
		final var startMark = BasicDatumAuxiliaryRecord.createMark(
				new DatumId.DatumIdent(datumIdent.getKind(), datumIdent.getObjectId(),
						datumIdent.getSourceId(), prevTs),
				markNote, dataValidationMetadata(validationType, sourceMap));
		final GeneralDatumMetadata startMeta = nonnull(startMark.getMetadata(), "Metadata");
		startMeta.putInfoValue(validationType, CommonUserEvents.CORRELATION_ID_DATA_KEY, correlationId);
		startMeta.putInfoValue(validationType, DatumAuxiliary.POSITION_META_KEY,
				DatumAuxiliary.START_POSITION);
		startMeta.putInfoValue(validationType, DatumAuxiliary.TIMESTAMP_META_KEY,
				datumIdent.getTimestamp());
		startMeta.putInfoValue(validationType, CommonUserEvents.DURATION_DATA_KEY, timeDiffMs);
		startMeta.putInfoValue(validationType, DatumAuxiliary.DATA_VALUE_THRESHOLD_META_KEY,
				timeGapThreshold.toString());

		final var endMark = BasicDatumAuxiliaryRecord.createMark(datumIdent, markNote,
				dataValidationMetadata(validationType, sourceMap));
		final GeneralDatumMetadata endMeta = nonnull(endMark.getMetadata(), "Metadata");
		endMeta.putInfoValue(validationType, CommonUserEvents.CORRELATION_ID_DATA_KEY, correlationId);
		endMeta.putInfoValue(validationType, DatumAuxiliary.POSITION_META_KEY,
				DatumAuxiliary.END_POSITION);
		endMeta.putInfoValue(validationType, DatumAuxiliary.TIMESTAMP_META_KEY, prevTs);
		endMeta.putInfoValue(validationType, CommonUserEvents.DURATION_DATA_KEY, timeDiffMs);
		endMeta.putInfoValue(validationType, DatumAuxiliary.DATA_VALUE_THRESHOLD_META_KEY,
				timeGapThreshold.toString());

		return List.of(startMark, endMark);
	}

}
