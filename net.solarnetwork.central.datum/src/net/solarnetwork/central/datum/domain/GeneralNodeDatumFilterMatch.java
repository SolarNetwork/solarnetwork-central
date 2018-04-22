/* ==================================================================
 * GeneralNodeDatumFilterMatch.java - Aug 27, 2014 7:08:17 PM
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
import net.solarnetwork.central.domain.FilterMatch;

/**
 * API for a GeneralNodeDatum search or filter match result.
 * 
 * @author matt
 * @version 1.1
 */
public interface GeneralNodeDatumFilterMatch extends FilterMatch<GeneralNodeDatumPK>, ReportingDatum {

	/**
	 * Get all available sample data as a Map.
	 * 
	 * @return the sample data as a Map, or <em>null</em> if no sample data
	 *         available
	 */
	public Map<String, ?> getSampleData();

}
