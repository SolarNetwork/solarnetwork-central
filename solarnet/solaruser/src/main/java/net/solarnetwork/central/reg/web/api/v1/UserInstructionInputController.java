/* ==================================================================
 * UserInstructionInputController.java - 30/03/2024 7:38:00 am
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration;
import net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * Web service API for Instruction Input (ININ) management.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT)
@GlobalExceptionRestController
@RestController("v1InstructionInputController")
@RequestMapping(value = { "/api/v1/sec/user/inin", "/u/sec/inin" })
public class UserInstructionInputController {

	private final UserInstructionInputBiz userInstructionInputBiz;
	private final long maxInstructionInputLength;

	/**
	 * Constructor.
	 *
	 * @param userInstructionInputBiz
	 *        the service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserInstructionInputController(UserInstructionInputBiz userInstructionInputBiz,
			@Value("${app.inin.max-input-length}") long maxInstructionInputLength) {
		super();
		this.userInstructionInputBiz = requireNonNullArgument(userInstructionInputBiz,
				"userInstructionInputBiz");
		this.maxInstructionInputLength = maxInstructionInputLength;
	}

	/**
	 * List the available request transform services.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/request-transform", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableRequestTransformServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = userInstructionInputBiz
				.availableRequestTransformServices(locale);
		return success(result);
	}

	/**
	 * List the available response transform services.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/response-transform", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableResponseTransformServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = userInstructionInputBiz
				.availableResponseTransformServices(locale);
		return success(result);
	}

}
