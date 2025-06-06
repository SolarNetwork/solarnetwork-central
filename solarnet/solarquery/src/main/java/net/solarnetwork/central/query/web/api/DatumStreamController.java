/* ==================================================================
 * DatumStreamController.java - 29/04/2022 10:44:01 AM
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

package net.solarnetwork.central.query.web.api;

import static java.lang.String.format;
import static net.solarnetwork.central.query.config.DatumQueryBizConfig.STREAM_DATUM_FILTER;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.datum.v2.support.CsvStreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.datum.v2.support.ObjectMapperStreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.config.JsonConfig;
import net.solarnetwork.central.query.domain.StreamDatumResult;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.io.ProvidedOutputStream;

/**
 * Controller for querying datum stream related data.
 *
 * @author matt
 * @version 1.4
 */
@Controller("v1DatumStreamController")
@RequestMapping({ "/api/v1/pub/datum/stream", "/api/v1/sec/datum/stream" })
@Tag(name = "datum-stream", description = """
		Methods to query datum streams with streaming result. These methods are similar to
		the `/datum` API methods, but stream the results directly to the client, and offer
		unlimited results for most criteria.
		""")
@GlobalExceptionRestController
public class DatumStreamController {

	private final ObjectMapper objectMapper;
	private final ObjectMapper cborObjectMapper;
	private final QueryBiz queryBiz;
	private SmartValidator filterValidator;
	private Duration mostRecentStartPeriod = DatumController.DEFAULT_MOST_RECENT_START_PERIOD;

	/**
	 * Constructor.
	 *
	 * @param queryBiz
	 *        the QueryBiz to use
	 * @param objectMapper
	 *        the object mapper to use for JSON
	 * @param cborObjectMapper
	 *        the mapper to use for CBOR
	 */
	@Autowired
	public DatumStreamController(QueryBiz queryBiz, ObjectMapper objectMapper,
			@Qualifier(JsonConfig.CBOR_MAPPER) ObjectMapper cborObjectMapper) {
		super();
		this.queryBiz = requireNonNullArgument(queryBiz, "queryBiz");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.cborObjectMapper = requireNonNullArgument(cborObjectMapper, "cborObjectMapper");
	}

	private static final Pattern GZIP_ENCODING = Pattern.compile("\\bgzip\\b", Pattern.CASE_INSENSITIVE);

	private static OutputStream responseOutputStream(HttpServletResponse response,
			String acceptEncoding) {
		return new ProvidedOutputStream(() -> {
			try {
				OutputStream out = response.getOutputStream();
				if ( acceptEncoding != null && GZIP_ENCODING.matcher(acceptEncoding).find() ) {
					out = new GZIPOutputStream(out);
					response.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
				}
				return out;
			} catch ( IOException e ) {
				throw new IllegalStateException(e);
			}
		});
	}

	private static Writer responseWriter(HttpServletResponse response, String acceptEncoding) {
		return new OutputStreamWriter(responseOutputStream(response, acceptEncoding),
				StandardCharsets.UTF_8);
	}

	private StreamDatumFilteredResultsProcessor processorForType(final List<MediaType> acceptTypes,
			final String acceptEncoding, final HttpServletResponse response) throws IOException {
		StreamDatumFilteredResultsProcessor processor = null;
		for ( MediaType acceptType : acceptTypes ) {
			if ( MediaType.APPLICATION_CBOR.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperStreamDatumFilteredResultsProcessor(
						cborObjectMapper.createGenerator(responseOutputStream(response, acceptEncoding)),
						cborObjectMapper.getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_CBOR_VALUE));
				break;
			} else if ( MediaType.APPLICATION_JSON.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperStreamDatumFilteredResultsProcessor(
						objectMapper.createGenerator(responseOutputStream(response, acceptEncoding)),
						objectMapper.getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_JSON_VALUE));
				break;
			} else if ( CsvStreamDatumFilteredResultsProcessor.TEXT_CSV_MIME_TYPE
					.isCompatibleWith(acceptType) ) {
				processor = new CsvStreamDatumFilteredResultsProcessor(
						responseWriter(response, acceptEncoding));
				break;
			} else {
				throw new IllegalArgumentException(
						format("The [%s] media type is not supported.", acceptType));
			}
		}
		response.setContentType(processor.getMimeType().toString());
		return processor;
	}

	@SuppressWarnings("JavaDurationGetSecondsToToSeconds")
	private void populateMostRecentImplicitStartDate(final StreamDatumFilterCommand cmd) {
		if ( mostRecentStartPeriod != null && cmd.isMostRecent() && cmd.getStartDate() == null
				&& cmd.getLocalStartDate() == null ) {
			// add implicit start date, to speed up query
			cmd.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS)
					.minusSeconds(mostRecentStartPeriod.getSeconds()));
		}
	}

	/**
	 * Query for a listing of datum.
	 *
	 * @param criteria
	 *        the query criteria
	 * @param accept
	 *        the HTTP accept header value
	 * @param acceptEncoding
	 *        the HTTP accept-encoding header value
	 * @param response
	 *        the HTTP response
	 */
	// @formatter:off
	@Operation(operationId = "datumStreamList",
			summary = "List node stream datum matching filter criteria",
			description = """
					Query for node stream datum that match criteria like node ID, source ID, and date range.""",
			parameters = @Parameter(name = "criteria", description = """
					The search criteria. A maximum result count will be enforced.
					""", schema = @Schema(implementation = StreamDatumFilter.class),
					style = ParameterStyle.FORM, explode = Explode.TRUE))
	@ApiResponse(description = "Streaming list of stream datum", responseCode = "200",
			content = @Content(schema = @Schema(implementation = StreamDatumResult.class)))
	// @formatter:on
	@ResponseBody
	@RequestMapping(value = "/datum", method = RequestMethod.GET)
	public void listDatum(final StreamDatumFilterCommand criteria,
			@RequestHeader(HttpHeaders.ACCEPT) final String accept,
			@RequestHeader(name = HttpHeaders.ACCEPT_ENCODING,
					required = false) final String acceptEncoding,
			final HttpServletResponse response, BindingResult validationResult) throws IOException {
		if ( filterValidator != null ) {
			filterValidator.validate(criteria, validationResult);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		populateMostRecentImplicitStartDate(criteria);
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (StreamDatumFilteredResultsProcessor processor = processorForType(acceptTypes,
				acceptEncoding, response)) {
			queryBiz.findFilteredStreamDatum(criteria, processor, criteria.getSortDescriptors(),
					criteria.getOffset(), criteria.getMax());
		}
	}

	/**
	 * Query for a reading datum.
	 *
	 * @param criteria
	 *        the query criteria
	 * @param readingType
	 *        the reading type
	 * @param tolerance
	 *        the tolerance
	 * @param accept
	 *        the HTTP accept header value
	 * @param acceptEncoding
	 *        the HTTP accept-encoding header value
	 * @param response
	 *        the HTTP response
	 */
	// @formatter:off
	@Operation(operationId = "readingDatumStreamList",
			summary = "List node reading style stream datum matching filter criteria",
			description = """
					Query for node stream datum that match criteria like node ID, source ID, and date range,
					returning reading style results.
					""",
			parameters = { 
					@Parameter(name = "criteria", description = """
						The search criteria. A maximum result count will be enforced.
						""", schema = @Schema(
								implementation = AggregateGeneralNodeDatumFilter.class),
								style = ParameterStyle.FORM, explode = Explode.TRUE),
					@Parameter(name = "readingType", description = "The desired reading type."),
					@Parameter(name = "tolerance", description = "The reading tolerance, used by some reading types."),
			}
	)
	@ApiResponse(description = "Streaming list of stream datum",
			responseCode = "200",
			content = @Content( schema = @Schema(implementation = StreamDatumResult.class)))
	// @formatter:on
	@ResponseBody
	@RequestMapping(value = "/reading", method = RequestMethod.GET)
	public void listReadings(final StreamDatumFilterCommand criteria,
			final @RequestParam("readingType") DatumReadingType readingType,
			@RequestParam(value = "tolerance", required = false,
					defaultValue = "P1M") final Period tolerance,
			@RequestHeader(HttpHeaders.ACCEPT) final String accept,
			@RequestHeader(name = HttpHeaders.ACCEPT_ENCODING,
					required = false) final String acceptEncoding,

			final HttpServletResponse response, BindingResult validationResult) throws IOException {
		if ( filterValidator != null ) {
			filterValidator.validate(criteria, validationResult, readingType, tolerance);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (StreamDatumFilteredResultsProcessor processor = processorForType(acceptTypes,
				acceptEncoding, response)) {
			queryBiz.findFilteredStreamReadings(criteria, readingType, tolerance, processor,
					criteria.getSortDescriptors(), criteria.getOffset(), criteria.getMax());
		}
	}

	/**
	 * Get the filter validator to use.
	 *
	 * @return the validator
	 */
	public SmartValidator getFilterValidator() {
		return filterValidator;
	}

	/**
	 * Set the filter validator to use.
	 *
	 * @param filterValidator
	 *        the validator to set
	 * @throws IllegalArgumentException
	 *         if {@code validator} does not support the
	 *         {@link StreamDatumFilter} class
	 */
	@Autowired
	@Qualifier(STREAM_DATUM_FILTER)
	public void setFilterValidator(SmartValidator filterValidator) {
		if ( filterValidator != null && !filterValidator.supports(StreamDatumFilter.class) ) {
			throw new IllegalArgumentException(
					"The Validator must support the StreamDatumFilter class.");
		}
		this.filterValidator = filterValidator;
	}

	/**
	 * Get the length of time to use to determine an implicit start date in most
	 * recent queries.
	 *
	 * @return the mostRecentStartPeriod the duration
	 * @since 1.3
	 */
	public Duration getMostRecentStartPeriod() {
		return mostRecentStartPeriod;
	}

	/**
	 * Set the length of time to use to determine an implicit start date in most
	 * recent queries.
	 *
	 * @param mostRecentStartPeriod
	 *        the period to set
	 * @since 1.3
	 */
	public void setMostRecentStartPeriod(Duration mostRecentStartPeriod) {
		this.mostRecentStartPeriod = mostRecentStartPeriod;
	}

}
