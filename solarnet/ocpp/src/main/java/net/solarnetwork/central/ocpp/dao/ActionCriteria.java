/* ==================================================================
 * ActionCriteria.java - 16/11/2022 5:34:39 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao;

/**
 * Search criteria for action related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface ActionCriteria {

	/**
	 * Get the first action.
	 * 
	 * <p>
	 * This returns the first available action from the {@link #getActionIds()}
	 * array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first action, or {@literal null} if not available
	 */
	String getAction();

	/**
	 * Get an array of actions.
	 * 
	 * @return array of actions (may be {@literal null})
	 */
	String[] getActions();

	/**
	 * Test if this filter has any action criteria.
	 * 
	 * @return {@literal true} if the action ID is non-null
	 */
	default boolean hasActionCriteria() {
		return getAction() != null;
	}

}
