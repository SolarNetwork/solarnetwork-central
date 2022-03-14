/* ==================================================================
 * GenericSourceLocationFilter.java - Nov 18, 2013 6:37:59 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.web;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.support.SourceLocationFilter;

/**
 * Extension of {@link SourceLocationFilter} to add generic type support.
 * 
 * @author matt
 * @version 1.0
 */
public class GenericSourceLocationFilter extends SourceLocationFilter {

	private static final long serialVersionUID = 3714571144435365987L;

	public enum LocationType {
		Basic, Price, Weather;
	}

	private LocationType type = LocationType.Basic;

	/**
	 * Default constructor.
	 */
	public GenericSourceLocationFilter() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param type
	 *        the type
	 * @param source
	 *        the source name
	 * @param locationName
	 *        the location name
	 */
	public GenericSourceLocationFilter(LocationType type, String source, String locationName) {
		super(source, locationName);
		this.type = type;
	}

	@Override
	public Map<String, ?> getFilter() {
		Map<String, ?> filter = super.getFilter();
		if ( type != null ) {
			Map<String, Object> f = new LinkedHashMap<String, Object>(filter);
			f.put("locationType", type);
			filter = f;
		}
		return filter;
	}

	public LocationType getType() {
		return type;
	}

	public void setType(LocationType type) {
		this.type = type;
	}

}
