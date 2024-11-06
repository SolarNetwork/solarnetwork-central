/* ==================================================================
 * CloudDatumStreamLocalizedServiceInfo.java - 23/10/2024 8:52:22 am
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

package net.solarnetwork.central.c2c.domain;

import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.settings.ConfigurableLocalizedServiceInfo;
import net.solarnetwork.util.IntRange;

/**
 * Localized service information for cloud datum stream services.
 *
 * @author matt
 * @version 1.2
 */
public interface CloudDatumStreamLocalizedServiceInfo extends ConfigurableLocalizedServiceInfo {

	/**
	 * Get the polling requirement.
	 *
	 * @return {@literal true} if this service requires polling to acquire data
	 */
	boolean isRequiresPolling();

	/**
	 * Tell if the service requires a {@link CloudDatumStreamConfiguration} for
	 * the
	 * {@link CloudDatumStreamService#dataValues(net.solarnetwork.central.domain.UserLongCompositePK, java.util.Map)
	 * dataValues()} method to function.
	 *
	 * <p>
	 * A cloud datum stream service might require service properties like
	 * credentials that operate at the cloud datum stream level, rather than the
	 * cloud integration level. If that is the case this method will return
	 * {@code true} and the ID of the datum stream configuration to use can be
	 * provided on the {@link CloudDatumStreamService#DATUM_STREAM_ID_FILTER}
	 * filter key.
	 * </p>
	 *
	 * @return {@code true} if this service requires a datum stream for the
	 *         {@link CloudDatumStreamService#dataValues(net.solarnetwork.central.domain.UserLongCompositePK, java.util.Map)
	 *         dataValues()} method to function
	 * @since 1.1
	 */
	boolean isDataValuesRequireDatumStream();

	/**
	 * Tell if the service supports returning datum for arbitrary date ranges.
	 *
	 * <p>
	 * Some
	 * {@link CloudDatumStreamService#datum(CloudDatumStreamConfiguration, CloudDatumStreamQueryFilter)}
	 * implementations may not support arbitrary date ranges in the filter
	 * argument, or may ignore any provided date range completely. In those
	 * cases this method will return {@code false}, and the datum returned when
	 * a date range is included in the filter will be implementation-specific.
	 * </p>
	 *
	 * @return {@code true} if this service supports querying datum with
	 *         arbitrary date ranges, {@code false} if date ranges are not
	 *         supported or limited in some way
	 * @since 1.2
	 */
	boolean isArbitraryDateRangesSupported();

	/**
	 * Get a collection of supported placeholder keys.
	 *
	 * @return the placeholders, or {@literal null}
	 */
	Iterable<String> getSupportedPlaceholders();

	/**
	 * Get a set of data value wildcard identifier levels.
	 *
	 * <p>
	 * The values in this set represent 0-based offsets within a
	 * {@link CloudDataValue#getIdentifiers()} list that allow a
	 * {@link CloudDataValue#WILDCARD_IDENTIFIER} value to be specified in a
	 * {@link CloudDatumStreamPropertyConfiguration#getValueReference()}.
	 * <p>
	 *
	 * @return the 0-based list offsets, or {@literal null}
	 */
	Iterable<Integer> getSupportedDataValueWildcardIdentifierLevels();

	/**
	 * Get the data value identifier levels that can uniquely identify a
	 * SolarNetwork source ID.
	 *
	 * @return the 0-based range, or {@literal null}
	 */
	IntRange getDataValueIdentifierLevelsSourceIdRange();

}
