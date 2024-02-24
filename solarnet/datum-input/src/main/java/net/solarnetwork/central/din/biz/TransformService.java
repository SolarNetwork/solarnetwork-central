/* ==================================================================
 * TransformService.java - 20/02/2024 5:38:09 pm
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

package net.solarnetwork.central.din.biz;

import java.io.IOException;
import java.util.Map;
import org.springframework.util.MimeType;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * A service that can transform input data into datum instances.
 *
 * @author matt
 * @version 1.0
 */
public interface TransformService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/** The JSON MIME type. */
	public static final MimeType JSON_TYPE = MimeType.valueOf("application/json");

	/** The XML MIME type. */
	public static final MimeType XML_TYPE = MimeType.valueOf("text/xml");

	/**
	 * A parameter key for a transform instance cache key.
	 *
	 * <p>
	 * This key is meant to provide the transform with a context-specific cache
	 * key that can be applied to a given transform invocation. The key must
	 * uniquely identify the transform configuration.
	 * </p>
	 */
	public static final String PARAM_CONFIGURATION_CACHE_KEY = "cache-key";

	/**
	 * Test if the service supports a given input object.
	 *
	 * @param input
	 *        the input to test
	 * @param type
	 *        the content type of the input
	 * @return {@literal true} if the service supports the given input
	 */
	boolean supportsInput(Object input, MimeType type);

	/**
	 * Transform an input object of a given content type into a collection of
	 * {@link Datum}.
	 *
	 * @param input
	 *        the input
	 * @param type
	 *        the content type of the input
	 * @param config
	 *        the transform configuration, with service properties specific to
	 *        the service implementation
	 * @param parameters
	 *        optional transformation parameters, implementation specific
	 * @return the output datum
	 * @throws IOException
	 *         if an IO error occurs
	 */
	Iterable<Datum> transform(Object input, MimeType type, IdentifiableConfiguration config,
			Map<String, ?> parameters) throws IOException;

}
