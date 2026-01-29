/* ==================================================================
 * PropertyRestrictedObjectDatumStreamMetadata.java - 28/01/2026 5:14:28 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics.AccumulatingStatistic;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics.InstantaneousStatistic;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Adapt an {@link ObjectDatumStreamMetadata} to a restricted set of property
 * names.
 *
 * @author matt
 * @version 1.0
 */
public class PropertyRestrictedObjectDatumStreamMetadata implements ObjectDatumStreamMetadata {

	/** The delegate metadata source. */
	private final ObjectDatumStreamMetadata delegate;

	/** The restricted instantaneous property names. */
	private final String[] restrictedInstantaneousProperties;

	/**
	 * The restricted instantaneous delegate index mapping (restricted index to
	 * delegate index).
	 */
	private final int[] restrictedInstantaneousMapping;

	/** The restricted accumulating property names. */
	private final String[] restrictedAccumulatingProperties;

	/**
	 * The restricted accumulating delegate index mapping (restricted index to
	 * delegate index).
	 */
	private final int[] restrictedAccumulatingMapping;

	/** The restricted status property names. */
	private final String[] restrictedStatusProperties;

	/**
	 * The restricted status delegate index mapping (restricted index to
	 * delegate index).
	 */
	private final int[] restrictedStatusMapping;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate metadata
	 * @param allowedNames
	 *        the restricted set of property names to allow
	 */
	public PropertyRestrictedObjectDatumStreamMetadata(ObjectDatumStreamMetadata delegate,
			Set<String> allowedNames) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		requireNonNullArgument(allowedNames, "allowedNames");

		String[] names = delegate.propertyNamesForType(DatumSamplesType.Instantaneous);
		if ( names != null && names.length > 0 ) {
			List<String> restrictedNames = new ArrayList<>(names.length);
			int[] mapping = new int[names.length];
			restrictNames(allowedNames, names, restrictedNames, mapping);
			if ( restrictedNames.size() < names.length ) {
				restrictedInstantaneousProperties = restrictedNames.toArray(String[]::new);
				restrictedInstantaneousMapping = fitToSize(mapping,
						restrictedInstantaneousProperties.length);
			} else {
				restrictedInstantaneousProperties = null;
				restrictedInstantaneousMapping = null;
			}
		} else {
			restrictedInstantaneousProperties = null;
			restrictedInstantaneousMapping = null;
		}

		names = delegate.propertyNamesForType(DatumSamplesType.Accumulating);
		if ( names != null && names.length > 0 ) {
			List<String> restrictedNames = new ArrayList<>(names.length);
			int[] mapping = new int[names.length];
			restrictNames(allowedNames, names, restrictedNames, mapping);
			if ( restrictedNames.size() < names.length ) {
				restrictedAccumulatingProperties = restrictedNames.toArray(String[]::new);
				restrictedAccumulatingMapping = fitToSize(mapping,
						restrictedAccumulatingProperties.length);
			} else {
				restrictedAccumulatingProperties = null;
				restrictedAccumulatingMapping = null;
			}
		} else {
			restrictedAccumulatingProperties = null;
			restrictedAccumulatingMapping = null;
		}

		names = delegate.propertyNamesForType(DatumSamplesType.Status);
		if ( names != null && names.length > 0 ) {
			List<String> restrictedNames = new ArrayList<>(names.length);
			int[] mapping = new int[names.length];
			restrictNames(allowedNames, names, restrictedNames, mapping);
			if ( restrictedNames.size() < names.length ) {
				restrictedStatusProperties = restrictedNames.toArray(String[]::new);
				restrictedStatusMapping = fitToSize(mapping, restrictedStatusProperties.length);
			} else {
				restrictedStatusProperties = null;
				restrictedStatusMapping = null;
			}
		} else {
			restrictedStatusProperties = null;
			restrictedStatusMapping = null;
		}
	}

	private static void restrictNames(Set<String> allowedNames, String[] names, List<String> outputNames,
			int[] outputMapping) {
		for ( int i = 0; i < names.length; i++ ) {
			String name = names[i];
			if ( allowedNames.contains(name) ) {
				outputMapping[outputNames.size()] = i;
				outputNames.add(name);
			}
		}
	}

	private static int[] fitToSize(final int[] mapping, final int len) {
		if ( len < 1 ) {
			return null;
		}
		int[] result = new int[len];
		System.arraycopy(mapping, 0, result, 0, len);
		return result;
	}

	@Override
	public UUID getStreamId() {
		return delegate.getStreamId();
	}

	@Override
	public String getTimeZoneId() {
		return delegate.getTimeZoneId();
	}

	@Override
	public Long getObjectId() {
		return delegate.getObjectId();
	}

	@Override
	public String getSourceId() {
		return delegate.getSourceId();
	}

	@Override
	public String[] getPropertyNames() {
		if ( restrictedInstantaneousProperties == null && restrictedAccumulatingProperties == null
				&& restrictedStatusProperties == null ) {
			return delegate.getPropertyNames();
		}

		final String[] iProps = propertyNamesForType(DatumSamplesType.Instantaneous);
		final String[] aProps = propertyNamesForType(DatumSamplesType.Accumulating);
		final String[] sProps = propertyNamesForType(DatumSamplesType.Status);

		final int iLen = (iProps != null ? iProps.length : 0);
		final int aLen = (aProps != null ? aProps.length : 0);
		final int sLen = (sProps != null ? sProps.length : 0);
		final int len = iLen + aLen + sLen;
		if ( len < 1 ) {
			return null;
		}
		String[] result = new String[len];
		if ( iLen > 0 ) {
			System.arraycopy(iProps, 0, result, 0, iLen);
		}
		if ( aLen > 0 ) {
			System.arraycopy(aProps, 0, result, iLen, aLen);
		}
		if ( sLen > 0 ) {
			System.arraycopy(sProps, 0, result, iLen + aLen, sLen);
		}
		return result;

	}

	@Override
	public String getMetaJson() {
		return delegate.getMetaJson();
	}

	@Override
	public String[] propertyNamesForType(DatumSamplesType type) {
		return switch (type) {
			case Instantaneous -> restrictedInstantaneousProperties != null
					? (restrictedInstantaneousProperties.length > 0 ? restrictedInstantaneousProperties
							: null)
					: delegate.propertyNamesForType(type);
			case Accumulating -> restrictedAccumulatingProperties != null
					? (restrictedAccumulatingProperties.length > 0 ? restrictedAccumulatingProperties
							: null)
					: delegate.propertyNamesForType(type);
			case Status -> restrictedStatusProperties != null
					? (restrictedStatusProperties.length > 0 ? restrictedStatusProperties : null)
					: delegate.propertyNamesForType(type);
			default -> delegate.propertyNamesForType(type);
		};
	}

	@Override
	public Object value(DatumProperties props, DatumSamplesType type, int propertyIndex) {
		final int[] mapping = switch (type) {
			case Instantaneous -> restrictedInstantaneousMapping;
			case Accumulating -> restrictedAccumulatingMapping;
			case Status -> restrictedStatusMapping;
			default -> null;
		};
		if ( mapping == null ) {
			return ObjectDatumStreamMetadata.super.value(props, type, propertyIndex);
		}
		if ( propertyIndex >= mapping.length ) {
			return null;
		}
		return props.value(type, mapping[propertyIndex]);
	}

	@Override
	public BigDecimal stat(DatumPropertiesStatistics stats, InstantaneousStatistic type,
			int propertyIndex) {
		if ( restrictedInstantaneousMapping == null ) {
			return ObjectDatumStreamMetadata.super.stat(stats, type, propertyIndex);
		}
		if ( propertyIndex >= restrictedInstantaneousMapping.length ) {
			return null;
		}
		return stats.stat(type, restrictedInstantaneousMapping[propertyIndex]);
	}

	@Override
	public BigDecimal stat(DatumPropertiesStatistics stats, AccumulatingStatistic type,
			int propertyIndex) {
		if ( restrictedAccumulatingMapping == null ) {
			return ObjectDatumStreamMetadata.super.stat(stats, type, propertyIndex);
		}
		if ( propertyIndex >= restrictedAccumulatingMapping.length ) {
			return null;
		}
		return stats.stat(type, restrictedAccumulatingMapping[propertyIndex]);
	}

	@Override
	public ObjectDatumKind getKind() {
		return delegate.getKind();
	}

	@Override
	public Location getLocation() {
		return delegate.getLocation();
	}

	/**
	 * Get the property mapping for a sample type.
	 *
	 * <p>
	 * Only {@code Instantaneous}, {@code Accumulating}, and {@code Status}
	 * types are supported.
	 * </p>
	 *
	 * @param type
	 *        the type to get the mapping for
	 * @return the mapping of restricted property name index to actual datum
	 *         stream property name index
	 */
	public int[] propertyMappingForType(DatumSamplesType type) {
		return switch (type) {
			case Instantaneous -> restrictedInstantaneousMapping;
			case Accumulating -> restrictedAccumulatingMapping;
			case Status -> restrictedStatusMapping;
			default -> null;
		};
	}

}
