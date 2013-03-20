/* ==================================================================
 * WeatherConditions.java - Mar 20, 2013 4:44:54 PM
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

package net.solarnetwork.central.query.domain;

import java.util.TimeZone;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.WeatherDatum;

/**
 * A snapshot of weather conditions at a particular time.
 * 
 * @author matt
 * @version 1.0
 */
public class WeatherConditions {

	private final WeatherDatum weather;
	private final DayDatum day;
	private final TimeZone timeZone;

	/**
	 * Construct with values.
	 * 
	 * @param weather
	 *        the weather
	 * @param day
	 *        the day
	 * @param timeZone
	 *        the time zone
	 */
	public WeatherConditions(WeatherDatum weather, DayDatum day, TimeZone timeZone) {
		super();
		this.weather = weather;
		this.day = day;
		this.timeZone = timeZone;
	}

	public WeatherDatum getWeather() {
		return weather;
	}

	public DayDatum getDay() {
		return day;
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

}
