/* ==================================================================
 * ResponseTransformService.java - 29/03/2024 9:24:10 am
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
import java.io.OutputStream;
import java.util.Map;
import org.springframework.util.MimeType;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * A service that can transform instruction results into output data.
 *
 * @author matt
 * @version 1.0
 */
public interface ResponseTransformService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/**
	 * Test if the service supports a given desired output type.
	 *
	 * @param type
	 *        the content type of the desired output
	 * @return {@literal true} if the service supports the given output type
	 */
	boolean supportsOutputType(MimeType type);

	/**
	 * Transform a collection of {@link NodeInstruction} into data of a given
	 * content type .
	 *
	 * @param instructions
	 *        the input
	 * @param type
	 *        the desired content type of the output
	 * @param config
	 *        the transform configuration, with service properties specific to
	 *        the service implementation
	 * @param parameters
	 *        optional transformation parameters, implementation specific
	 * @param out
	 *        the destination to write the output data to
	 * @throws IOException
	 *         if an IO error occurs
	 */
	void transformOutput(Iterable<NodeInstruction> instructions, MimeType type,
			IdentifiableConfiguration config, Map<String, ?> parameters, OutputStream out)
			throws IOException;

}
