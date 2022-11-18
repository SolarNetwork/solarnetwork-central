/* ==================================================================
 * WebUtils.java - 15/08/2022 3:24:54 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web;

import static java.lang.String.format;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.support.CsvFilteredResultsProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.support.ObjectMapperFilteredResultsProcessor;
import net.solarnetwork.central.support.OutputSerializationSupportContext;

/**
 * Helper utilities for web APIs.
 * 
 * @author matt
 * @version 1.1
 */
public final class WebUtils {

	/** The {@literal text/csv} media type. */
	public static final MediaType TEXT_CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");

	/** The {@literal text/csv} media type with a UTF-8 character set. */
	public static final MediaType TEXT_CSV_UTF8_MEDIA_TYPE = MediaType
			.parseMediaType("text/csv; charset=UTF-8");

	private WebUtils() {
		// not allowed
	}

	/**
	 * Build a {@link UriComponents} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariableValues
	 *        the optional URI variable values
	 * @return the URI components
	 */
	public static UriComponents withoutHost(UriComponentsBuilder builder, Object... uriVariableValues) {
		return builder.scheme(null).host(null).port(null).buildAndExpand(uriVariableValues);
	}

	/**
	 * Build a {@link UriComponents} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariables
	 *        the optional URI variables
	 * @return the URI components
	 */
	public static UriComponents withoutHost(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		return builder.scheme(null).host(null).port(null).buildAndExpand(uriVariables);
	}

	/**
	 * Build a {@link URI} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariableValues
	 *        the optional URI variable values
	 * @return the URI
	 */
	public static URI uriWithoutHost(UriComponentsBuilder builder, Object... uriVariableValues) {
		return withoutHost(builder, uriVariableValues).toUri();
	}

	/**
	 * Build a {@link URI} without any scheme, host, or port.
	 * 
	 * @param builder
	 *        the builder
	 * @param uriVariables
	 *        the optional URI variables
	 * @return the URI
	 */
	public static URI uriWithoutHost(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		return withoutHost(builder, uriVariables).toUri();
	}

	/**
	 * Setup a filtered results processor.
	 * 
	 * <p>
	 * The following types are supported:
	 * </p>
	 * 
	 * <ul>
	 * <li>application/json</li>
	 * <li>application/cbor</li>
	 * <li>text/csv</li>
	 * </ul>
	 * 
	 * @param <T>
	 *        the result type
	 * @param acceptTypes
	 *        the acceptable types
	 * @param response
	 *        the HTTP response
	 * @param context
	 *        the output context
	 * @return the processor
	 * @throws IOException
	 *         if an IO error occurs
	 * @since 1.1
	 */
	public static <T> FilteredResultsProcessor<T> filteredResultsProcessorForType(
			final List<MediaType> acceptTypes, final HttpServletResponse response,
			OutputSerializationSupportContext<T> context) throws IOException {
		FilteredResultsProcessor<T> processor = null;
		for ( MediaType acceptType : acceptTypes ) {
			if ( MediaType.APPLICATION_CBOR.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperFilteredResultsProcessor<>(
						context.cborObjectMapper().createGenerator(response.getOutputStream()),
						context.cborObjectMapper().getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_CBOR_VALUE), context.jsonSerializer());
				break;
			} else if ( MediaType.APPLICATION_JSON.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperFilteredResultsProcessor<>(
						context.jsonObjectMapper().createGenerator(response.getOutputStream()),
						context.jsonObjectMapper().getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_JSON_VALUE), context.jsonSerializer());
				break;
			} else if ( TEXT_CSV_MEDIA_TYPE.isCompatibleWith(acceptType) ) {
				Charset cs = acceptType.getCharset();
				if ( cs == null ) {
					cs = StandardCharsets.UTF_8;
				}
				processor = new CsvFilteredResultsProcessor<>(
						new OutputStreamWriter(response.getOutputStream(), cs), TEXT_CSV_MEDIA_TYPE,
						true, context.registrar());
				break;
			} else {
				throw new IllegalArgumentException(
						format("The [%s] media type is not supported.", acceptType));
			}
		}
		response.setContentType(processor.getMimeType().toString());
		return processor;
	}

}
