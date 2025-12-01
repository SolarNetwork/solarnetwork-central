/* ==================================================================
 * BaseCloudControlService.java - 3/11/2025 4:53:43 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicCloudControlLocalizedServiceInfo;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Base implementation of {@link CloudControlService}.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseCloudControlService extends BaseCloudIntegrationsIdentifiableService
		implements CloudControlService {

	/** A clock to use. */
	protected final Clock clock;

	/** The integration configuration entity DAO. */
	protected final CloudIntegrationConfigurationDao integrationDao;

	/** The datum stream configuration DAO. */
	protected final CloudControlConfigurationDao controlDao;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param settings
	 *        the service settings
	 * @param integrationDao
	 *        the integration DAO
	 * @param controlDao
	 *        the control DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseCloudControlService(String serviceIdentifier, String displayName,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			List<SettingSpecifier> settings, Clock clock,
			CloudIntegrationConfigurationDao integrationDao, CloudControlConfigurationDao controlDao) {
		super(serviceIdentifier, displayName, userEventAppenderBiz, encryptor, settings);
		this.clock = requireNonNullArgument(clock, "clock");
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.controlDao = requireNonNullArgument(controlDao, "controlDao");
	}

	@Override
	public LocalizedServiceInfo getLocalizedServiceInfo(Locale locale) {
		return new BasicCloudControlLocalizedServiceInfo(
				super.getLocalizedServiceInfo(locale != null ? locale : Locale.getDefault()),
				getSettingSpecifiers(), dataValuesRequireDatumStream());
	}

	/**
	 * Get the data values datum stream requirement.
	 *
	 * @return {@literal true} if a datum stream is required to work with data
	 *         values
	 */
	protected boolean dataValuesRequireDatumStream() {
		return false;
	}

	/**
	 * API to perform an action on a full control configuration model.
	 *
	 * @param <T>
	 *        the result type
	 */
	@FunctionalInterface
	public static interface IntegrationAction<T> {

		/**
		 * Handle a full control configuration model.
		 *
		 * <p>
		 * All arguments will be non-null.
		 * </p>
		 *
		 * @param ms
		 *        the message source
		 * @param control
		 *        the control configuration
		 * @param integration
		 *        the integration
		 */
		T doWithControlIntegration(MessageSource ms, CloudControlConfiguration control,
				CloudIntegrationConfiguration integration);

	}

	/**
	 * Fetch the entities related to a control and perform an action with them.
	 *
	 * <p>
	 * This is a convenient method to invoke in implementations of
	 * {@link CloudControlService#executeInstruction(UserLongCompositePK, net.solarnetwork.central.instructor.domain.NodeInstruction)}
	 * and similar methods.
	 * </p>
	 *
	 * @param <T>
	 *        the result type
	 * @param cloudControlId
	 *        the ID of the cloud control to fetch the related entities of
	 * @param handler
	 *        callback to handle the entities in some way
	 */
	protected <T> T performAction(UserLongCompositePK cloudControlId, IntegrationAction<T> handler) {
		final CloudControlConfiguration control = requireNonNullObject(
				controlDao.get(requireNonNullArgument(cloudControlId, "cloudControlId")),
				"cloudControl");
		return performAction(control, handler);
	}

	/**
	 * Fetch the entities related to a control and perform an action with them.
	 *
	 * <p>
	 * This is a convenient method to invoke in implementations of
	 * {@link CloudControlService#executeInstruction(UserLongCompositePK, net.solarnetwork.central.instructor.domain.NodeInstruction)}
	 * and similar methods.
	 * </p>
	 *
	 * @param <T>
	 *        the result type
	 * @param control
	 *        the control to fetch the related entities of
	 * @param handler
	 *        callback to handle the entities in some way
	 */
	protected <T> T performAction(CloudControlConfiguration control, IntegrationAction<T> handler) {
		assert control != null && handler != null;
		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		if ( !control.isFullyConfigured() ) {
			String msg = "Control is not fully configured.";
			Errors errors = new BindException(control, "control");
			errors.reject("error.control.notFullyConfigured", null, msg);
			throw new ValidationException(msg, errors, ms);
		}

		final var integrationId = new UserLongCompositePK(control.getUserId(),
				requireNonNullArgument(control.getIntegrationId(), "control.integrationId"));
		final CloudIntegrationConfiguration integration = requireNonNullObject(
				integrationDao.get(integrationId), "integration");

		return handler.doWithControlIntegration(ms, control, integration);
	}

}
