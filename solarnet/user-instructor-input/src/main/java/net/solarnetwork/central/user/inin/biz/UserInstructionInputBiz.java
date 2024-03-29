/* ==================================================================
 * UserInstructionInputBiz.java - 29/03/2024 9:11:06 am
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

package net.solarnetwork.central.user.inin.biz;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.MimeType;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.dao.InstructionInputFilter;
import net.solarnetwork.central.inin.domain.InstructionInputConfigurationEntity;
import net.solarnetwork.central.user.inin.domain.InstructionInputConfigurationInput;
import net.solarnetwork.central.user.inin.domain.TransformInstructionResults;
import net.solarnetwork.central.user.inin.domain.TransformOutput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.LocalizedServiceInfo;

/**
 * Service API for SolarUser instruction input support.
 *
 * @author matt
 * @version 1.0
 */
public interface UserInstructionInputBiz {

	/**
	 * Get a localized list of all available
	 * {@link net.solarnetwork.central.inin.biz.RequestTransformService}
	 * information.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the request transform service info
	 */
	Iterable<LocalizedServiceInfo> availableRequestTransformServices(Locale locale);

	/**
	 * Get a localized list of all available
	 * {@link net.solarnetwork.central.inin.biz.ResponseTransformService}
	 * information.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the response transform service info
	 */
	Iterable<LocalizedServiceInfo> availableResponseTransformServices(Locale locale);

	/**
	 * Get a list of all available datum input configurations for a given user.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param userId
	 *        the user ID to get configurations for
	 * @param filter
	 *        an optional filter
	 * @param configurationClass
	 *        the desired configuration type
	 * @return the available configurations, never {@literal null}
	 */
	<C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> FilterResults<C, K> configurationsForUser(
			Long userId, InstructionInputFilter filter, Class<C> configurationClass);

	/**
	 * Get a specific configuration kind for a given ID.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the primary key type
	 * @param id
	 *        the primary key of the configuration to get
	 * @param configurationClass
	 *        the configuration type to get
	 * @return the configuration, or {@literal null} if not available
	 */
	<C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C configurationForId(
			K id, Class<C> configurationClass);

	/**
	 * Save a datum input configuration for a user.
	 *
	 * @param <T>
	 *        the configuration input type
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the primary key type
	 * @param id
	 *        the ID of the configuration to save; at a minimum the user ID
	 *        component must be provided
	 * @param input
	 *        the configuration input to save
	 * @return the saved configuration
	 */
	<T extends InstructionInputConfigurationInput<C, K>, C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> C saveConfiguration(
			K id, T input);

	/**
	 * Update the enabled status of configurations, optionally filtered.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 * @param id
	 *        the ID of the configuration to save; at a minimum the user ID
	 *        component must be provided
	 * @param enabled
	 *        the enabled status to set
	 * @param configurationClass
	 *        the configuration type to get
	 */
	<C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> void enableConfiguration(
			K id, boolean enabled, Class<C> configurationClass);

	/**
	 * Delete a specific datum input configuration.
	 *
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the primary key type
	 * @param id
	 *        the primary key of the configuration to delete
	 * @param configurationClass
	 *        the type of the configuration to delete
	 */
	<C extends InstructionInputConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable & UserIdRelated> void deleteConfiguration(
			K id, Class<C> configurationClass);

	/**
	 * Execute a transform on input data.
	 *
	 * <p>
	 * This method can be used to test out a given endpoint's request and
	 * response transforms. The generated instructions are not executed or
	 * persisted, just returned for inspection. The mock
	 * {@code instructionResults} are provided as input to the response
	 * transform.
	 * </p>
	 *
	 * @param id
	 *        the endpoint configuration ID to execute
	 * @param contentType
	 *        the data content type
	 * @param in
	 *        the input data to transform
	 * @param outputType
	 *        the accepted output type
	 * @param instructionResults
	 *        instruction results to provide to the response transform
	 * @param optional
	 *        parameters
	 * @throws IOException
	 *         if an IO error occurs
	 */
	TransformOutput previewTransform(UserUuidPK id, MimeType contentType, InputStream in,
			MimeType outputType, Collection<TransformInstructionResults> instructionResults,
			Map<String, ?> parameters) throws IOException;
}
