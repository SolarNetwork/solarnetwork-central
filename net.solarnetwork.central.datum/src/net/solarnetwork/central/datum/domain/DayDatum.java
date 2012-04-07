/* ===================================================================
 * DayDatum.java
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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.datum.domain;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Domain object for day related data.
 * 
 * <p>Note a {@code DayDatum} is not directly related to a {@code SolarNode},
 * and the {@code nodeId} value may actually be <em>null</em>. This class
 * implements both {@link NodeDatum} and {@link LocationDatum} for ease of use,
 * although strictly speaking it is only a {@link LocationDatum}.</p>
 * 
 * <p>The {@code day} property reflects the year/month/day of this datum
 * (e.g. a SQL date value). The {@code sunrise} and {@code sunset} properties 
 * reflect only time values for this day (e.g. a SQL time value).</p>
 * 
 * <p>The {@code latitude} and {@long longitude} may or may not be used, it 
 * depends on how granular the node wants to track day information.</p>
 * 
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
public class DayDatum extends BaseNodeDatum implements LocationDatum {

	private static final long serialVersionUID = 2802754315725736855L;

	private Long locationId;
	private LocalDate day;
	private LocalTime sunrise;
	private LocalTime sunset;
	private Float temperatureHighCelcius;
	private Float temperatureLowCelcius;
	private Float temperatureStartCelcius;
	private Float temperatureEndCelcius;
	private String skyConditions;
	private SkyCondition condition;
	
	/**
	 * Default constructor.
	 */
	public DayDatum() {
		super();
	}

	/**
	 * Test if another DayDatum's {@code day} value is the same
	 * as this instance's {@code day} value.
	 * 
	 * <p>Only non-null values are compared, so if both {@code day} values
	 * are <em>null</em> this method will return <em>false</em>.</p>
	 * 
	 * @param other the DayDatum to compare to
	 * @return <em>true</em> if both {@code day} values are equal
	 */
	public boolean isSameDay(DayDatum other) {
		return this.day != null && other != null && other.day != null
			&& this.day.equals(other.day);
	}
	
	@Override
	public String toString() {
		return "DayDatum{nodeId=" +getNodeId()
			+",locationId=" +this.locationId
			+",day=" +this.day
			+",sunrize=" +this.sunrise
			+",sunset=" +this.sunset
			+'}';
	}

	/**
	 * @return the locationId
	 */
	public Long getLocationId() {
		return locationId;
	}
	
	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	/**
	 * @return the day
	 */
	public LocalDate getDay() {
		return day;
	}

	/**
	 * @param day the day to set
	 */
	public void setDay(LocalDate day) {
		this.day = day;
	}

	/**
	 * @return the sunrise
	 */
	public LocalTime getSunrise() {
		return sunrise;
	}
	
	/**
	 * @param sunrise the sunrise to set
	 */
	public void setSunrise(LocalTime sunrise) {
		this.sunrise = sunrise;
	}
	
	/**
	 * @return the sunset
	 */
	public LocalTime getSunset() {
		return sunset;
	}
	
	/**
	 * @param sunset the sunset to set
	 */
	public void setSunset(LocalTime sunset) {
		this.sunset = sunset;
	}

	/**
	 * @return the temperatureHighCelcius
	 */
	public Float getTemperatureHighCelcius() {
		return temperatureHighCelcius;
	}
	
	/**
	 * @param temperatureHighCelcius the temperatureHighCelcius to set
	 */
	public void setTemperatureHighCelcius(Float temperatureHighCelcius) {
		this.temperatureHighCelcius = temperatureHighCelcius;
	}
	
	/**
	 * @return the temperatureLowCelcius
	 */
	public Float getTemperatureLowCelcius() {
		return temperatureLowCelcius;
	}
	
	/**
	 * @param temperatureLowCelcius the temperatureLowCelcius to set
	 */
	public void setTemperatureLowCelcius(Float temperatureLowCelcius) {
		this.temperatureLowCelcius = temperatureLowCelcius;
	}
	
	/**
	 * @return the skyConditions
	 */
	public String getSkyConditions() {
		return skyConditions;
	}
	
	/**
	 * @param skyConditions the skyConditions to set
	 */
	public void setSkyConditions(String skyConditions) {
		this.skyConditions = skyConditions;
	}
	
	/**
	 * @return the condition
	 */
	public SkyCondition getCondition() {
		return condition;
	}
	
	/**
	 * @param condition the condition to set
	 */
	public void setCondition(SkyCondition condition) {
		this.condition = condition;
	}
	
	/**
	 * @return the temperatureStartCelcius
	 */
	public Float getTemperatureStartCelcius() {
		return temperatureStartCelcius;
	}
	
	/**
	 * @param temperatureStartCelcius the temperatureStartCelcius to set
	 */
	public void setTemperatureStartCelcius(Float temperatureStartCelcius) {
		this.temperatureStartCelcius = temperatureStartCelcius;
	}
	
	/**
	 * @return the temperatureEndCelcius
	 */
	public Float getTemperatureEndCelcius() {
		return temperatureEndCelcius;
	}
	
	/**
	 * @param temperatureEndCelcius the temperatureEndCelcius to set
	 */
	public void setTemperatureEndCelcius(Float temperatureEndCelcius) {
		this.temperatureEndCelcius = temperatureEndCelcius;
	}

}
