/* ==================================================================
 * Capability.java - Jun 2, 2011 8:51:08 PM
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

import net.solarnetwork.central.domain.SolarCapability;
import net.solarnetwork.central.domain.SolarNodeCapability;

/**
 * A {@link SolarNodeCapability} with extensions for DRAS support.
 * 
 * @author matt
 * @version $Revision$
 */
public class Capability extends SolarCapability
implements Cloneable, Serializable {

	private static final long serialVersionUID = 6611043478212458160L;

	private String demandResponseKind;
	private Long shedCapacityWatts;
	private Long shedCapacityWattHours;
	private Long varCapacityVoltAmps;

	private Long contractedCapacityWatts;
	
	public Long getShedCapacityWatts() {
		return shedCapacityWatts;
	}
	public void setShedCapacityWatts(Long shedCapacityWatts) {
		this.shedCapacityWatts = shedCapacityWatts;
	}
	public Long getShedCapacityWattHours() {
		return shedCapacityWattHours;
	}
	public void setShedCapacityWattHours(Long shedCapacityWattHours) {
		this.shedCapacityWattHours = shedCapacityWattHours;
	}
	public String getDemandResponseKind() {
		return demandResponseKind;
	}
	public void setDemandResponseKind(String demandResponseKind) {
		this.demandResponseKind = demandResponseKind;
	}
	public Long getVarCapacityVoltAmps() {
		return varCapacityVoltAmps;
	}
	public void setVarCapacityVoltAmps(Long varCapacityVoltAmps) {
		this.varCapacityVoltAmps = varCapacityVoltAmps;
	}
	public Long getContractedCapacityWatts() {
		return contractedCapacityWatts;
	}
	public void setContractedCapacityWatts(Long contractedCapacityWatts) {
		this.contractedCapacityWatts = contractedCapacityWatts;
	}
	
}
