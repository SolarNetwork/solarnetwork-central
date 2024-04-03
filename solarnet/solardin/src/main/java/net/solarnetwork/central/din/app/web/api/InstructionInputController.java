/* ==================================================================
 * InstructionInputController.java - 30/03/2024 4:28:35 pm
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

package net.solarnetwork.central.din.app.web.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.inin.biz.InstructionInputEndpointBiz;
import net.solarnetwork.central.inin.security.SecurityEndpointCredential;
import net.solarnetwork.central.inin.security.SecurityUtils;
import net.solarnetwork.central.web.MaxUploadSizeInputStream;
import net.solarnetwork.io.ProvidedOutputStream;
import net.solarnetwork.util.ObjectUtils;

/**
 * Instruction input controller.
 *
 * @author matt
 * @version 1.0
 */
@RestController("v1InstructionInputController")
@RequestMapping("/api/v1/instr/endpoint/{endpointId}")
public class InstructionInputController {

	private final InstructionInputEndpointBiz inputBiz;
	private final long maxInputLength;

	/**
	 * Constructor.
	 *
	 * @param inputBiz
	 *        the input service
	 * @param maxInputLength
	 *        the maximum datum input length
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public InstructionInputController(InstructionInputEndpointBiz inputBiz,
			@Value("${app.inin.max-input-length}") long maxInputLength) {
		super();
		this.inputBiz = ObjectUtils.requireNonNullArgument(inputBiz, "inputBiz");
		this.maxInputLength = maxInputLength;
	}

	/**
	 * Post instruction.
	 *
	 * @param endpointId
	 *        the endpoint ID to post to
	 * @param contentType
	 *        the data content type
	 * @param encoding
	 *        the data encoding
	 * @param in
	 *        the data stream
	 * @return the persisted datum IDs
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public void postDatum(@PathVariable("endpointId") UUID endpointId,
			@RequestHeader(value = "Content-Type", required = true) String contentType,
			@RequestHeader(value = "Content-Encoding", required = false) String encoding, WebRequest req,
			InputStream in, @RequestHeader(value = "Accept", required = true) String accept,
			@RequestHeader(value = "Accept-Encoding", required = false) String acceptEncoding,
			HttpServletResponse response) throws IOException {
		final SecurityEndpointCredential actor = SecurityUtils.getCurrentEndpointCredential();

		final MediaType mediaType = MediaType.parseMediaType(contentType);
		final MediaType outputType = MediaType.parseMediaType(accept);

		InputStream input = in;
		if ( encoding != null && encoding.toLowerCase().contains("gzip") ) {
			input = new GZIPInputStream(in);
		}

		var params = new HashMap<String, String>(8);
		for ( Iterator<String> itr = req.getParameterNames(); itr.hasNext(); ) {
			var paramName = itr.next();
			String[] vals = req.getParameterValues(paramName);
			if ( vals != null && vals.length > 0 ) {
				params.put(paramName, vals[0]);
			}
		}

		// use ProvidedOutputStream to delay opening output stream in case of exception
		try (ProvidedOutputStream out = new ProvidedOutputStream(() -> {
			try {
				response.setContentType(mediaType.toString());
				OutputStream o = response.getOutputStream();
				if ( acceptEncoding != null && acceptEncoding.contains("gzip") ) {
					o = new GZIPOutputStream(o);
				}
				return o;
			} catch ( IOException e ) {
				throw new IllegalStateException("IOException generating output", e);
			}
		})) {

			// limit input size
			input = new MaxUploadSizeInputStream(input, maxInputLength);
			var instructions = inputBiz.importInstructions(actor.getUserId(), endpointId, mediaType,
					input, params);

			inputBiz.generateResponse(actor.getUserId(), endpointId, instructions, outputType, out,
					params);
		}
	}

}
