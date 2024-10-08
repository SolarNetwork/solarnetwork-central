/* ==================================================================
 * BaseCloudIntegrationsIdentifiableService.java - 7/10/2024 7:52:14 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * Extension of {@link BaseSettingsSpecifierLocalizedServiceInfoProvider} for
 * cloud integration services.
 *
 * @author matt
 * @version 1.0
 */
public abstract class BaseCloudIntegrationsIdentifiableService
		extends BaseSettingsSpecifierLocalizedServiceInfoProvider<String>
		implements CloudIntegrationsUserEvents {

	/** The display name. */
	protected final String displayName;

	/** The user event appender service. */
	protected final UserEventAppenderBiz userEventAppenderBiz;

	/** A sensitive value encryptor. */
	protected final TextEncryptor encryptor;

	/** The service settings. */
	protected final List<SettingSpecifier> settings;

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseCloudIntegrationsIdentifiableService(String serviceIdentifier, String displayName,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			List<SettingSpecifier> settings) {
		super(serviceIdentifier);
		this.displayName = requireNonNullArgument(displayName, "displayName");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.encryptor = requireNonNullArgument(encryptor, "encryptor");
		this.settings = requireNonNullArgument(settings, "settings");
	}

	@Override
	public final String getDisplayName() {
		return displayName;
	}

	@Override
	public final List<SettingSpecifier> getSettingSpecifiers() {
		return settings;
	}

}
