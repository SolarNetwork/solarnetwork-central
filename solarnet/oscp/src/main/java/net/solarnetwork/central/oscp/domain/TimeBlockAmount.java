/* ==================================================================
 * TimeBlockAmount.java - 24/08/2022 11:43:33 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain;

import java.math.BigDecimal;
import java.time.Instant;
import oscp.v20.ForecastedBlock;

/**
 * An amount of something associated with a block of time.
 * 
 * @author matt
 * @version 1.0
 */
public record TimeBlockAmount(Instant start, Instant end, Phase phase, BigDecimal amount,
		MeasurementUnit unit) {

	/**
	 * Get an OSCP 2.0 forecast block value for this instance.
	 * 
	 * @return the OSCP 2.0 value
	 */
	public ForecastedBlock toOscp20ForecastValue() {
		ForecastedBlock result = new ForecastedBlock();
		result.setStartTime(start);
		result.setEndTime(end);
		if ( phase != null ) {
			result.setPhase(phase.toOscp20Value());
		}
		if ( amount != null ) {
			result.setCapacity(amount.doubleValue());
		}
		if ( unit != null ) {
			result.setUnit(unit.toOscp20ForecastValue());
		}
		return result;
	}

	/**
	 * Get an instance for an OSCP 2.0 forecast block value.
	 * 
	 * @param type
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static TimeBlockAmount forOscp20ForecastValue(ForecastedBlock block) {
		if ( block == null ) {
			return null;
		}
		Phase phase = (block.getPhase() != null ? Phase.forOscp20Value(block.getPhase()) : null);
		BigDecimal amount = (block.getCapacity() != null ? BigDecimal.valueOf(block.getCapacity())
				: null);
		MeasurementUnit unit = (block.getUnit() != null ? MeasurementUnit.forOscp20Value(block.getUnit())
				: null);
		return new TimeBlockAmount(block.getStartTime(), block.getEndTime(), phase, amount, unit);
	}

}
