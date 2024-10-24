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
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.util.StringUtils;

/**
 * A cloud data hierarchy component.
 *
 * <p>
 * This is used to represent the cloud data values that can be mapped into a
 * datum stream.
 * </p>
 *
 * @author matt
 * @version 1.3
 */
@JsonPropertyOrder({ "name", "reference", "identifiers", "metadata", "children" })
public class CloudDataValue implements Serializable, Comparable<CloudDataValue> {

	private static final long serialVersionUID = 782616385882360558L;

	/** Standard metadata key for a street address. */
	public static final String STREET_ADDRESS_METADATA = "street";

	/** Standard metadata key for a postal code. */
	public static final String POSTAL_CODE_METADATA = "postalCode";

	/** Standard metadata key for a locality (city) name or code. */
	public static final String LOCALITY_METADATA = "l";

	/** Standard metadata key for a state or province name or code. */
	public static final String STATE_PROVINCE_METADATA = "st";

	/** Standard metadata key for a country name or code. */
	public static final String COUNTRY_METADATA = "c";

	/** Standard metadata key for a time zone identifier. */
	public static final String TIME_ZONE_METADATA = "tz";

	/** Standard metadata key for a device manufacturer name. */
	public static final String MANUFACTURER_METADATA = "manufacturer";

	/** Standard metadata key for a device model name. */
	public static final String DEVICE_MODEL_METADATA = "model";

	/**
	 * Standard metadata key for a device firmware version.
	 *
	 * @since 1.2
	 */
	public static final String DEVICE_FIRMWARE_VERSION_METADATA = "firmwareVersion";

	/** Standard metadata key for a device serial number name. */
	public static final String DEVICE_SERIAL_NUMBER_METADATA = "serial";

	/** Standard metadata key for a unit of measure name. */
	public static final String UNIT_OF_MEASURE_METADATA = "unit";

	/**
	 * A wildcard identifier value.
	 *
	 * @since 1.1
	 */
	public static final String WILDCARD_IDENTIFIER = "*";

	private final List<String> identifiers;
	private final String name;
	private final String reference;
	private final Map<String, ?> metadata;
	private final Collection<CloudDataValue> children;

	/**
	 * Generate a path-like reference value out of a list of identifiers.
	 *
	 * @param identifiers
	 *        the identifiers
	 * @return the reference value, never {@literal null}
	 */
	public static String pathReferenceValue(Collection<String> identifiers) {
		var buf = new StringBuilder();
		if ( identifiers != null && !identifiers.isEmpty() ) {
			for ( String ident : identifiers ) {
				buf.append('/').append(ident);
			}
		}
		if ( buf.isEmpty() ) {
			buf.append('/');
		}
		return buf.toString();
	}

	/**
	 * Create a new data value instance.
	 *
	 * <p>
	 * The {@code reference} will be set to a path-like value using the
	 * {@code identifier} components.
	 * </p>
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 * @since 1.3
	 */
	public static CloudDataValue dataValue(List<String> identifiers, String name) {
		return dataValue(identifiers, name, null);
	}

	/**
	 * Create a new data value instance.
	 *
	 * <p>
	 * The {@code reference} will be set to a path-like value using the
	 * {@code identifier} components.
	 * </p>
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 */
	public static CloudDataValue dataValue(List<String> identifiers, String name,
			Map<String, ?> metadata) {
		return new CloudDataValue(identifiers, name, pathReferenceValue(identifiers), metadata);
	}

	/**
	 * Create a new data value instance without any {@code reference} value.
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 */
	public static CloudDataValue intermediateDataValue(List<String> identifiers, String name,
			Map<String, ?> metadata) {
		return new CloudDataValue(identifiers, name, null, metadata);
	}

	/**
	 * Create a new data value instance without any {@code reference} value.
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @param children
	 *        the optional children values
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 * @since 1.1
	 */
	public static CloudDataValue intermediateDataValue(List<String> identifiers, String name,
			Map<String, ?> metadata, Collection<CloudDataValue> children) {
		return new CloudDataValue(identifiers, name, null, metadata, children);
	}

	/**
	 * Create a new data value instance.
	 *
	 * <p>
	 * The {@code reference} will be set to a path-like value using the
	 * {@code identifier} components.
	 * </p>
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param metadata
	 *        the metadata
	 * @param children
	 *        the optional children values
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 */
	public static CloudDataValue dataValue(List<String> identifiers, String name,
			Map<String, ?> metadata, Collection<CloudDataValue> children) {
		return new CloudDataValue(identifiers, name, pathReferenceValue(identifiers), metadata,
				children);
	}

	/**
	 * Create a new data value instance.
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param reference
	 *        the unique hierarchy reference
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 */
	public static CloudDataValue dataValue(List<String> identifiers, String name, String reference,
			Map<String, ?> metadata) {
		return new CloudDataValue(identifiers, name, reference, metadata);
	}

	/**
	 * Create a new data value instance.
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param reference
	 *        the unique hierarchy reference
	 * @param metadata
	 *        the metadata
	 * @param children
	 *        the optional children values
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 */
	public static CloudDataValue dataValue(List<String> identifiers, String name, String reference,
			Map<String, ?> metadata, Collection<CloudDataValue> children) {
		return new CloudDataValue(identifiers, name, reference, metadata, children);
	}

	/**
	 * Constructor.
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param reference
	 *        the unique hierarchy reference
	 * @param metadata
	 *        the metadata
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 */
	public CloudDataValue(List<String> identifiers, String name, String reference,
			Map<String, ?> metadata) {
		this(identifiers, name, reference, metadata, null);
	}

	/**
	 * Constructor.
	 *
	 * @param identifiers
	 *        the value identifiers, unique within the overall hierarchy
	 * @param name
	 *        the component name
	 * @param reference
	 *        the unique hierarchy reference
	 * @param metadata
	 *        the metadata
	 * @param children
	 *        the optional children values
	 * @throws IllegalArgumentException
	 *         if {@code identifiers} or {@code name} is {@literal null}
	 */
	public CloudDataValue(List<String> identifiers, String name, String reference,
			Map<String, ?> metadata, Collection<CloudDataValue> children) {
		super();
		this.identifiers = requireNonNullArgument(identifiers, "identifiers");
		this.name = requireNonNullArgument(name, "name");
		this.reference = reference;
		this.metadata = metadata;
		this.children = children;
	}

	@Override
	public int compareTo(CloudDataValue o) {
		final int lenLeft = identifiers.size();
		final int lenRight = o.identifiers.size();
		for ( int i = 0, len = Math.min(lenRight, lenRight); i < len; i++ ) {
			int result = StringUtils.naturalSortCompare(identifiers.get(i), o.identifiers.get(i), true);
			if ( result != 0 ) {
				return result;
			}
		}
		if ( lenLeft < lenRight ) {
			return -1;
		} else if ( lenLeft > lenRight ) {
			return 1;
		}
		return 0;
	}

	/**
	 * Get the data value hierarchy identifier.
	 *
	 * @return the identifiers, unique within the overall hierarchy, never
	 *         {@literal null}
	 */
	public final List<String> getIdentifiers() {
		return identifiers;
	}

	/**
	 * Get the component name.
	 *
	 * @return the name, never {@literal null}
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Get the reference.
	 *
	 * @return the reference, or {@literal null}
	 */
	public final String getReference() {
		return reference;
	}

	/**
	 * Get the component metadata.
	 *
	 * @return the metadata, or {@literal null}
	 */
	public final Map<String, ?> getMetadata() {
		return metadata;
	}

	/**
	 * Get the component children.
	 *
	 * @return the children
	 */
	public final Collection<CloudDataValue> getChildren() {
		return children;
	}

}
