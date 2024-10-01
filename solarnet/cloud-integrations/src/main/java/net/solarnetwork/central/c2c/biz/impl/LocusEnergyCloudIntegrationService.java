/* ==================================================================
 * LocusEnergyCloudIntegrationService.java - 30/09/2024 12:05:16 pm
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Locus Energy implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.0
 */
public class LocusEnergyCloudIntegrationService extends
		BaseSettingsSpecifierLocalizedServiceInfoProvider<String> implements CloudIntegrationService {

	/** The client identifier setting name. */
	public static final String CLIENT_ID_SETTING = "clientId";

	/** The client secret setting name. */
	public static final String CLIENT_SECRET_SETTING = "clientSecret";

	/** The username setting name. */
	public static final String USERNAME_SETTING = "username";

	/** The password setting name. */
	public static final String PASSWORD_SETTING = "password";

	private final Collection<CloudDatumStreamService> datumStreamServices;

	private static final List<SettingSpecifier> SETTINGS;
	static {
		var settings = new ArrayList<SettingSpecifier>(1);
		settings.add(new BasicTextFieldSettingSpecifier(CLIENT_ID_SETTING, null));
		settings.add(new BasicTextFieldSettingSpecifier(CLIENT_SECRET_SETTING, null, true));
		settings.add(new BasicTextFieldSettingSpecifier(USERNAME_SETTING, null));
		settings.add(new BasicTextFieldSettingSpecifier(PASSWORD_SETTING, null, true));
		SETTINGS = Collections.unmodifiableList(settings);
	}

	/**
	 * Constructor.
	 *
	 * @param datumStreamService
	 *        the datum stream service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LocusEnergyCloudIntegrationService(LocusEnergyCloudDatumStreamService datumStreamService) {
		super("net.solarnetwork.c2c.LocusEnergyIntegrationService");
		this.datumStreamServices = Collections
				.singleton(requireNonNullArgument(datumStreamService, "datumStreamService"));
	}

	@Override
	public String getDisplayName() {
		return "Locus Energy";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return SETTINGS;
	}

	@Override
	public Iterable<CloudDatumStreamService> datumStreamServices() {
		return datumStreamServices;
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration config) {
		// TODO Auto-generated method stub
		return null;
	}

}
