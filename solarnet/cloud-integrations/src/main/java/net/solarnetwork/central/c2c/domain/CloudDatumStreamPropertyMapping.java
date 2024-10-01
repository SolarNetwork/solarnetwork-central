/* ==================================================================
 * CloudDatumStreamPropertyMapping.java - 29/09/2024 2:53:43 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.domain;

import java.math.BigDecimal;
import net.solarnetwork.domain.datum.DatumSamplesType;

/**
 * A mapping of a cloud data reference to a datum stream property.
 *
 * @param name
 *        the datum stream property name
 * @param type
 *        the datum stream property type
 * @param valueReference
 *        the source {@link net.solarnetwork.central.c2c.domain.CloudDataValue}
 *        reference identifier
 * @param multiplier
 *        an optional multiplier to apply to the source data values
 * @param scale
 *        an optional maximum number of decimal places to round the source data
 *        values to
 * @author matt
 * @version 1.0
 */
public record CloudDatumStreamPropertyMapping(String name, DatumSamplesType type, String valueReference,
		BigDecimal multiplier, Integer scale) {

}
