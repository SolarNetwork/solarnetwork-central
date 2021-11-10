/* ==================================================================
 * AppSetting.java - 10/11/2021 8:21:01 AM
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
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.BasicIdentity;

/**
 * An application setting.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties("id")
public class AppSetting extends BasicIdentity<KeyTypePK>
		implements Entity<KeyTypePK>, Cloneable, Serializable {

	private static final long serialVersionUID = -7907835439616081294L;

	private final Instant created;
	private final Instant modified;
	private final String value;

	/**
	 * Create a new setting instance.
	 * 
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @param value
	 *        the value
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if {@code key} or {@code type} are {@literal null}
	 */
	public static AppSetting appSetting(String key, String type, String value) {
		return new AppSetting(key, type, null, null, value);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param value
	 *        the value
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public AppSetting(KeyTypePK id, Instant created, Instant modified, String value) {
		super(requireNonNullArgument(id, "id"));
		this.created = created;
		this.modified = modified;
		this.value = value;
	}

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the key
	 * @param type
	 *        the type
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @param value
	 *        the value
	 * @throws IllegalArgumentException
	 *         any argument other than {@code value} is {@literal null}
	 */
	public AppSetting(String key, String type, Instant created, Instant modified, String value) {
		this(new KeyTypePK(key, type), created, modified, value);
	}

	/**
	 * Create a copy with a new value.
	 * 
	 * @param value
	 *        the new value to set
	 * @return the new copy
	 */
	public AppSetting withValue(String value) {
		return new AppSetting(getId(), getCreated(), Instant.now(), value);
	}

	@Override
	public AppSetting clone() {
		return (AppSetting) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AppSetting{");
		if ( getKey() != null ) {
			builder.append("key=").append(getKey()).append(", ");
		}
		if ( getType() != null ) {
			builder.append("type=").append(getType()).append(", ");
		}
		if ( value != null ) {
			builder.append("value=").append(value).append(", ");
		}
		if ( created != null ) {
			builder.append("created=").append(created).append(", ");
		}
		if ( modified != null ) {
			builder.append("modified=").append(modified);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Instant getCreated() {
		return created;
	}

	/**
	 * Get the modification date.
	 * 
	 * @return the modification date, or {@literal null}
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Get the key.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return getId().getKey();
	}

	/**
	 * Get the type.
	 * 
	 * @return the type
	 */
	public String getType() {
		return getId().getType();
	}

	/**
	 * Get the value.
	 * 
	 * @return the value, or {@literal null}
	 */
	public String getValue() {
		return value;
	}

}
