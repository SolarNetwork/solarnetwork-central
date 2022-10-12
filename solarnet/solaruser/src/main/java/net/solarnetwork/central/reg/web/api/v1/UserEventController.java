/* ==================================================================
 * UserEventController.java - 6/08/2022 9:12:41 am
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

package net.solarnetwork.central.reg.web.api.v1;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.UserEventBiz;
import net.solarnetwork.central.common.dao.BasicUserEventFilter;
import net.solarnetwork.central.common.dao.UserEventFilter;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.reg.config.JsonConfig;
import net.solarnetwork.central.reg.config.UserEventConfig;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.support.ObjectMapperFilteredResultsProcessor;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.central.web.GlobalExceptionRestController;

/**
 * Web service API for user event management.
 * 
 * @author matt
 * @version 1.0
 */
@GlobalExceptionRestController
@RestController("v1UserEventsController")
@RequestMapping(value = { "/api/v1/sec/user/events" })
public class UserEventController {

	private final ObjectMapper objectMapper;
	private final ObjectMapper cborObjectMapper;
	private final UserEventBiz userEventBiz;
	private Validator filterValidator;

	/**
	 * Constructor.
	 * 
	 * @param userEventBiz
	 *        the UserEventBiz to use
	 * @param objectMapper
	 *        the object mapper to use for JSON
	 * @param cborObjectMapper
	 *        the mapper to use for CBOR
	 */
	@Autowired
	public UserEventController(UserEventBiz userEventBiz, ObjectMapper objectMapper,
			@Qualifier(JsonConfig.CBOR_MAPPER) ObjectMapper cborObjectMapper) {
		super();
		this.userEventBiz = requireNonNullArgument(userEventBiz, "userEventBiz");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.cborObjectMapper = requireNonNullArgument(cborObjectMapper, "cborObjectMapper");
	}

	private FilteredResultsProcessor<UserEvent> processorForType(final List<MediaType> acceptTypes,
			final HttpServletResponse response) throws IOException {
		FilteredResultsProcessor<UserEvent> processor = null;
		for ( MediaType acceptType : acceptTypes ) {
			if ( MediaType.APPLICATION_CBOR.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperFilteredResultsProcessor<>(
						cborObjectMapper.createGenerator(response.getOutputStream()),
						cborObjectMapper.getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_CBOR_VALUE),
						UserEventSerializer.INSTANCE);
				break;
			} else if ( MediaType.APPLICATION_JSON.isCompatibleWith(acceptType) ) {
				processor = new ObjectMapperFilteredResultsProcessor<>(
						objectMapper.createGenerator(response.getOutputStream()),
						objectMapper.getSerializerProvider(),
						MimeType.valueOf(MediaType.APPLICATION_JSON_VALUE),
						UserEventSerializer.INSTANCE);
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
	@RequestMapping(value = "", method = RequestMethod.GET)
	public void listUserEvents(final BasicUserEventFilter cmd,
			@RequestHeader(HttpHeaders.ACCEPT) final String accept, final HttpServletResponse response,
			BindingResult validationResult) throws IOException {
		if ( filterValidator != null ) {
			filterValidator.validate(cmd, validationResult);
			if ( validationResult.hasErrors() ) {
				throw new ValidationException(validationResult);
			}
		}
		cmd.setUserId(SecurityUtils.getCurrentActorUserId()); // force to actor
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (FilteredResultsProcessor<UserEvent> processor = processorForType(acceptTypes, response)) {
			userEventBiz.findFilteredUserEvents(cmd, processor);
		}
	}

	/**
	 * Get the filter validator to use.
	 * 
	 * @return the validator
	 */
	public Validator getFilterValidator() {
		return filterValidator;
	}

	/**
	 * Set the filter validator to use.
	 * 
	 * @param filterValidator
	 *        the validator to set
	 * @throws IllegalArgumentException
	 *         if {@code validator} does not support the {@link UserEventFilter}
	 *         class
	 */
	@Autowired
	@Qualifier(UserEventConfig.USER_EVENT)
	public void setFilterValidator(Validator filterValidator) {
		if ( filterValidator != null && !filterValidator.supports(UserEventFilter.class) ) {
			throw new IllegalArgumentException("The Validator must support the UserEventFilter class.");
		}
		this.filterValidator = filterValidator;
	}

}
