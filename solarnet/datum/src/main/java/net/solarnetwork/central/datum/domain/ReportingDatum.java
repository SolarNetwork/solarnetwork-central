/* ===================================================================
 * ReportingDatum.java
 * 
 * Created Sep 24, 2009 3:04:11 PM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.central.datum.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Basic reporting-level Datum API.
 * 
 * @author matt
 * @version 2.0
 */
public interface ReportingDatum {

	/**
	 * Get a "local" date for this datum, local to the node or location the
	 * datum is associated with.
	 * 
	 * @return local date
	 */
	public LocalDate getLocalDate();

	/**
	 * Get a "local" time for this datum, local to the node or location the
	 * datum is associated with.
	 * 
	 * @return local time
	 */
	public LocalTime getLocalTime();

}
