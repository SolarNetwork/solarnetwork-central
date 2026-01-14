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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.support.CsvFilteredResultsProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.support.ObjectMapperFilteredResultsProcessor;
import net.solarnetwork.central.support.OutputSerializationSupportContext;
import net.solarnetwork.util.StringUtils;

/**
 * Helper utilities for web APIs.
 *
 * @author matt
 * @version 2.0
 */
public final class WebUtils {

	/**
	 * The {@literal text/csv} media type value.
	 *
	 * @since 1.2
	 */
	public static final String TEXT_CSV_MEDIA_TYPE_VALUE = "text/csv";

	/** The {@literal text/csv} media type. */
	public static final MediaType TEXT_CSV_MEDIA_TYPE = MediaType
			.parseMediaType(TEXT_CSV_MEDIA_TYPE_VALUE);

	/**
	 * The {@literal text/csv} media type with a UTF-8 character set value.
	 */
	public static final String TEXT_CSV_UTF8_MEDIA_TYPE_VALUE = "text/csv; charset=UTF-8";

	/** The {@literal text/csv} media type with a UTF-8 character set. */
	public static final MediaType TEXT_CSV_UTF8_MEDIA_TYPE = MediaType
			.parseMediaType(TEXT_CSV_UTF8_MEDIA_TYPE_VALUE);

	/**
	 * The media type value for Microsoft XLSX.
	 *
	 * @since 1.2
	 */
	public static final String XLSX_MEDIA_TYPE_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	/**
	 * The media type for Microsoft XLSX.
	 *
	 * @since 1.2
	 */
	public static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(XLSX_MEDIA_TYPE_VALUE);

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
	 * Set up a filtered results processor.
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
	 * <p>
	 * If {@code acceptTypes} is empty, then {@code application/json} will be
	 * assumed.
	 * </p>
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
		for ( MediaType acceptType : acceptTypes != null && !acceptTypes.isEmpty() ? acceptTypes
				: List.of(MediaType.APPLICATION_JSON) ) {
			if ( MediaType.APPLICATION_CBOR.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperFilteredResultsProcessor<>(
						context.cborObjectMapper().createGenerator(response.getOutputStream()),
						context.cborObjectMapper()._serializationContext(), // FIXME use "allowed" method
						MimeType.valueOf(MediaType.APPLICATION_CBOR_VALUE), context.jsonSerializer());
				break;
			} else if ( MediaType.APPLICATION_JSON.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperFilteredResultsProcessor<>(
						context.jsonObjectMapper().createGenerator(response.getOutputStream()),
						context.jsonObjectMapper()._serializationContext(), // FIXME use "allowed" method
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
			}
		}
		if ( processor == null ) {
			throw new IllegalArgumentException(format("No supported media type within [%s]",
					StringUtils.commaDelimitedStringFromCollection(acceptTypes)));

		}
		response.setContentType(processor.getMimeType().toString());
		return processor;
	}

	/**
	 * Get the request URI including query parameters as a string.
	 *
	 * @param request
	 *        the servlet request
	 * @return the request URI with query parameters included
	 * @since 1.3
	 */
	public static String requestUriWithQueryParameters(final HttpServletRequest request) {
		String url = request.getRequestURI();
		String q = request.getQueryString();
		if ( q != null ) {
			return url + "?" + q;
		}
		return url;
	}

	/**
	 * Handle a {@link DataAccessException} by either logging a WARN log message
	 * if {@code retries} is greater than 0, or re-throwing the exception
	 * otherwise.
	 *
	 * @param req
	 *        the HTTP request
	 * @param e
	 *        the exception
	 * @param retries
	 *        the number of retry attempts remaining
	 * @param retryDelay
	 *        a delay in milliseconds to sleep for, if {@code retries} is
	 *        greater than 0
	 * @param log
	 *        the logger to log a WARN message to
	 * @since 1.3
	 */
	public static void handleTransientDataAccessExceptionRetry(final HttpServletRequest req,
			final DataAccessException e, final int retries, final long retryDelay, final Logger log) {
		if ( retries > 0 ) {
			log.warn(
					"Transient {} exception in request {}, will retry up to {} more times after a delay of {}ms: {}",
					e.getClass().getSimpleName(), requestUriWithQueryParameters(req), retries,
					retryDelay, e.toString());
			if ( retryDelay > 0 ) {
				try {
					Thread.sleep(retryDelay);
				} catch ( InterruptedException e2 ) {
					// ignore
				}
			}
		} else {
			throw e;
		}
	}

	/**
	 * Perform an action with {@link DataAccessException} retry.
	 *
	 * @param <T>
	 *        the action argument type
	 * @param action
	 *        the action to perform
	 * @param req
	 *        the HTTP request
	 * @param tries
	 *        the number of attempts to try
	 * @param retryDelay
	 *        a delay in milliseconds to sleep for, if {@code retries} is
	 *        greater than 0
	 * @param log
	 *        the logger to log a WARN message to
	 * @return the action result
	 */
	public static <T> T doWithTransientDataAccessExceptionRetry(final Supplier<T> action,
			final HttpServletRequest req, int tries, final long retryDelay, final Logger log) {
		while ( true ) {
			try {
				return action.get();
			} catch ( TransientDataAccessException | DataAccessResourceFailureException
					| UncategorizedSQLException e ) {
				handleTransientDataAccessExceptionRetry(req, e, --tries, retryDelay, log);
			}
		}
	}

	/**
	 * Create an input stream that throws a
	 * {@link MaxUploadSizeExceededException} when the given length is exceeded
	 * while reading.
	 *
	 * @param in
	 *        the input stream
	 * @param maxLength
	 *        the maximum length to allow reading before a
	 *        {@link MaxUploadSizeExceededException} is thrown
	 * @return the new input stream
	 * @since 1.4
	 */
	public static InputStream maxUploadSizeExceededInputStream(InputStream in, long maxLength)
			throws IOException {
		return BoundedInputStream.builder().setInputStream(in).setMaxCount(maxLength)
				.setOnMaxCount((_, _) -> {
					throw new MaxUploadSizeExceededException(maxLength);
				}).get();
	}

}
