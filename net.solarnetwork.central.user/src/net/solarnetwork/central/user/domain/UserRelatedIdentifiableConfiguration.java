/* ==================================================================
 * UserIdentifiableConfiguration.java - 11/06/2020 9:24:32 am
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

package net.solarnetwork.central.user.domain;

import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.domain.IdentifiableConfiguration;

/**
 * API for an {@link IdentifiableConfiguration} that is a user-related entity.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public interface UserRelatedIdentifiableConfiguration
		extends IdentifiableConfiguration, UserRelatedEntity<UserLongPK> {

}
