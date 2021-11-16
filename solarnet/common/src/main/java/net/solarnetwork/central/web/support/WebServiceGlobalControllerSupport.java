/* ==================================================================
 * WebServiceGlobalControllerSupport.java - 16/11/2021 10:48:48 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web.support;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.web.domain.Response;

/**
 * Global REST controller support.
 * 
 * @author matt
 * @version 1.0
 */
@RestControllerAdvice
public class WebServiceGlobalControllerSupport {

	/** A class-level logger. */
	private static final Logger log = LoggerFactory.getLogger(WebServiceGlobalControllerSupport.class);

	@Autowired
	private MessageSource messageSource;

	@Value("${spring.servlet.multipart.max-file-size:1MB}")
	private DataSize maxUploadSize = DataSize.ofMegabytes(1);

	/**
	 * Handle a {@link MaxUploadSizeExceededException}.
	 * 
	 * @param e
	 *        the exception
	 * @param request
	 *        the request
	 * @param locale
	 *        the locale
	 * @return the response
	 * @since 2.0
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
			WebRequest request, Locale locale) {
		log.warn("MaxUploadSizeExceededException for {}", request.getDescription(true));
		String msg = "Upload size exceeded";
		String maxSize = NumberUtils.humanReadableCount(
				e.getMaxUploadSize() > -1 ? e.getMaxUploadSize() : maxUploadSize.toBytes());
		if ( messageSource != null ) {
			msg = messageSource.getMessage("error.web.upload-size-exceeded", new Object[] { maxSize },
					msg, locale);
		}
		return new Response<Object>(Boolean.FALSE, "WEB.00100", msg, null);
	}

}
