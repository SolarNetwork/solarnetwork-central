/* ==================================================================
 * SourceLocationFilter.java - Oct 19, 2011 6:48:11 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.domain.MutableSortDescriptor;
import net.solarnetwork.domain.SerializeIgnore;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.ObjectUtils;

/**
 * Criteria for location data tied to a source.
 *
 * @author matt
 * @version 2.2
 */
public class SourceLocationFilter implements Serializable, SourceLocation {

	@Serial
	private static final long serialVersionUID = 5979398734497676907L;

	private @Nullable Long id;
	private @Nullable String source;
	private SolarLocation location;
	private @Nullable List<MutableSortDescriptor> sorts;
	private @Nullable Long offset;
	private @Nullable Integer max;

	/**
	 * Default constructor.
	 */
	public SourceLocationFilter() {
		this(null, null);
	}

	/**
	 * Construct with criteria parameters.
	 *
	 * @param source
	 *        the source name
	 * @param locationName
	 *        the location name
	 */
	public SourceLocationFilter(@Nullable String source, @Nullable String locationName) {
		this.source = source;
		this.location = new SolarLocation();
		this.location.setName(locationName);
	}

	@Override
	public String toString() {
		return "SourceLocationFilter{source=" + source + ",location=" + location + "}";
	}

	/**
	 * Change values that are non-null but empty to null.
	 *
	 * <p>
	 * This method is helpful for web form submission, to remove filter values
	 * that are empty and would otherwise try to match on empty string values.
	 * </p>
	 */
	public void removeEmptyValues() {
		if ( !StringUtils.hasText(source) ) {
			source = null;
		}
		if ( location != null ) {
			location.removeEmptyValues();
		}
	}

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> filter = new LinkedHashMap<>();
		if ( source != null ) {
			filter.put("source", source);
		}
		if ( location != null ) {
			filter.putAll(location.getFilter());
		}
		return filter;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.3
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((max == null) ? 0 : max.hashCode());
		result = prime * result + ((offset == null) ? 0 : offset.hashCode());
		result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.3
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( (obj == null) || !(obj instanceof SourceLocationFilter other) ) {
			return false;
		}
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		if ( location == null ) {
			if ( other.location != null ) {
				return false;
			}
		} else if ( !location.equals(other.location) ) {
			return false;
		}
		if ( max == null ) {
			if ( other.max != null ) {
				return false;
			}
		} else if ( !max.equals(other.max) ) {
			return false;
		}
		if ( offset == null ) {
			if ( other.offset != null ) {
				return false;
			}
		} else if ( !offset.equals(other.offset) ) {
			return false;
		}
		if ( sorts == null ) {
			if ( other.sorts != null ) {
				return false;
			}
		} else if ( !sorts.equals(other.sorts) ) {
			return false;
		}
		if ( source == null ) {
			return other.source == null;
		}
		return source.equals(other.source);
	}

	@SerializeIgnore
	public final @Nullable String getLocationName() {
		return location.getName();
	}

	public final void setLocationName(@Nullable String locationName) {
		location.setName(locationName);
	}

	@SerializeIgnore
	public final @Nullable String getSourceName() {
		return getSource();
	}

	public final void setSourceName(@Nullable String sourceName) {
		setSource(sourceName);
	}

	@Override
	public final @Nullable String getSource() {
		return source;
	}

	public final void setSource(@Nullable String source) {
		this.source = source;
	}

	@Override
	public final SolarLocation getLocation() {
		return location;
	}

	public final void setLocation(SolarLocation location) {
		this.location = ObjectUtils.requireNonNullArgument(location, "location");
	}

	public final List<SortDescriptor> getSortDescriptors() {
		if ( sorts == null ) {
			return new ArrayList<>(2);
		}
		return new ArrayList<>(sorts);
	}

	public final @Nullable String getTimeZoneId() {
		return (location == null ? null : location.getTimeZoneId());
	}

	public final void setTimeZoneId(@Nullable String timeZoneId) {
		location.setTimeZoneId(timeZoneId);
	}

	public final @Nullable List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	public final void setSorts(@Nullable List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	public final @Nullable Long getOffset() {
		return offset;
	}

	public final void setOffset(@Nullable Long offset) {
		this.offset = offset;
	}

	public final @Nullable Integer getMax() {
		return max;
	}

	public final void setMax(@Nullable Integer max) {
		this.max = max;
	}

	@Override
	public final @Nullable Long getId() {
		return id;
	}

	public final void setId(@Nullable Long id) {
		this.id = id;
	}

}
