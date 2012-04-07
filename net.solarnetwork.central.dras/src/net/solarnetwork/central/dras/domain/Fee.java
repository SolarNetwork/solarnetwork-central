/* ==================================================================
 * Fee.java - Jun 23, 2011 1:50:50 PM
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

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;

import org.joda.time.Period;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * Fee schedule associated with users.
 * 
 * @author matt
 * @version $Revision$
 */
public class Fee extends BaseEntity implements Cloneable, Serializable {

	private static final long serialVersionUID = -3399592519530154580L;

	private String currency;
	private Long establishFee; 	// one off
	private Long availableFee; 	// per availabilityPeriod
	private Period availablePeriod;
	private Long eventFee;		// per event
	private Long deliveryFee;	// per watt hour
	private Long cancelFee;		// per canceled event
	
	/**
	 * Default constructor.
	 */
	public Fee() {
		super();
	}
	
	/**
	 * Construct with an ID.
	 * 
	 * @param id the ID
	 */
	public Fee(Long id) {
		super();
		setId(id);
	}
	
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public Long getEstablishFee() {
		return establishFee;
	}
	public void setEstablishFee(Long establishFee) {
		this.establishFee = establishFee;
	}
	public Long getAvailableFee() {
		return availableFee;
	}
	public void setAvailableFee(Long availableFee) {
		this.availableFee = availableFee;
	}
	public Period getAvailablePeriod() {
		return availablePeriod;
	}
	public void setAvailablePeriod(Period availablePeriod) {
		this.availablePeriod = availablePeriod;
	}
	public Long getEventFee() {
		return eventFee;
	}
	public void setEventFee(Long eventFee) {
		this.eventFee = eventFee;
	}
	public Long getDeliveryFee() {
		return deliveryFee;
	}
	public void setDeliveryFee(Long deliveryFee) {
		this.deliveryFee = deliveryFee;
	}
	public Long getCancelFee() {
		return cancelFee;
	}
	public void setCancelFee(Long cancelFee) {
		this.cancelFee = cancelFee;
	}
	
}
