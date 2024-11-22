/* ==================================================================
 * AlsoEnergyFieldFunction.java - 22/11/2024 5:33:25 pm
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

package net.solarnetwork.central.c2c.biz.impl;

/**
 * Enumeration of AlsoEnergy field functions.
 *
 * @author matt
 * @version 1.0
 */
public enum AlsoEnergyFieldFunction {
	/** Average. */
	Avg,

	/** Last posted. */
	Last,

	/** Minimum. */
	Min,

	/** Maximum. */
	Max,

	/** Difference. */
	Diff,

	/** Sum. */
	Sum,

	/** Integral. */
	Integral,

	/** Difference, non zero. */
	DiffNonZero,

	/** Previous. */
	Previous,

	;
}
