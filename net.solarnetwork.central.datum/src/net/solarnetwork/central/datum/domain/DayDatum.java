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
 */

package net.solarnetwork.central.datum.domain;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Domain object for day related data.
 * 
 * <p>
 * Note a {@code DayDatum} is not directly related to a {@code SolarNode}, and
 * the {@code nodeId} value may actually be <em>null</em>. This class implements
 * both {@link NodeDatum} and {@link LocationDatum} for ease of use, although
 * strictly speaking it is only a {@link LocationDatum}.
 * </p>
 * 
 * <p>
 * The {@code day} property reflects the year/month/day of this datum (e.g. a
 * SQL date value). The {@code sunrise} and {@code sunset} properties reflect
 * only time values for this day (e.g. a SQL time value).
 * </p>
 * 
 * <p>
 * The {@code latitude} and {@long longitude} may or may not be used, it depends
 * on how granular the node wants to track day information.
 * </p>
 * 
 * @author matt.magoffin
 * @version 1.1
 */
public class DayDatum extends BaseNodeDatum implements LocationDatum {

	private static final long serialVersionUID = 2802754315725736855L;

	private Long locationId;
	private LocalDate day;
	private LocalTime sunrise;
	private LocalTime sunset;
	private Float temperatureHighCelsius;
	private Float temperatureLowCelsius;
	private Float temperatureStartCelsius;
	private Float temperatureEndCelsius;
	private String skyConditions;
	private SkyCondition condition;

	/**
	 * Default constructor.
	 */
	public DayDatum() {
		super();
	}

	/**
	 * Test if another DayDatum's {@code day} value is the same as this
	 * instance's {@code day} value.
	 * 
	 * <p>
	 * Only non-null values are compared, so if both {@code day} values are
	 * <em>null</em> this method will return <em>false</em>.
	 * </p>
	 * 
	 * @param other
	 *        the DayDatum to compare to
	 * @return <em>true</em> if both {@code day} values are equal
	 */
	public boolean isSameDay(DayDatum other) {
		return this.day != null && other != null && other.day != null && this.day.equals(other.day);
	}

	@Override
	public String toString() {
		return "DayDatum{nodeId=" + getNodeId() + ",locationId=" + this.locationId + ",day=" + this.day
				+ ",sunrize=" + this.sunrise + ",sunset=" + this.sunset + '}';
	}

	@Override
	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public LocalDate getDay() {
		return day;
	}

	public void setDay(LocalDate day) {
		this.day = day;
	}

	public LocalTime getSunrise() {
		return sunrise;
	}

	public void setSunrise(LocalTime sunrise) {
		this.sunrise = sunrise;
	}

	public LocalTime getSunset() {
		return sunset;
	}

	public void setSunset(LocalTime sunset) {
		this.sunset = sunset;
	}

	@Deprecated
	@JsonIgnore
	public Float getTemperatureHighCelcius() {
		return temperatureHighCelsius;
	}

	public Float getTemperatureHighCelsius() {
		return temperatureHighCelsius;
	}

	public void setTemperatureHighCelsius(Float temperatureHighCelcius) {
		this.temperatureHighCelsius = temperatureHighCelcius;
	}

	@Deprecated
	@JsonIgnore
	public Float getTemperatureLowCelcius() {
		return temperatureLowCelsius;
	}

	public Float getTemperatureLowCelsius() {
		return temperatureLowCelsius;
	}

	public void setTemperatureLowCelsius(Float temperatureLowCelcius) {
		this.temperatureLowCelsius = temperatureLowCelcius;
	}

	public String getSkyConditions() {
		return skyConditions;
	}

	public void setSkyConditions(String skyConditions) {
		this.skyConditions = skyConditions;
	}

	public SkyCondition getCondition() {
		return condition;
	}

	public void setCondition(SkyCondition condition) {
		this.condition = condition;
	}

	@Deprecated
	@JsonIgnore
	public Float getTemperatureStartCelcius() {
		return temperatureStartCelsius;
	}

	public Float getTemperatureStartCelsius() {
		return temperatureStartCelsius;
	}

	public void setTemperatureStartCelsius(Float temperatureStartCelcius) {
		this.temperatureStartCelsius = temperatureStartCelcius;
	}

	@Deprecated
	@JsonIgnore
	public Float getTemperatureEndCelcius() {
		return temperatureEndCelsius;
	}

	public Float getTemperatureEndCelsius() {
		return temperatureEndCelsius;
	}

	public void setTemperatureEndCelsius(Float temperatureEndCelcius) {
		this.temperatureEndCelsius = temperatureEndCelcius;
	}

}
