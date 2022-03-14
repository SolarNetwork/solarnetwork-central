/* ==================================================================
 * SourceLocation.java - Oct 23, 2011 8:29:11 PM
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
 */

package net.solarnetwork.central.domain;

/**
 * A filter for locations based on a specific source.
 * 
 * @author matt
 * @version 1.1
 */
public interface SourceLocation extends Filter {

	/**
	 * Get a specific ID to find.
	 * 
	 * @return the ID
	 */
	Long getId();

	/**
	 * Get the source name.
	 * 
	 * @return the source name
	 */
	String getSource();

	/**
	 * A location filter.
	 * 
	 * @return the location filter
	 */
	Location getLocation();

}
