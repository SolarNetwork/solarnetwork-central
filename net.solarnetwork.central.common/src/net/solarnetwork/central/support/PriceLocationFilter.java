/* ==================================================================
 * PriceLocationFilter.java - Nov 19, 2013 6:18:55 AM
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

package net.solarnetwork.central.support;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.PriceLocation;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SourceLocation;

/**
 * Filter for {@link PriceLocation}.
 * 
 * @author matt
 * @version 1.0
 */
public class PriceLocationFilter extends SourceLocationFilter {

	private static final long serialVersionUID = -7774575665777266529L;

	private String currency;
	private String timeZoneId;

	/**
	 * Default constructor.
	 */
	public PriceLocationFilter() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param source
	 *        the source name
	 * @param locationName
	 *        the location name
	 */
	public PriceLocationFilter(String source, String locationName) {
		super(source, locationName);
	}

	/**
	 * Copy constructor for a {@link SourceLocation}.
	 * 
	 * @param sourceLocation
	 *        the object to copy
	 */
	public PriceLocationFilter(SourceLocation sourceLocation) {
		super();
		setSource(sourceLocation.getSource());
		Location loc = sourceLocation.getLocation();
		if ( loc instanceof SolarLocation ) {
			setLocation((SolarLocation) loc);
		} else {
			setLocation(new SolarLocation(sourceLocation.getLocation()));
		}
	}

	@Override
	public Map<String, ?> getFilter() {
		Map<String, ?> filter = super.getFilter();
		if ( currency != null || timeZoneId != null ) {
			Map<String, Object> f = new LinkedHashMap<String, Object>(filter);
			if ( currency != null ) {
				f.put("currency", currency);
			}
			if ( timeZoneId != null ) {
				f.put("tz", timeZoneId);
			}
			filter = f;
		}
		return filter;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

}
