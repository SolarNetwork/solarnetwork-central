/* ===================================================================
 * PriceDatum.java
 * 
 * Created Aug 7, 2009 5:42:23 PM
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
 * Domain object for a unit of data collected from a power price monitor.
 *
 * <p>Note a {@code PriceDatum} is not directly related to a {@code SolarNode},
 * and the {@code nodeId} value may actually be <em>null</em>. This class
 * implements both {@link NodeDatum} and {@link LocationDatum} for ease of use,
 * although strictly speaking it is only a {@link LocationDatum}.</p>
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public class PriceDatum extends BaseNodeDatum implements LocationDatum {

	private static final long serialVersionUID = 4601794526965944988L;

	private Long locationId;
	private Float price = null;

	/**
	 * Default constructor.
	 */
	public PriceDatum() {
		super();
	}
	
	@Override
	public String toString() {
		return "PriceDatum{nodeId=" +getNodeId()
			+",locationId=" +getLocationId()
			+",price=" +this.price
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
	 * @return the price
	 */
	public Float getPrice() {
		return price;
	}
	
	/**
	 * @param price the price to set
	 */
	public void setPrice(Float price) {
		this.price = price;
	}
	
}
