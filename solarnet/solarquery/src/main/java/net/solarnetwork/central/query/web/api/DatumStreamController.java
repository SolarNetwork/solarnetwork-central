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
import java.time.Period;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.datum.v2.support.ObjectMapperStreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.config.JsonConfig;
import net.solarnetwork.central.web.GlobalExceptionRestController;

/**
 * Controller for querying datum stream related data.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1DatumStreamController")
@RequestMapping("/api/v1/sec/datum/stream")
@GlobalExceptionRestController
public class DatumStreamController {

	private final ObjectMapper objectMapper;
	private final ObjectMapper cborObjectMapper;
	private final QueryBiz queryBiz;
	private SmartValidator filterValidator;

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

	private StreamDatumFilteredResultsProcessor processorForType(final List<MediaType> acceptTypes,
			final HttpServletResponse response) throws IOException {
		StreamDatumFilteredResultsProcessor processor = null;
		for ( MediaType acceptType : acceptTypes ) {
			if ( MediaType.APPLICATION_CBOR.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperStreamDatumFilteredResultsProcessor(
						cborObjectMapper.createGenerator(response.getOutputStream()),
						cborObjectMapper.getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_CBOR_VALUE));
				break;
			} else if ( MediaType.APPLICATION_JSON.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperStreamDatumFilteredResultsProcessor(
						objectMapper.createGenerator(response.getOutputStream()),
						objectMapper.getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_JSON_VALUE));
				break;
			} else {
				throw new IllegalArgumentException(
						format("The [%s] media type is not supported.", acceptType));
			}
		}
		response.setContentType(processor.getMimeType().toString());
		return processor;
	}

	/**
	 * Query for a listing of datum.
	 * 
	 * @param cmd
	 *        the query criteria
	 * @param accept
	 *        the HTTP accept header value
	 * @param response
	 *        the HTTP response
	 */
	@ResponseBody
	@RequestMapping(value = "/datum", method = RequestMethod.GET)
	public void listDatum(final StreamDatumFilterCommand cmd,
			@RequestHeader(HttpHeaders.ACCEPT) final String accept, final HttpServletResponse response,
			BindingResult validationResult) throws IOException {
		if ( filterValidator != null ) {
			filterValidator.validate(cmd, validationResult);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (StreamDatumFilteredResultsProcessor processor = processorForType(acceptTypes, response)) {
			queryBiz.findFilteredStreamDatum(cmd, processor, cmd.getSortDescriptors(), cmd.getOffset(),
					cmd.getMax());
		}
	}

	/**
	 * Query for a reading datum.
	 * 
	 * @param cmd
	 *        the query criteria
	 * @param accept
	 *        the HTTP accept header value
	 * @param response
	 *        the HTTP response
	 */
	@ResponseBody
	@RequestMapping(value = "/reading", method = RequestMethod.GET)
	public void listReadings(final StreamDatumFilterCommand cmd,
			final @RequestParam("readingType") DatumReadingType readingType,
			@RequestParam(value = "tolerance", required = false, defaultValue = "P1M") final Period tolerance,
			@RequestHeader(HttpHeaders.ACCEPT) final String accept, final HttpServletResponse response,
			BindingResult validationResult) throws IOException {
		if ( filterValidator != null ) {
			filterValidator.validate(cmd, validationResult, readingType, tolerance);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (StreamDatumFilteredResultsProcessor processor = processorForType(acceptTypes, response)) {
			queryBiz.findFilteredStreamReadings(cmd, readingType, tolerance, processor,
					cmd.getSortDescriptors(), cmd.getOffset(), cmd.getMax());
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
	 *        the valiadtor to set
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

}
