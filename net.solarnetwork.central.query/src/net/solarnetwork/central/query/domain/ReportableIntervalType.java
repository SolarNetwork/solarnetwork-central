/* ==================================================================
 * ReportableIntervalType.java - Aug 5, 2009 3:12:15 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.domain;

import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.domain.PowerDatum;

/**
 * Enum type for use in reportable interval calculations.
 */
public enum ReportableIntervalType {

	/** ConsumptionDatum */
	Consumption,

	/** PowerDatum. */
	Power;

	/**
	 * Get a NodeDatum class type for this enum value.
	 * 
	 * @return the class type
	 */
	public Class<? extends NodeDatum> getDatumTypeClass() {
		switch (this) {
			case Consumption:
				return ConsumptionDatum.class;

			case Power:
				return PowerDatum.class;

			default:
				return null;
		}
	}
}
