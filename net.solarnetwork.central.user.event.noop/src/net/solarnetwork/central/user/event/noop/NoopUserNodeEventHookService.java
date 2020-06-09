/* ==================================================================
 * NoopUserNodeEventHookService.java - 9/06/2020 7:36:27 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.noop;

import java.util.Collections;
import java.util.List;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.user.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.domain.UserNodeEventTask;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * No-operation implementation of {@link UserNodeEventHookService}.
 * 
 * @author matt
 * @version 1.0
 */
public class NoopUserNodeEventHookService extends
		BaseSettingsSpecifierLocalizedServiceInfoProvider<String> implements UserNodeEventHookService {

	/**
	 * Constructor.
	 */
	public NoopUserNodeEventHookService() {
		super("net.solarnetwork.central.user.event.noop.NoopUserNodeEventHookService");
	}

	@Override
	public String getDisplayName() {
		return "NOOP User Node Event Hook Service";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public boolean processUserNodeEventHook(UserNodeEventHookConfiguration config,
			UserNodeEventTask event) throws RepeatableTaskException {
		log.debug("Got user node event task {} for user {} hook {} with props {}", event.getId(),
				event.getUserId(), event.getHookId(), event.getTaskProperties());
		return true;
	}

}
