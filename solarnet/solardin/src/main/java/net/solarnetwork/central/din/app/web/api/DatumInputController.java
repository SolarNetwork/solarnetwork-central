/* ==================================================================
 * DatumInputController.java - 24/02/2024 8:07:03 am
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

import static net.solarnetwork.domain.Result.success;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import net.solarnetwork.central.din.biz.DatumInputEndpointBiz;
import net.solarnetwork.central.din.security.SecurityEndpointCredential;
import net.solarnetwork.central.din.security.SecurityUtils;
import net.solarnetwork.central.web.MaxUploadSizeInputStream;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.util.ObjectUtils;

/**
 * Datum input controller.
 *
 * @author matt
 * @version 1.2
 */
@RestController("v1DatumInputController")
@RequestMapping("/api/v1/endpoint/{endpointId}")
public class DatumInputController {

	private final DatumInputEndpointBiz inputBiz;
	private final long maxDatumInputLength;

	/**
	 * Constructor.
	 *
	 * @param inputBiz
	 *        the input service
	 * @param maxDatumInputLength
	 *        the maximum datum input length
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumInputController(DatumInputEndpointBiz inputBiz,
			@Value("${app.din.max-datum-input-length}") long maxDatumInputLength) {
		super();
		this.inputBiz = ObjectUtils.requireNonNullArgument(inputBiz, "inputBiz");
		this.maxDatumInputLength = maxDatumInputLength;
	}

	/**
	 * Post one or more datum.
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
	@RequestMapping(value = "/datum", method = RequestMethod.POST, consumes = {
			MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE })
	public Result<Collection<DatumId>> postDatum(@PathVariable("endpointId") UUID endpointId,
			@RequestHeader(value = "Content-Type", required = true) String contentType,
			@RequestHeader(value = "Content-Encoding", required = false) String encoding, WebRequest req,
			InputStream in) throws IOException {
		final SecurityEndpointCredential actor = SecurityUtils.getCurrentEndpointCredential();

		final MediaType mediaType = MediaType.parseMediaType(contentType);

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

		// limit input size
		input = new MaxUploadSizeInputStream(input, maxDatumInputLength);
		var result = inputBiz.importDatum(actor.getUserId(), endpointId, mediaType, input, params);

		return success(result);
	}

}
