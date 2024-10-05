/* ==================================================================
 * CloudDataValue.java - 5/10/2024 8:41:56 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.Map;

/**
 * A cloud data hierarchy component.
 *
 * <p>
 * This is used to represent the cloud data values that can be mapped into a
 * datum stream.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class CloudDataValue<K> {

	/** Standard metadata key for a street address. */
	public static final String STREET_ADDRESS_METADATA = "street";

	/** Standard metadata key for a locality (city) name or code. */
	public static final String LOCALITY_METADATA = "l";

	/** Standard metadata key for a state or province name or code. */
	public static final String STATE_PROVINCE_METADATA = "st";

	/** Standard metadata key for a country name or code. */
	public static final String COUNTRY_METADATA = "c";

	/** Standard metadata key for a time zone identifier. */
	public static final String TIME_ZONE_METADATA = "tz";

	private final K id;
	private final String identifier;
	private final String name;
	private final Map<String, ?> metadata;
	private final Collection<CloudDataValue<K>> children;

	/**
	 * Create a new data value instance.
	 *
	 * @param id
	 *        the component ID
	 * @param identifier
	 *        the value identifier, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public static <T> CloudDataValue<T> dataValue(T id, String identifier, String name,
			Map<String, ?> metadata) {
		return new CloudDataValue<>(id, identifier, name, metadata);
	}

	/**
	 * Create a new data value instance.
	 *
	 * @param parent
	 *        the parent
	 * @param id
	 *        the component ID
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @param children
	 *        the optional children values
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public static <T> CloudDataValue<T> dataValue(T id, String identifier, String name,
			Map<String, ?> metadata, Collection<CloudDataValue<T>> children) {
		return new CloudDataValue<>(id, identifier, name, metadata, children);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the component ID
	 * @param identifier
	 *        the value identifier, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public CloudDataValue(K id, String identifier, String name, Map<String, ?> metadata) {
		this(id, identifier, name, metadata, null);
	}

	/**
	 * Constructor.
	 *
	 * @param parent
	 *        the parent
	 * @param id
	 *        the component ID
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @param children
	 *        the optional children values
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public CloudDataValue(K id, String identifier, String name, Map<String, ?> metadata,
			Collection<CloudDataValue<K>> children) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.identifier = identifier;
		this.name = name;
		this.metadata = metadata;
		this.children = children;
	}

	/**
	 * Get the component identifier.
	 *
	 * @return the id, never {@literal null}
	 */
	public final K getId() {
		return id;
	}

	/**
	 * Get the data value hierarchy identifier.
	 *
	 * @return the identifier, unique within the overall hierarchy, or
	 *         {@literal null} if this component does not have a data value
	 *         reference
	 */
	public final String getIdentifier() {
		return identifier;
	}

	/**
	 * Get the component name.
	 *
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Get the component metadata.
	 *
	 * @return the metadata
	 */
	public final Map<String, ?> getMetadata() {
		return metadata;
	}

	/**
	 * Get the component children.
	 *
	 * @return the children
	 */
	public final Collection<CloudDataValue<K>> getChildren() {
		return children;
	}

}
