/* ==================================================================
 * CompositeKey2.java - 11/08/2022 10:19:49 am
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

package net.solarnetwork.central.domain;

/**
 * API for a composite key with two components.
 *
 * @param <K1>
 *        the first key component type
 * @param <K2>
 *        the second key component type
 * @author matt
 * @version 1.1
 */
public interface CompositeKey2<K1, K2> extends CompositeKey {

	/**
	 * Get the first key component.
	 *
	 * @return the first key component
	 */
	K1 keyComponent1();

	/**
	 * Get the second key component.
	 *
	 * @return the second key component
	 */
	K2 keyComponent2();

	@Override
	default int keyComponentLength() {
		return 2;
	}

	@Override
	default boolean keyComponentIsAssigned(int index) {
		return switch (index) {
			case 0 -> keyComponent1() != null;
			case 1 -> keyComponent2() != null;
			default -> false;
		};
	}

	@Override
	default Object keyComponent(int index) {
		if ( !keyComponentIsAssigned(index) ) {
			return null;
		}
		return switch (index) {
			case 0 -> keyComponent1();
			case 1 -> keyComponent2();
			default -> null;
		};
	}

}
