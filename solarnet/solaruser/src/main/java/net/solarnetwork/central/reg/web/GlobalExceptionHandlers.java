/* ==================================================================
 * GlobalExceptionHandlers.java - 15/11/2018 9:51:42 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * App-wide global exception handlers.
 * 
 * @author matt
 * @version 2.0
 */
@RestControllerAdvice(annotations = GlobalExceptionRestController.class)
public class GlobalExceptionHandlers {

	/**
	 * Handle an {@link MaxUploadSizeExceededException}.
	 * 
	 * <p>
	 * This needs to be globally defined because it occurs before the request is
	 * associated with any controller.
	 * </p>
	 * 
	 * @param e
	 *        the exception
	 * @return an error response object
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseBody
	@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
	public Response<?> handle(MaxUploadSizeExceededException e) {
		Throwable cause = e;
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		return new Response<Object>(Boolean.FALSE, "DI.00401", cause.getMessage(), null);
	}

}
