/* ==================================================================
 * UserSecretFilter.java - 21/03/2025 5:35:34 pm
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

package net.solarnetwork.central.user.dao;

import net.solarnetwork.central.common.dao.KeyCriteria;
import net.solarnetwork.central.common.dao.TopicCriteria;
import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Filter API for user secret entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserSecretFilter extends UserCriteria, TopicCriteria, KeyCriteria, PaginationCriteria {

}
