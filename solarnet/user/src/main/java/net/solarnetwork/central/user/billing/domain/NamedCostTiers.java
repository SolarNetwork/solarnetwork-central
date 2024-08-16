/* ==================================================================
 * NamedCostTiers.java - 16/08/2024 2:48:33 pm
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

package net.solarnetwork.central.user.billing.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * A set of named costs associated with a date.
 * 
 * @author matt
 * @version 1.0
 */
public interface NamedCostTiers {

	/**
	 * Get the tiers.
	 * 
	 * @return the tiers (unmodifiable)
	 */
	public List<? extends NamedCost> getTiers();

	/**
	 * Get the tiers date.
	 * 
	 * <p>
	 * The {@code date} might be interpreted as an effective date for this
	 * collection of tiers.
	 * </p>
	 * 
	 * @return the date, or {@literal null}
	 */
	LocalDate getDate();

}
