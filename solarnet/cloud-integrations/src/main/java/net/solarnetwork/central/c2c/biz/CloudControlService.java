/* ==================================================================
 * CloudControlService.java - 3/11/2025 3:58:45 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Unique;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for a cloud control service.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudControlService
		extends Unique<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/**
	 * A standard data value filter key for a datum stream ID.
	 *
	 * <p>
	 * Some service implementations require a
	 * {@link CloudDatumStreamConfiguration} for the
	 * {@link #dataValues(UserLongCompositePK, Map)} method to function. This
	 * filter key can be used to provide the ID of the datum stream to use.
	 * </p>
	 */
	String DATUM_STREAM_ID_FILTER = "datumStreamId";

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
	 * @return the available filter criteria, never {@literal null}
	 */
	Iterable<LocalizedServiceInfo> dataValueFilters(Locale locale);

	/**
	 * List data values.
	 *
	 * @param integrationId
	 *        the ID of the {@link CloudIntegrationConfiguration} to get the
	 *        data values for
	 * @param filters
	 *        an optional set of search filters to limit the control value
	 *        groups to; the available key values come from the identifiers
	 *        returned by {@link #dataValueFilters(Locale)}
	 * @return the available values, never {@literal null}
	 *
	 */
	Iterable<CloudDataValue> dataValues(UserLongCompositePK integrationId, Map<String, ?> filters);

	/**
	 * Get a set of all instruction topics supported by this service.
	 *
	 * @return the set of supported topics, never {@code null}
	 */
	Set<String> supportedTopics();

	/**
	 * Execute an instruction on a specific control.
	 *
	 * @param cloudControlId
	 *        the ID of the {@link CloudControlConfiguration} to execute the
	 *        instruction on
	 * @param instruction
	 *        the instruction to execute
	 * @return the resulting instruction status, or {@literal null} if not
	 *         accepted
	 */
	InstructionStatus executeInstruction(UserLongCompositePK cloudControlId,
			NodeInstruction instruction);

	/**
	 * Validate an instruction is non-{@code null} and has a supported
	 * {@code topic}.
	 *
	 * @param instruction
	 *        the instruction to validate
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @throws UnsupportedOperationException
	 *         if the topic is not supported
	 */
	default void validateTopicIsSupported(NodeInstruction instruction) {
		if ( !supportedTopics().contains(
				requireNonNullArgument(instruction, "instruction").getInstruction().getTopic()) ) {
			throw new UnsupportedOperationException(
					"Topic [%s] not supported.".formatted(instruction.getInstruction().getTopic()));
		}

	}

}
