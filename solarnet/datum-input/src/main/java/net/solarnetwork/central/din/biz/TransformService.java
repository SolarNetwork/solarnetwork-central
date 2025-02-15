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
import java.io.InputStream;
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
 * @version 1.1
 */
public interface TransformService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/** The JSON MIME type. */
	MimeType JSON_TYPE = MimeType.valueOf("application/json");

	/** The XML MIME type. */
	MimeType XML_TYPE = MimeType.valueOf("text/xml");

	/**
	 * A parameter key for a transform instance cache key.
	 *
	 * <p>
	 * This key is meant to provide the transform with a context-specific cache
	 * key that can be applied to a given transform invocation. The key must
	 * uniquely identify the transform configuration.
	 * </p>
	 */
	String PARAM_CONFIGURATION_CACHE_KEY = "cache-key";

	/**
	 * A parameter key for a transform debug output {@link Appendable}.
	 *
	 * <p>
	 * The provided {@link Appendable} will have the raw XSLT transform result
	 * appended.
	 * </p>
	 */
	String PARAM_XSLT_OUTPUT = "xslt-output";

	/** A parameter key for a SolarNetwork user ID. */
	String PARAM_USER_ID = "user-id";

	/** A parameter key for an endpoint ID. */
	String PARAM_ENDPOINT_ID = "endpoint-id";

	/** A parameter key for a transform ID. */
	String PARAM_TRANSFORM_ID = "transform-id";

	/** A parameter key for a preview boolean flag. */
	String PARAM_PREVIEW = "preview";

	/**
	 * A parameter key for the previous transform input data, as an
	 * {@link InputStream}.
	 *
	 * @since 1.1
	 */
	String PARAM_PREVIOUS_INPUT = "previous-input";

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
