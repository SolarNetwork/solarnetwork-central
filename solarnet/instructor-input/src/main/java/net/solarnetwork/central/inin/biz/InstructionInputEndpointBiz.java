/* ==================================================================
 * InstructionInputEndpointBiz.java - 29/03/2024 9:56:16 am
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.MimeType;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * API for a instruction input endpoint service.
 *
 * @author matt
 * @version 1.0
 */
public interface InstructionInputEndpointBiz {

	/** A node IDs parameter name. */
	String PARAM_NODE_IDS = "nodeIds";

	/**
	 * Import instructions, submitting them for execution.
	 *
	 * @param userId
	 *        the endpoint owner user ID
	 * @param endpointId
	 *        the endpoint ID to import to
	 * @param contentType
	 *        the data content type
	 * @param in
	 *        the data stream to import
	 * @param parameters
	 *        optional parameters, such as {@link #PARAM_NODE_IDS}
	 * @return the collection of instructions successfully imported
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	Collection<NodeInstruction> importInstructions(Long userId, UUID endpointId, MimeType contentType,
			InputStream in, Map<String, String> parameters) throws IOException;

	/**
	 * Generate instructions response.
	 *
	 * @param userId
	 *        the endpoint owner user ID
	 * @param endpointId
	 *        the endpoint ID to import to
	 * @param instructions
	 *        the instructions to generate the response for
	 * @param outputType
	 *        the response content type
	 * @param out
	 *        the output destination
	 * @param parameters
	 *        optional parameters
	 * @throws IOException
	 *         if any IO error occurs
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	void generateResponse(Long userId, UUID endpointId, Collection<NodeInstruction> instructions,
			MimeType outputType, OutputStream out, Map<String, String> parameters) throws IOException;

}
