/* ==================================================================
 * RequestTransformService.java - 29/03/2024 9:13:02 am
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

package net.solarnetwork.central.inin.biz;

import java.io.IOException;
import java.util.Map;
import org.springframework.util.MimeType;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * A service that can transform input data into instruction instances.
 *
 * @author matt
 * @version 1.0
 */
public interface RequestTransformService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

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
	 * {@link NodeInstruction}.
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
	Iterable<NodeInstruction> transformInput(Object input, MimeType type,
			IdentifiableConfiguration config, Map<String, ?> parameters) throws IOException;

}
