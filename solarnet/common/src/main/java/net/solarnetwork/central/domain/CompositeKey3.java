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
 * API for a composite key with three components.
 * 
 * @param <K1>
 *        the first key component type
 * @param <K2>
 *        the second key component type
 * @param <K3>
 *        the third key component type
 * @author matt
 * @version 1.0
 */
public interface CompositeKey3<K1, K2, K3> extends CompositeKey {

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

	/**
	 * Get the third key component.
	 * 
	 * @return the third key component
	 */
	K3 keyComponent3();

	@Override
	default int keyComponentLength() {
		return 3;
	}

	@Override
	default boolean keyComponentIsAssigned(int index) {
		switch (index) {
			case 0:
				return keyComponent1() != null;

			case 1:
				return keyComponent2() != null;

			case 2:
				return keyComponent3() != null;
		}
		return false;
	}

	@Override
	default Object keyComponent(int index) {
		if ( !keyComponentIsAssigned(index) ) {
			return null;
		}
		switch (index) {
			case 0:
				return keyComponent1();

			case 1:
				return keyComponent2();

			case 2:
				return keyComponent3();
		}
		return null;
	}

}
