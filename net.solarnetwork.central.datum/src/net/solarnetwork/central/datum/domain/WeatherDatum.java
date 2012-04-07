/* ===================================================================
 * WeatherDatum.java
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

import org.joda.time.DateTime;

/**
 * Domain object for weather related data.
 * 
 * <p>Note a {@code WeatherDatum} is not directly related to a {@code SolarNode},
 * and the {@code nodeId} value may actually be <em>null</em>. This class
 * implements both {@link NodeDatum} and {@link LocationDatum} for ease of use,
 * although strictly speaking it is only a {@link LocationDatum}.</p>
 * 
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
public class WeatherDatum extends BaseNodeDatum implements LocationDatum {
	
	private static final long serialVersionUID = 5090759003465856008L;

	private Long locationId;
	private DateTime infoDate;			// date weather info current as
	private String skyConditions;
	private Float temperatureCelcius;
	private Float humidity;
	private Float barometricPressure;
	private String barometerDelta;
	private Float visibility;
	private Integer uvIndex;
	private Float dewPoint;
	private SkyCondition condition;

	/**
	 * Default constructor.
	 */
	public WeatherDatum() {
		super();
	}
	
	@Override
	public String toString() {
		return "WeatherDatum{nodeId=" +getNodeId()
			+",locationId=" +this.locationId
			+",infoDate=" +this.infoDate
			+",temp=" +this.temperatureCelcius
			+",humidity=" +this.humidity
			+",barometricPressure=" +this.barometricPressure
			+",barometerDelta=" +this.barometerDelta
			+'}';
	}
	
	/**
	 * Test if another WeatherDatum's {@code infoDate} value is the same
	 * as this instance's {@code infoDate} value.
	 * 
	 * <p>Only non-null values are compared, so if both {@code infoDate} values
	 * are <em>null</em> this method will return <em>false</em>.</p>
	 * 
	 * @param other the WeatherDatum to compare to
	 * @return <em>true</em> if both {@code infoDate} values are equal
	 */
	public boolean isSameInfoDate(WeatherDatum other) {
		return this.infoDate != null && other != null && other.infoDate != null
			&& this.infoDate.isEqual(other.infoDate);
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
	 * @return the temperatureCelcius
	 */
	public Float getTemperatureCelcius() {
		return temperatureCelcius;
	}
	
	/**
	 * @param temperatureCelcius the temperatureCelcius to set
	 */
	public void setTemperatureCelcius(Float temperatureCelcius) {
		this.temperatureCelcius = temperatureCelcius;
	}
	
	/**
	 * @return the humidity
	 */
	public Float getHumidity() {
		return humidity;
	}
	
	/**
	 * @param humidity the humidity to set
	 */
	public void setHumidity(Float humidity) {
		this.humidity = humidity;
	}
	
	/**
	 * @return the barometricPressure
	 */
	public Float getBarometricPressure() {
		return barometricPressure;
	}
	
	/**
	 * @param barometricPressure the barometricPressure to set
	 */
	public void setBarometricPressure(Float barometricPressure) {
		this.barometricPressure = barometricPressure;
	}
	
	/**
	 * @return the barometerDelta
	 */
	public String getBarometerDelta() {
		return barometerDelta;
	}
	
	/**
	 * @param barometerDelta the barometerDelta to set
	 */
	public void setBarometerDelta(String barometerDelta) {
		this.barometerDelta = barometerDelta;
	}
	
	/**
	 * @return the visibility
	 */
	public Float getVisibility() {
		return visibility;
	}
	
	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(Float visibility) {
		this.visibility = visibility;
	}
	
	/**
	 * @return the uvIndex
	 */
	public Integer getUvIndex() {
		return uvIndex;
	}
	
	/**
	 * @param uvIndex the uvIndex to set
	 */
	public void setUvIndex(Integer uvIndex) {
		this.uvIndex = uvIndex;
	}
	
	/**
	 * @return the dewPoint
	 */
	public Float getDewPoint() {
		return dewPoint;
	}
	
	/**
	 * @param dewPoint the dewPoint to set
	 */
	public void setDewPoint(Float dewPoint) {
		this.dewPoint = dewPoint;
	}
	
	/**
	 * @return the infoDate
	 */
	public DateTime getInfoDate() {
		return infoDate;
	}

	/**
	 * @param infoDate the infoDate to set
	 */
	public void setInfoDate(DateTime infoDate) {
		this.infoDate = infoDate;
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
	
}
