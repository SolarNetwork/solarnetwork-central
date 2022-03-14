/* ==================================================================
 * KeyTypePK.java - 10/11/2021 8:24:15 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.util.Objects;

/**
 * Primary key based on a key/type string tuple.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class KeyTypePK extends BasePK implements Cloneable, Serializable, Comparable<KeyTypePK> {

	private static final long serialVersionUID = -8662588990798009764L;

	private final String key;
	private final String type;

	/**
	 * Create a new key instance.
	 * 
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @return the key
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static KeyTypePK keyType(String key, String type) {
		return new KeyTypePK(key, type);
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public KeyTypePK(String key, String type) {
		super();
		this.key = requireNonNullArgument(key, "key");
		this.type = requireNonNullArgument(type, "type");
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("k=");
		if ( key != null ) {
			buf.append(key);
		}
		buf.append(";t=");
		if ( type != null ) {
			buf.append(type);
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		if ( key != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("key=");
			buf.append(key);
		}
		if ( type != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("type=");
			buf.append(type);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, type);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof KeyTypePK) ) {
			return false;
		}
		KeyTypePK other = (KeyTypePK) obj;
		return Objects.equals(key, other.key) && Objects.equals(type, other.type);
	}

	/**
	 * Compare two {@code KeyTypePK} objects.
	 * 
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 * 
	 * <ol>
	 * <li>key</li>
	 * <li>type</li>
	 * </ol>
	 * 
	 * @param o
	 *        the object to compare to
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	@Override
	public int compareTo(KeyTypePK o) {
		if ( o == null ) {
			return 1;
		}
		int comparison = key.compareTo(o.key);
		if ( comparison != 0 ) {
			return comparison;
		}
		return type.compareTo(o.type);
	}

	/**
	 * Get the key.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the type.
	 * 
	 * @return the type
	 */
	public String getType() {
		return type;
	}

}
