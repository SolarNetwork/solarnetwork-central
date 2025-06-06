/* ==================================================================
 * CompositeKey.java - 11/08/2022 10:48:16 am
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
 * API for a composite key.
 * 
 * @author matt
 * @version 1.3
 */
public interface CompositeKey {

	/**
	 * Get a short identifier string.
	 * 
	 * <p>
	 * This implementation returns a string like {@code (x,y,...)} where all key
	 * components are joined with a comma delimiter and the whole key surrounded
	 * by parentheses. Unassigned key components will be represented as an empty
	 * string.
	 * </p>
	 * 
	 * @return the identifier
	 * @since 1.3
	 */
	default String ident() {
		StringBuilder buf = new StringBuilder(64);
		buf.append('(');
		final int len = keyComponentLength();
		for ( int i = 0; i < len; i++ ) {
			if ( i > 0 ) {
				buf.append(',');
			}
			if ( keyComponentIsAssigned(i) ) {
				buf.append(keyComponent(i));
			}
		}
		buf.append(')');
		return buf.toString();
	}

	/**
	 * Get the number of components in the composite key.
	 * 
	 * @return the number of components
	 */
	int keyComponentLength();

	/**
	 * Test if a given key component is assigned a value or not.
	 * 
	 * <p>
	 * This method is designed to support cases when the key component is
	 * generated by an external system (such as a database) but the value is not
	 * allowed to be {@literal null}. In this situation a placeholder value
	 * representing "not a value" can be used, and this method should return
	 * {@literal false} if that value has been set on the instance.
	 * </p>
	 * 
	 * @param index
	 *        the component index to test, starting from {@literal 0}
	 * @return {@literal true} if the key component's value should be considered
	 *         "assigned", {@literal false} otherwise
	 */
	boolean keyComponentIsAssigned(int index);

	/**
	 * Test if all key components have assigned values.
	 * 
	 * @return {@literal true} if all key component values should be considered
	 *         "assigned", {@literal false} otherwise
	 * @see #keyComponentIsAssigned(int)
	 * @since 1.3
	 */
	default boolean allKeyComponentsAreAssigned() {
		final int len = keyComponentLength();
		for ( int i = 0; i < len; i++ ) {
			if ( !keyComponentIsAssigned(i) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the key component for a specific index.
	 * 
	 * @param index
	 *        the index of the key component to get, starting from {@literal 0}
	 * @return the associated key component, or {@literal null} if the component
	 *         is not assigned, or the index is out of range
	 */
	Object keyComponent(int index);

	/**
	 * Convert a value into a key component value.
	 * 
	 * @param <T>
	 *        the expected key component type
	 * @param index
	 *        the index of the key component to convert the value for
	 * @param val
	 *        the value to convert, or {@literal null} to use an "unassigned"
	 *        value
	 * @return the key component value
	 * @throws IllegalArgumentException
	 *         if {@code val} is not a supported type, or {@code index} is out
	 *         of range of the key component length
	 * @since 1.2
	 */
	@SuppressWarnings("TypeParameterUnusedInFormals")
	<T> T keyComponentValue(int index, Object val);

	/**
	 * Create a new key instance based on a template and component arguments.
	 * 
	 * <p>
	 * If less component values are provided than the length of the given key
	 * type, then "unassigned" values will be used for those components.
	 * </p>
	 * 
	 * @param template
	 *        the template key, or the implementation <b>may</b> support
	 *        {@literal null} to create a new key from scratch, or else
	 *        {@link UnsupportedOperationException} will be thrown
	 * @param components
	 *        the component values to use
	 * @return the new key instance
	 * @since 1.2
	 * @throws IllegalArgumentException
	 *         if the component values are not appropriate for the key type
	 */
	CompositeKey createKey(CompositeKey template, Object... components);

}
