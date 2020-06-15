/* ==================================================================
 * UserNodeEventHookService.java - 8/06/2020 2:29:33 pm
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

package net.solarnetwork.central.user.event.biz;

import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.support.LocalizedServiceInfoProvider;

/**
 * API for a service that can handle {@link UserNodeEventTask} objects by acting
 * on them in some way so the event can then be discarded.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserNodeEventHookService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/**
	 * Process an event.
	 * 
	 * @param config
	 *        the event hook configuration
	 * @param event
	 *        the event data
	 * @return {@literal true} if the event was processed successfully and can
	 *         be discarded; {@literal false} if the event was not processed
	 *         successfully and should be tried again in the future
	 * @throws RepeatableTaskException
	 *         if any processing error occurs and the event should be
	 *         reprocessed in the future
	 */
	boolean processUserNodeEventHook(UserNodeEventHookConfiguration config, UserNodeEventTask event)
			throws RepeatableTaskException;

}
