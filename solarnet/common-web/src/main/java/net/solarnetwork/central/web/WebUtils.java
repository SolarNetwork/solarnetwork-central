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
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.input.BoundedInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.util.MimeType;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.support.CsvFilteredResultsProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.support.ObjectMapperFilteredResultsProcessor;
import net.solarnetwork.central.support.OutputSerializationSupportContext;
import net.solarnetwork.security.AbstractAuthorizationBuilder;
import net.solarnetwork.util.StringUtils;

/**
 * Helper utilities for web APIs.
 *
 * @author matt
 * @version 2.1
 */
public final class WebUtils {

	/**
	 * A "global" web logger, for library-level logging.
	 *
	 * @since 2.1
	 */
	public static final Logger GLOBAL_WEB_LOG = LoggerFactory
			.getLogger("net.solarnetwork.central.web.GLOBAL");

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

	/**
	 * A value to use for anonymous users in log messages.
	 *
	 * @since 2.1
	 */
	public static final String ANONYMOUS_USER_PRINCIPAL = "anonymous";

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
	public static UriComponents withoutHost(UriComponentsBuilder builder,
			Object @Nullable... uriVariableValues) {
		final UriComponentsBuilder b = builder.scheme(null).host(null).port(null);
		if ( uriVariableValues != null ) {
			return b.buildAndExpand(uriVariableValues);
		}
		return b.build();
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
	public static UriComponents withoutHost(UriComponentsBuilder builder,
			@Nullable Map<String, ?> uriVariables) {
		final UriComponentsBuilder b = builder.scheme(null).host(null).port(null);
		if ( uriVariables != null ) {
			return b.buildAndExpand(uriVariables);
		}
		return b.build();
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
	public static URI uriWithoutHost(UriComponentsBuilder builder,
			Object @Nullable... uriVariableValues) {
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
	public static URI uriWithoutHost(UriComponentsBuilder builder,
			@Nullable Map<String, ?> uriVariables) {
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

	/**
	 * Test if an exception was caused by an HTTP response that can no longer be
	 * written to, typically because the client disconnected.
	 *
	 * <p>
	 * The servlet container throws a {@link ClientAbortException} when writing
	 * to a client that has disconnected. Spring wraps the response passed to
	 * handler methods, which throws an {@link AsyncRequestNotUsableException}
	 * when writing fails, or when writing after a previous failure.
	 * </p>
	 *
	 * @param e
	 *        the exception to test
	 * @return {@code true} if {@code e} or any of its causes is a
	 *         {@link ClientAbortException} or
	 *         {@link AsyncRequestNotUsableException}
	 * @since 2.1
	 */
	public static boolean isClientAbortException(@Nullable Throwable e) {
		for ( Throwable t = e; t != null; t = t.getCause() ) {
			if ( t instanceof ClientAbortException || t instanceof AsyncRequestNotUsableException ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get a standardized string description of a request.
	 *
	 * @param request
	 *        the request
	 * @return the description
	 * @since 2.1
	 */
	@SuppressWarnings("RedundantControlFlow")
	public static String requestDescription(WebRequest request) {
		StringBuilder buf = new StringBuilder(request.getDescription(false));
		Map<String, String[]> params = request.getParameterMap();
		if ( !params.isEmpty() ) {
			buf.append("?");
			boolean next = false;
			for ( Entry<String, String[]> e : params.entrySet() ) {
				if ( next ) {
					buf.append('&');
				} else {
					next = true;
				}
				buf.append(e.getKey()).append("=");
				String[] vals = e.getValue();
				if ( vals == null || vals.length < 1 ) {
					continue;
				} else if ( vals.length == 1 ) {
					buf.append(vals[0]);
				} else {
					for ( int i = 0, len = vals.length; i < len; i++ ) {
						if ( i > 0 ) {
							buf.append(",");
						}
						buf.append(vals[i]);
					}
				}
			}
		}
		return buf.toString();
	}

	/**
	 * Get the user principal name of a given request.
	 *
	 * @param request
	 *        the request
	 * @return the name, or {@link #ANONYMOUS_USER_PRINCIPAL}
	 * @since 2.1
	 */
	public static String userPrincipalName(WebRequest request) {
		Principal userPrincipal = request.getUserPrincipal();
		if ( userPrincipal != null ) {
			return userPrincipal.getName();
		}
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if ( authHeader != null ) {
			int idx = authHeader.indexOf(' ');
			if ( idx > 0 && idx + 1 < authHeader.length() ) {
				String data = authHeader.substring(idx + 1);
				Map<String, String> dataMap = StringUtils.commaDelimitedStringToMap(data);
				String name = (dataMap != null
						? dataMap.get(AbstractAuthorizationBuilder.AUTHORIZATION_COMPONENT_CREDENTIAL)
						: null);
				if ( name != null ) {
					return name;
				}
			}
		}
		return ANONYMOUS_USER_PRINCIPAL;
	}

	/**
	 * Throw an exception unless the HTTP response has already been committed.
	 *
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param response
	 *        the response
	 * @since 2.1
	 */
	public static void throwUnlessCommitted(final RuntimeException e, WebRequest request,
			final HttpServletResponse response) {
		if ( !response.isCommitted() ) {
			throw e;
		}
		final var level = isClientAbortException(e) ? Level.DEBUG : Level.ERROR;
		if ( GLOBAL_WEB_LOG.isEnabledForLevel(level) ) {
			GLOBAL_WEB_LOG.atLevel(level).log(
					"{} in request {}; user [{}]; response committed so error can not be passed to client: {}",
					e.getClass().getSimpleName(), requestDescription(request),
					userPrincipalName(request), e.toString(), e);
		}
	}

}
