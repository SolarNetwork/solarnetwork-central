/* ==================================================================
 * GlobalMetricTrackerCollector.java - 1/11/2018 7:30:10 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.tracker.biz;

import net.solarnetwork.central.datum.domain.GeneralNodeDatum;

/**
 * API for collecting metric tracker data.
 * 
 * @author matt
 * @version 1.0
 */
public interface GlobalMetricTrackerCollector {

	/**
	 * Add a collection of node datum to the tracking collector for tracking
	 * purposes.
	 * 
	 * @param datums
	 *        the datum to accept
	 */
	public void addGeneralNodeDatum(Iterable<GeneralNodeDatum> datums);
}
