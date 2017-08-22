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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Criteria for location data tied to a source.
 * 
 * @author matt
 * @version 1.3
 */
public class SourceLocationFilter implements Serializable, SourceLocation {

	private static final long serialVersionUID = 5979398734497676907L;

	private Long id;
	private String source;
	private SolarLocation location;
	private List<MutableSortDescriptor> sorts;
	private Integer offset;
	private Integer max;

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
	public SourceLocationFilter(String source, String locationName) {
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
		Map<String, Object> filter = new LinkedHashMap<String, Object>();
		if ( source != null ) {
			filter.put("source", source);
		}
		if ( location != null ) {
			filter.putAll(location.getFilter());
		}
		return filter;
	}

	@SerializeIgnore
	public String getLocationName() {
		return (this.location == null ? null : this.location.getName());
	}

	public void setLocationName(String locationName) {
		if ( this.location == null ) {
			this.location = new SolarLocation();
		}
		this.location.setName(locationName);
	}

	@SerializeIgnore
	public String getSourceName() {
		return getSource();
	}

	public void setSourceName(String sourceName) {
		setSource(sourceName);
	}

	@Override
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public SolarLocation getLocation() {
		return location;
	}

	public void setLocation(SolarLocation location) {
		this.location = location;
	}

	public List<SortDescriptor> getSortDescriptors() {
		if ( sorts == null ) {
			return Collections.emptyList();
		}
		return new ArrayList<SortDescriptor>(sorts);
	}

	public String getTimeZoneId() {
		return (location == null ? null : location.getTimeZoneId());
	}

	public void setTimeZoneId(String timeZoneId) {
		if ( location == null ) {
			location = new SolarLocation();
		}
		location.setTimeZoneId(timeZoneId);
	}

	public List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	public void setSorts(List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof SourceLocationFilter) ) {
			return false;
		}
		SourceLocationFilter other = (SourceLocationFilter) obj;
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
			if ( other.source != null ) {
				return false;
			}
		} else if ( !source.equals(other.source) ) {
			return false;
		}
		return true;
	}

}
