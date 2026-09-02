/* ==================================================================
 * CloudDatumStreamService.java - 29/09/2024 2:50:01 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Unique;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for a cloud datum stream service.
 *
 * @author matt
 * @version 2.3
 */
public interface CloudDatumStreamService
		extends Unique<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/**
	 * A standard setting for either a map or comma-delimited mapping list of
	 * data value references to associated source ID values.
	 *
	 * <p>
	 * This setting is intended to be used by cloud services that can provide
	 * multiple SolarNetwork datum streams, as a way to map each cloud device to
	 * a SolarNetwork source.
	 * </p>
	 */
	String SOURCE_ID_MAP_SETTING = "sourceIdMap";

	/**
	 * The setting to upper-case source ID values.
	 *
	 * @since 1.4
	 */
	String UPPER_CASE_SOURCE_ID_SETTING = "upperCaseSourceId";

	/**
	 * A standard data value filter key for a datum stream ID.
	 *
	 * <p>
	 * Some service implementations require a
	 * {@link CloudDatumStreamConfiguration} for the
	 * {@link #dataValues(UserLongCompositePK, Map)} method to function. This
	 * filter key can be used to provide the ID of the datum stream to use.
	 * </p>
	 *
	 * @since 1.3
	 */
	String DATUM_STREAM_ID_FILTER = "datumStreamId";

	/**
	 * A standard setting for either an array or comma-delimited list of virtual
	 * source ID values.
	 *
	 * <p>
	 * Virtual source IDs are those created exclusively from expression property
	 * configurations.
	 * </p>
	 *
	 * @since 1.5
	 */
	String VIRTUAL_SOURCE_IDS_SETTING = "virtualSourceIds";

	/**
	 * A standard setting for either a map or comma-delimited mapping list of
	 * data value references to associated interval strings.
	 *
	 * <p>
	 * This setting is intended to be used by cloud services that can optimize
	 * their time-based queries based on the date constraints in this mapping.
	 * The keys in the mapping represent data value references and the values
	 * are intervals, formatted like {@code "date1/date2"}. Either date can be
	 * omitted to represent an open-ended time span. See
	 * {@link net.solarnetwork.central.support.DateTimeUtils#intervalMap(Map)}
	 * for more details.
	 * </p>
	 *
	 * @since 2.1
	 */
	String OPERATIONAL_DATE_RANGES_SETTING = "operationalDateRanges";

	/**
	 * A standard setting for either an list or comma-delimited list of
	 * "validation types" to ignore.
	 *
	 * <p>
	 * Validation types are implementation specific, and denote types of
	 * validation like {@code energy-spike}.
	 * </p>
	 *
	 * @since 2.1
	 */
	String VALIDATION_IGNORE_SETTING = "validationIgnore";

	/**
	 * A standard setting for an energy validation threshold.
	 *
	 * <p>
	 * This number value represents a multiplication factor by which an energy
	 * value exceeds the expected maximum energy value for its time period.
	 * </p>
	 *
	 * @since 2.2
	 */
	String ENERGY_VALIDATION_THRESHOLD_SETTING = "energyValidationThreshold";

	/**
	 * A standard setting for an time gap validation threshold.
	 *
	 * <p>
	 * This value represents a duration between two datum that must be met to
	 * trigger a "time gap" style validation event.
	 * </p>
	 * <p>
	 * The value can be an ISO duration like {@code PT2H} for "2 hours" or an
	 * integer number of seconds.
	 * </p>
	 *
	 * @since 2.2
	 */
	String TIME_GAP_VALIDATION_THRESHOLD_SETTING = "timeGapValidationThreshold";

	/**
	 * The search filter for generated auxiliary records.
	 *
	 * @since 2.3
	 */
	public static final String GENERATED_AUXILIARY_SEARCH_FILTER = "(&(m/%s=%s)(m/%s=%s))".formatted(
			DatumAuxiliary.GENERATED_BY_META_KEY, DatumAuxiliary.GENERATED_BY_SOLARNETWORK,
			DatumAuxiliary.TYPE_META_KEY, DatumAuxiliary.DATA_VALIDATION_TYPE);

	/**
	 * Get a localized collection of the available data value filter criteria.
	 *
	 * <p>
	 * The {@link LocalizedServiceInfo#getId()} of each returned object
	 * represents the name of a filter parameter that can be passed to
	 * {@link #dataValues(UserLongCompositePK, Map)}.
	 * </p>
	 *
	 * @param locale
	 *        the desired locale
	 * @return the available filter criteria, never {@code null}
	 */
	Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale);

	/**
	 * Get a localized collection of the available data validation types.
	 *
	 * <p>
	 * The {@link LocalizedServiceInfo#getId()} of each returned object
	 * represents a validation type key that can be configured on the
	 * {@link #VALIDATION_IGNORE_SETTING} to disable. By default all supported
	 * validations are enabled.
	 * </p>
	 *
	 * @param locale
	 *        the desired locale
	 * @return the available filter criteria, never {@code null}
	 * @since 2.1
	 */
	default Iterable<LocalizedServiceInfo> supportedValidations(Locale locale) {
		return List.of();
	}

	/**
	 * List data values.
	 *
	 * @param integrationId
	 *        the ID of the {@link CloudIntegrationConfiguration} to get the
	 *        data values for
	 * @param filters
	 *        an optional set of search filters to limit the data value groups
	 *        to; the available key values come from the identifiers returned by
	 *        {@link #dataValueFilters(Locale)}
	 * @return the available values, never {@code null}
	 *
	 */
	Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId,
			@Nullable Map<String, ?> filters);

	/**
	 * Get the latest available datum for a datum stream configuration.
	 *
	 * <p>
	 * This method can be used to verify a datum stream's configuration is
	 * valid, such as credentials and the mapping onto datum.
	 * </p>
	 *
	 * @param datumStream
	 *        the datum stream configuration to get the latest datum for
	 * @return the result, never {@code null}
	 */
	Iterable<Datum> latestDatum(CloudDatumStreamConfiguration datumStream);

	/**
	 * List datum for a datum stream configuration that match some filter
	 * criteria.
	 *
	 * @param datumStream
	 *        the datum stream configuration to get the latest datum for
	 * @param filter
	 *        the query filter
	 * @return the result, never {@code null}
	 */
	CloudDatumStreamQueryResult datum(CloudDatumStreamConfiguration datumStream,
			CloudDatumStreamQueryFilter filter);

	/**
	 * Resolve the source IDs generated by a given datum stream configuration.
	 *
	 * @param datumStream
	 *        the datum stream configuration to resolve the generated source IDs
	 *        for
	 * @return the source IDs, never {@code null} but possibly empty
	 * @since 2.3
	 */
	default Set<String> datumStreamSourceIds(CloudDatumStreamConfiguration datumStream) {
		final Map<String, String> sourceIdMap = datumStream
				.servicePropertyStringMap(SOURCE_ID_MAP_SETTING);
		if ( sourceIdMap != null ) {
			return Set.copyOf(sourceIdMap.values());
		} else if ( datumStream.getSourceId() != null ) {
			return Set.of(datumStream.getSourceId());
		}
		return Set.of();
	}

}
