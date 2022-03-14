/* ==================================================================
 * ReportingGeneralLocationDatumMatch.java - Oct 17, 2014 2:26:57 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import java.util.Map;

/**
 * API for an aggregate (reporting) {@link GeneralLocationDatumFilterMatch}.
 * 
 * @author matt
 * @version 1.0
 */
public interface ReportingGeneralLocationDatumMatch extends ReportingDatum,
		GeneralLocationDatumFilterMatch {

	/**
	 * Get all available sample data as a Map.
	 * 
	 * @return the sample data as a Map, or <em>null</em> if no sample data
	 *         available
	 */
	public Map<String, ?> getSampleData();

}
