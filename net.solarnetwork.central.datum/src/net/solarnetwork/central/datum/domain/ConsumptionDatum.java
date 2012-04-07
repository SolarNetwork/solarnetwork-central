/* ===================================================================
 * ConsumptionDatum.java
 * 
 * Created Jul 29, 2009 10:47:39 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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


/**
 * Domain object for a unit of data collected from a power consumption monitor.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class ConsumptionDatum extends BaseNodeDatum implements LocationDatum {

	private static final long serialVersionUID = 714989418475390919L;

	private Long locationId = null;
	private Integer watts = null;
	private Long wattHourReading = null;

	private Float amps = null;
	private Float volts = null;

	/**
	 * Default constructor.
	 */
	public ConsumptionDatum() {
		super();
	}
	
	@Override
	public String toString() {
		return "ConsumptionDatum{nodeId=" +getNodeId()
			+",sourceId=" +getSourceId()
			+",watts=" +getWatts()
			+",wattHourReading=" +this.wattHourReading
			+'}';
	}
	
	/**
	 * Get the watts.
	 * 
	 * <p>This will return the {@code watts} value if available, or
	 * fall back to {@code amps} * {@code volts}.<?p>
	 * 
	 * @return watts, or <em>null</em> if watts not available and 
	 * either amps or volts are null
	 */
	public Integer getWatts() {
		if ( watts != null ) {
			return watts;
		}
		if ( amps == null || volts == null ) {
			return null;
		}
		return Integer.valueOf((int)Math.round(
				amps.doubleValue() * volts.doubleValue()));
	}

	// for backwards-compatibility only
	public void setAmps(Float amps) {
		this.amps = amps;
	}
	
	// for backwards-compatibility only
	public void setVolts(Float volts) {
		this.volts = volts;
	}
	
	public Long getLocationId() {
		return locationId;
	}
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}
	public Long getWattHourReading() {
		return wattHourReading;
	}
	public void setWattHourReading(Long wattHourReading) {
		this.wattHourReading = wattHourReading;
	}
	public void setWatts(Integer watts) {
		this.watts = watts;
	}
	
}
