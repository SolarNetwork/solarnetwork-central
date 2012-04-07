/* ==================================================================
 * LocationBoxFilter.java - Jun 10, 2011 1:48:31 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao;

import net.solarnetwork.central.domain.Location;

/**
 * Extends the {@link Location} filter with location-based bounding box.
 * 
 * @author matt
 * @version $Revision$
 */
public interface LocationBoxFilter extends Location, UserAwareFilter {

	/**
	 * Get the decimal latitude, using {@link #getLatitude()} as the
	 * opposite box corner.
	 * 
	 * <p>This is used to perform a simple lat/long based box filter,
	 * where {@link #getLatitude()} and {@link #getLongitude()} represent
	 * one corner of the box and {@link #getBoxLatitude()} and
	 * {@link #getBoxLongitude()} represent the diagonally opposite corner.</p>
	 * 
	 * @return box latitude
	 */
	Double getBoxLatitude();
	
	/**
	 * Get the decimal longitude, using {@link #getLongitude()} as the 
	 * opposite box corner.
	 * 
	 * @return box longitude
	 * @see #getBoxLatitude()
	 */
	Double getBoxLongitude();
	
	/**
	 * Return <em>true</em> if a valid lat/long box is defined via
	 * {@link #getLatitude()}, {@link #getLongitude()}, 
	 * {@link #getBoxLatitude()}, and {@link #getBoxLongitude()}.
	 * 
	 * @return boolean
	 */
	boolean isBox();

}
