/* ==================================================================
 * UserDatumInputController.java - 25/02/2024 7:06:49 am
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
import java.util.Collection;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.din.config.SolarNetDatumInputConfiguration;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.domain.DatumInputConfigurationEntity;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.din.biz.UserDatumInputBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.Result;

/**
 * Web service API for DNP3 management.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetDatumInputConfiguration.DATUM_INPUT)
@GlobalExceptionRestController
@RestController("v1DatumInputController")
@RequestMapping(value = { "/api/v1/sec/user/din", "/u/sec/din" })
public class UserDatumInputController {

	private final UserDatumInputBiz userDatumInputBiz;

	/**
	 * Constructor.
	 *
	 * @param userDatumInputBiz
	 *        the service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserDatumInputController(UserDatumInputBiz userDatumInputBiz) {
		super();
		this.userDatumInputBiz = requireNonNullArgument(userDatumInputBiz, "userDatumInputBiz");
	}

	/**
	 * List the available transform services.
	 *
	 * @param locale
	 *        the desired locale
	 * @return the services
	 */
	@RequestMapping(value = "/services/transform", method = RequestMethod.GET)
	public Result<Iterable<LocalizedServiceInfo>> availableTransformServices(Locale locale) {
		Iterable<LocalizedServiceInfo> result = null;
		if ( userDatumInputBiz != null ) {
			result = userDatumInputBiz.availableTransformServices(locale);
		}
		return success(result);
	}

	private <C extends DatumInputConfigurationEntity<?, ?>> Result<Collection<C>> listConfigurationsForCurrentUser(
			Class<C> clazz) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		Collection<C> result = null;
		if ( userDatumInputBiz != null ) {
			result = userDatumInputBiz.configurationsForUser(userId, clazz);
		}
		return success(result);
	}

	@RequestMapping(value = "/credentials", method = RequestMethod.GET)
	public Result<Collection<CredentialConfiguration>> listCredentialConfigurations() {
		return listConfigurationsForCurrentUser(CredentialConfiguration.class);
	}

	@RequestMapping(value = "/transforms", method = RequestMethod.GET)
	public Result<Collection<TransformConfiguration>> listTransformConfigurations() {
		return listConfigurationsForCurrentUser(TransformConfiguration.class);
	}

}
