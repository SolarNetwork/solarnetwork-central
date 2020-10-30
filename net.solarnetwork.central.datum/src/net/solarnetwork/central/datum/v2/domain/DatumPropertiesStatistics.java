/* ==================================================================
 * DatumPropertiesStatistics.java - 30/10/2020 4:35:52 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Statistic information associated with datum properties.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class DatumPropertiesStatistics implements Serializable {

	private static final long serialVersionUID = -1933887645480711417L;

	private BigDecimal[][] instantaneous;
	private BigDecimal[][] accumulating;

	/**
	 * Create a datum statistics instance.
	 * 
	 * @param instantaneous
	 *        the instantaneous statistic values
	 * @param accumulating
	 *        the accumulating statistic values
	 * @return the new instance, never {@literal null}
	 */
	public static DatumPropertiesStatistics statisticsOf(BigDecimal[][] instantaneous,
			BigDecimal[][] accumulating) {
		DatumPropertiesStatistics s = new DatumPropertiesStatistics();
		s.instantaneous = instantaneous;
		s.accumulating = accumulating;
		return s;
	}

	/**
	 * Get the instantaneous statistics.
	 * 
	 * @return the instantaneous statistics
	 */
	public BigDecimal[][] getInstantaneous() {
		return instantaneous;
	}

	/**
	 * Set the instantaneous statistics.
	 * 
	 * @param instantaneous
	 *        the instantaneous statistics to set
	 */
	public void setInstantaneous(BigDecimal[][] instantaneous) {
		this.instantaneous = instantaneous;
	}

	/**
	 * Get the accumulating statistics.
	 * 
	 * @return the accumulating statistics
	 */
	public BigDecimal[][] getAccumulating() {
		return accumulating;
	}

	/**
	 * Set the accumulating statistics.
	 * 
	 * @param accumulating
	 *        the accumulating statistics to set
	 */
	public void setAccumulating(BigDecimal[][] accumulating) {
		this.accumulating = accumulating;
	}

}
