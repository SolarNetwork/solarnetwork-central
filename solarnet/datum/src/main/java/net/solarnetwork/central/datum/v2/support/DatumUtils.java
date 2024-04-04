/* ==================================================================
 * DatumUtils.java - 24/11/2020 10:46:42 am
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.support;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.CombiningFilter;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRollupFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.MostRecentFilter;
import net.solarnetwork.central.datum.domain.ReadingTypeFilter;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.domain.SourceFilter;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.datum.domain.UserFilter;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.PropertyNameCriteria;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.DateRangeFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.LocalDateRangeFilter;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.NodeFilter;
import net.solarnetwork.central.domain.NodeMappingFilter;
import net.solarnetwork.central.domain.OptimizedQueryFilter;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SourceMappingFilter;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.DatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.SearchFilter.CompareOperator;
import net.solarnetwork.util.SearchFilter.LogicOperator;

/**
 * General datum utility methods.
 *
 * @author matt
 * @version 2.8
 * @since 2.8
 */
public final class DatumUtils {

	private DatumUtils() {
		// don't construct me
	}

	/**
	 * Convert a filter to a datum criteria.
	 *
	 * @param filter
	 *        the filter to convert
	 * @return the criteria, or {@literal null} if {@code filter} is
	 *         {@literal null}
	 */
	public static BasicDatumCriteria criteriaFromFilter(Filter filter) {
		return criteriaFromFilter(filter, null, null, null);
	}

	/**
	 * Convert a filter to a datum criteria, with sort and pagination support.
	 *
	 * @param filter
	 *        the filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        the optional offset
	 * @param max
	 *        the optional max
	 * @return the criteria, or {@literal null} if {@code filter} is
	 *         {@literal null}
	 */
	public static BasicDatumCriteria criteriaFromFilter(Filter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		if ( filter == null ) {
			return null;
		}
		BasicDatumCriteria c = new BasicDatumCriteria();
		List<? extends SortDescriptor> s = null;
		Integer m = max;
		Integer o = offset;
		String[] tags = null;

		if ( filter instanceof DatumFilterCommand f ) {
			// most common
			c.setNodeIds(f.getNodeIds());
			c.setLocationIds(f.getLocationIds());
			c.setLocation(locationFromFilter(f.getLocation()));
			c.setSourceIds(f.getSourceIds());
			c.setUserIds(f.getUserIds());
			c.setAggregation(f.getAggregation());
			c.setPartialAggregation(f.getPartialAggregation());
			c.setReadingType(f.getReadingType());
			c.setStartDate(f.getStartDate());
			c.setEndDate(f.getEndDate());
			c.setLocalStartDate(f.getLocalStartDate());
			c.setLocalEndDate(f.getLocalEndDate());
			c.setMostRecent(f.isMostRecent());
			c.setDatumRollupTypes(f.getDatumRollupTypes());
			c.setWithoutTotalResultsCount(f.isWithoutTotalResultsCount());
			c.setCombiningType(f.getCombiningType());
			c.setObjectIdMappings(f.getNodeIdMappings());
			c.setSourceIdMappings(f.getSourceIdMappings());
			c.setSearchFilter(f.getMetadataFilter());
			c.setPropertyNames(f.getPropertyNames());
			c.setInstantaneousPropertyNames(f.getInstantaneousPropertyNames());
			c.setAccumulatingPropertyNames(f.getAccumulatingPropertyNames());
			c.setStatusPropertyNames(f.getStatusPropertyNames());
			tags = f.getTags();
			if ( s == null || s.isEmpty() ) {
				s = f.getSorts();
			}
			if ( m == null ) {
				m = f.getMax();
			}
			if ( o == null ) {
				o = f.getOffset();
			}
		} else if ( filter instanceof StreamDatumFilterCommand f ) {
			// most common
			c.setStreamIds(f.getStreamIds());
			Long[] objIds = f.getObjectIds();
			if ( objIds != null ) {
				if ( f.getKind() == ObjectDatumKind.Location ) {
					c.setLocationIds(objIds);
				} else {
					c.setNodeIds(objIds);
				}
			}
			c.setSourceIds(f.getSourceIds());
			c.setUserIds(f.getUserIds());
			c.setAggregation(f.getAggregation());
			c.setPartialAggregation(f.getPartialAggregation());
			c.setStartDate(f.getStartDate());
			c.setEndDate(f.getEndDate());
			c.setLocalStartDate(f.getLocalStartDate());
			c.setLocalEndDate(f.getLocalEndDate());
			c.setMostRecent(f.isMostRecent());
			c.setDatumRollupTypes(f.getDatumRollupTypes());
			c.setWithoutTotalResultsCount(f.isWithoutTotalResultsCount());
			c.setCombiningType(f.getCombiningType());
			c.setObjectIdMappings(f.getNodeIdMappings());
			c.setSourceIdMappings(f.getSourceIdMappings());
			c.setSearchFilter(f.getMetadataFilter());
			tags = f.getTags();
			if ( s == null || s.isEmpty() ) {
				s = f.getSorts();
			}
			if ( m == null ) {
				m = f.getMax();
			}
			if ( o == null ) {
				o = f.getOffset();
			}
		} else {
			if ( filter instanceof StreamDatumFilter f ) {
				c.setStreamIds(f.getStreamIds());
				Long[] objIds = f.getObjectIds();
				if ( objIds != null ) {
					if ( f.getKind() == ObjectDatumKind.Location ) {
						c.setLocationIds(objIds);
					} else {
						c.setNodeIds(objIds);
					}
				}
			}
			if ( filter instanceof NodeFilter f ) {
				c.setNodeIds(f.getNodeIds());
			}
			if ( filter instanceof GeneralLocationDatumMetadataFilter f ) {
				c.setLocationIds(f.getLocationIds());
				c.setLocation(locationFromFilter(f.getLocation()));
				tags = f.getTags();
			} else if ( filter instanceof GeneralNodeDatumMetadataFilter f ) {
				tags = f.getTags();
			}
			if ( filter instanceof SourceFilter f ) {
				c.setSourceIds(f.getSourceIds());
			}
			if ( filter instanceof UserFilter f ) {
				c.setUserIds(f.getUserIds());
			}
			if ( filter instanceof AggregationFilter f ) {
				c.setAggregation(f.getAggregation());
				c.setPartialAggregation(f.getPartialAggregation());
			}
			if ( filter instanceof ReadingTypeFilter f ) {
				c.setReadingType(f.getReadingType());
			}
			if ( filter instanceof DateRangeFilter f ) {
				c.setStartDate(f.getStartDate());
				c.setEndDate(f.getEndDate());
			}
			if ( filter instanceof LocalDateRangeFilter f ) {
				c.setLocalStartDate(f.getLocalStartDate());
				c.setLocalEndDate(f.getLocalEndDate());
			}
			if ( filter instanceof MostRecentFilter f ) {
				c.setMostRecent(f.isMostRecent());
			}
			if ( filter instanceof DatumRollupFilter f ) {
				c.setDatumRollupTypes(f.getDatumRollupTypes());
			}
			if ( filter instanceof OptimizedQueryFilter f ) {
				c.setWithoutTotalResultsCount(f.isWithoutTotalResultsCount());
			}
			if ( filter instanceof CombiningFilter f ) {
				c.setCombiningType(f.getCombiningType());
			}
			if ( filter instanceof NodeMappingFilter f ) {
				c.setObjectIdMappings(f.getNodeIdMappings());
			}
			if ( filter instanceof SourceMappingFilter f ) {
				c.setSourceIdMappings(f.getSourceIdMappings());
			}
			if ( filter instanceof PropertyNameCriteria f ) {
				c.setPropertyNames(f.getPropertyNames());
				c.setInstantaneousPropertyNames(f.getInstantaneousPropertyNames());
				c.setAccumulatingPropertyNames(f.getAccumulatingPropertyNames());
				c.setStatusPropertyNames(f.getStatusPropertyNames());
			}
		}

		if ( s != null && !s.isEmpty() ) {
			List<SortDescriptor> sorts = new ArrayList<>(s.size());
			for ( SortDescriptor sd : s ) {
				sorts.add(new SimpleSortDescriptor(sd.getSortKey(), sd.isDescending()));
			}
			c.setSorts(sorts);
		} else if ( sortDescriptors != null && !sortDescriptors.isEmpty() ) {
			c.setSorts(sortDescriptors);
		}
		c.setMax(m);
		c.setOffset(o);

		if ( tags != null && tags.length > 0 ) {
			// turn tags into metadata query
			Map<String, Object> map = new LinkedHashMap<>(tags.length);
			for ( int i = 0; i < tags.length; i++ ) {
				map.put(String.valueOf(i), new SearchFilter("/t", tags[i], CompareOperator.EQUAL));
			}
			SearchFilter sf = new SearchFilter(map, LogicOperator.OR);
			// support merging into existing metadata query? for now assume tags take over
			c.setSearchFilter(sf.asLDAPSearchFilterString());
		}

		return c;
	}

	/**
	 * Convert a location filter to a location criteria.
	 *
	 * @param location
	 *        the location
	 * @return the criteria, or {@literal null} if {@code filter} is
	 *         {@literal null} or has only empty values
	 */
	public static SimpleLocation locationFromFilter(Location location) {
		if ( location == null ) {
			return null;
		}
		SimpleLocation l = new SimpleLocation();
		l.setName(location.getName());
		l.setCountry(location.getCountry());
		l.setRegion(location.getRegion());
		l.setStateOrProvince(location.getStateOrProvince());
		l.setLocality(location.getLocality());
		l.setPostalCode(location.getPostalCode());
		l.setStreet(location.getStreet());
		l.setLatitude(location.getLatitude());
		l.setLongitude(location.getLongitude());
		l.setElevation(location.getElevation());
		l.setTimeZoneId(location.getTimeZoneId());

		l.removeEmptyValues();
		if ( !l.hasLocationCriteria() ) {
			return null;
		}

		return l;
	}

	/**
	 * Get a copy of a {@link ObjectStreamCriteria} with any date values
	 * removed.
	 *
	 * @param criteria
	 *        the criteria
	 * @return the new criteria
	 */
	public static ObjectStreamCriteria criteriaWithoutDates(ObjectStreamCriteria criteria) {
		requireNonNullArgument(criteria, "criteria");
		BasicDatumCriteria result = null;
		if ( criteria instanceof BasicDatumCriteria ) {
			result = ((BasicDatumCriteria) criteria).clone();
		} else {
			result = toBasicDatumCriteria(criteria);
		}
		result.setStartDate(null);
		result.setEndDate(null);
		result.setLocalStartDate(null);
		result.setLocalEndDate(null);
		return result;
	}

	/**
	 * Get a {@link BasicDatumCriteria} instance from a
	 * {@link ObjectStreamCriteria} instance.
	 *
	 * @param criteria
	 *        the criteria to convert
	 * @return the basic datum criteria, or {@literal null} if {@code criteria}
	 *         is {@literal null}; if {@code criteria} is already a
	 *         {@link BasicDatumCriteria} instance it will be returned directly
	 * @since 2.1
	 */
	public static BasicDatumCriteria toBasicDatumCriteria(ObjectStreamCriteria criteria) {
		BasicDatumCriteria c;
		if ( criteria == null ) {
			return null;
		} else if ( criteria instanceof BasicDatumCriteria ) {
			c = (BasicDatumCriteria) criteria;
		} else {
			c = new BasicDatumCriteria();
			c.setAggregation(criteria.getAggregation());
			c.setLocationIds(criteria.getLocationIds());
			c.setLocation(SimpleLocation.locationValue(criteria.getLocation()));
			c.setMax(criteria.getMax());
			c.setNodeIds(criteria.getNodeIds());
			c.setObjectKind(criteria.getObjectKind());
			c.setOffset(criteria.getOffset());
			c.setPartialAggregation(criteria.getPartialAggregation());
			c.setSorts(criteria.getSorts());
			c.setSourceIds(criteria.getSourceIds());
			c.setStreamIds(criteria.getStreamIds());
			c.setTokenIds(criteria.getTokenIds());
			c.setUserIds(criteria.getUserIds());
			c.setCombiningType(criteria.getCombiningType());
			c.setObjectIdMappings(criteria.getObjectIdMappings());
			c.setSourceIdMappings(criteria.getSourceIdMappings());
			c.setStartDate(criteria.getStartDate());
			c.setEndDate(criteria.getEndDate());
			c.setLocalStartDate(criteria.getLocalStartDate());
			c.setLocalEndDate(criteria.getLocalEndDate());
		}
		return c;
	}

	/**
	 * Convert a "type" property to an {@link Aggregation}.
	 *
	 * @param criteria
	 *        the criteria to extract the type from; if instance of
	 *        {@link DatumFilter} then the "type" value will be converted to an
	 *        aggregation
	 * @return the aggregation, or {@literal null} if none
	 */
	public static Aggregation aggregationForType(Filter criteria) {
		Aggregation result = null;
		if ( criteria instanceof DatumFilter ) {
			String type = ((DatumFilter) criteria).getType();
			if ( "h".equalsIgnoreCase(type) ) {
				result = Aggregation.Hour;
			} else if ( "d".equalsIgnoreCase(type) ) {
				result = Aggregation.Day;
			} else if ( "m".equalsIgnoreCase(type) ) {
				result = Aggregation.Month;
			}
		}
		return result;
	}

	/**
	 * Populate the aggregation value of a {@link BasicDatumCriteria} from a
	 * "type" property value.
	 *
	 * @param criteria
	 *        the criteria to extract the type from
	 * @param filter
	 *        the criteria to populate the {@link Aggregation} on
	 */
	public static void populateAggregationType(Filter criteria, BasicDatumCriteria filter) {
		Aggregation agg = aggregationForType(criteria);
		if ( agg != null ) {
			filter.setAggregation(agg);
		}
	}

	/**
	 * Populate a {@link DatumSamples} instance with property values.
	 *
	 * @param s
	 *        the samples instance to populate
	 * @param propType
	 *        the property type
	 * @param propNames
	 *        the property names
	 * @param propValues
	 *        the property values
	 */
	public static void populateGeneralDatumSamples(DatumSamples s, DatumSamplesType propType,
			String[] propNames, Object[] propValues) {
		if ( propNames != null && propValues != null && propValues.length <= propNames.length ) {
			for ( int i = 0, len = propValues.length; i < len; i++ ) {
				s.putSampleValue(propType, propNames[i], propValues[i]);
			}
		}
	}

	/**
	 * Populate a {@link DatumSamples} instance with property values.
	 *
	 * @param s
	 *        the samples instance to populate
	 * @param props
	 *        the properties
	 * @param meta
	 *        the metadata
	 */
	public static void populateGeneralDatumSamples(DatumSamples s, DatumProperties props,
			DatumStreamMetadata meta) {
		populateGeneralDatumSamples(s, DatumSamplesType.Instantaneous,
				meta.propertyNamesForType(DatumSamplesType.Instantaneous), props.getInstantaneous());
		populateGeneralDatumSamples(s, DatumSamplesType.Accumulating,
				meta.propertyNamesForType(DatumSamplesType.Accumulating), props.getAccumulating());
		populateGeneralDatumSamples(s, DatumSamplesType.Status,
				meta.propertyNamesForType(DatumSamplesType.Status), props.getStatus());
		if ( props.getTagsLength() > 0 ) {
			Set<String> tags = new LinkedHashSet<>(props.getTagsLength());
			for ( String t : props.getTags() ) {
				tags.add(t);
			}
			s.setTags(tags);
		}
	}

	/**
	 * Populate a {@link DatumSamples} instance with instantaneous property
	 * statistics.
	 *
	 * <p>
	 * This will populate {@code _min} and {@code _max} instantaneous properties
	 * for all available instantaneous property statistic values.
	 * </p>
	 *
	 * @param s
	 *        the samples instance to populate
	 * @param stats
	 *        the statistics
	 * @param meta
	 *        the metadata
	 */
	public static void populateGeneralDatumSamplesInstantaneousStatistics(DatumSamples s,
			DatumPropertiesStatistics stats, ObjectDatumStreamMetadata meta) {
		final int len = stats.getInstantaneousLength();
		if ( len < 1 ) {
			return;
		}
		BigDecimal[][] iStats = stats.getInstantaneous();
		String[] propNames = meta.propertyNamesForType(DatumSamplesType.Instantaneous);
		if ( propNames == null ) {
			return;
		}
		final int max = Math.min(len, propNames.length);
		for ( int i = 0; i < max; i++ ) {
			BigDecimal[] propStats = iStats[i]; // count, min, max
			if ( propStats == null || propStats.length < 3 ) {
				continue;
			}
			s.putSampleValue(DatumSamplesType.Instantaneous, propNames[i] + "_min", propStats[1]);
			s.putSampleValue(DatumSamplesType.Instantaneous, propNames[i] + "_max", propStats[2]);
		}
	}

	/**
	 * Populate a {@link DatumSamples} instance with accumulating property
	 * statistics.
	 *
	 * <p>
	 * This will populate {@code _start} and {@code _end} instantaneous
	 * properties for all available accumulating property statistic values, and
	 * an accumulating property value for the difference statistic value.
	 * </p>
	 *
	 * @param s
	 *        the samples instance to populate
	 * @param stats
	 *        the statistics
	 * @param meta
	 *        the metadata
	 */
	public static void populateGeneralDatumSamplesAccumulatingStatistics(DatumSamples s,
			DatumPropertiesStatistics stats, ObjectDatumStreamMetadata meta) {
		final int len = stats.getAccumulatingLength();
		if ( len < 1 ) {
			return;
		}
		BigDecimal[][] aStats = stats.getAccumulating();
		String[] propNames = meta.propertyNamesForType(DatumSamplesType.Accumulating);
		if ( propNames == null ) {
			return;
		}
		final int max = Math.min(len, propNames.length);
		for ( int i = 0; i < max; i++ ) {
			BigDecimal[] propStats = aStats[i]; // diff, start, end
			if ( propStats == null || propStats.length < 3 ) {
				continue;
			}
			s.putSampleValue(DatumSamplesType.Accumulating, propNames[i], propStats[0]);
			s.putSampleValue(DatumSamplesType.Instantaneous, propNames[i] + "_start", propStats[1]);
			s.putSampleValue(DatumSamplesType.Instantaneous, propNames[i] + "_end", propStats[2]);
		}
	}

	/**
	 * Create a new {@link ReportingGeneralNodeDatum} out of a {@link Datum}.
	 *
	 * @param datum
	 *        the datum to convert
	 * @param meta
	 *        the datum metadata
	 * @return the general datum, or {@literal null} if {@code datum} is
	 *         {@literal null} or {@link ObjectDatumStreamMetadata#getKind()} is
	 *         not {@code Node}
	 */
	public static ReportingGeneralNodeDatum toGeneralNodeDatum(Datum datum,
			ObjectDatumStreamMetadata meta) {
		if ( datum == null || meta.getKind() != ObjectDatumKind.Node ) {
			return null;
		}
		ZoneId zone = meta.getTimeZoneId() != null ? ZoneId.of(meta.getTimeZoneId()) : ZoneOffset.UTC;

		// use ReportingGeneralNodeDatum to support localDateTime property
		ReportingGeneralNodeDatum gnd = new ReportingGeneralNodeDatum();

		gnd.setCreated(datum.getTimestamp());
		gnd.setLocalDateTime(datum.getTimestamp().atZone(zone).toLocalDateTime());
		gnd.setNodeId(meta.getObjectId());
		gnd.setSourceId(meta.getSourceId());

		DatumSamples s = new DatumSamples();
		// populate instantaneous statistics data if available
		if ( datum instanceof AggregateDatum ) {
			DatumPropertiesStatistics stats = ((AggregateDatum) datum).getStatistics();
			if ( stats != null ) {
				populateGeneralDatumSamplesInstantaneousStatistics(s, stats, meta);
			}
		}
		// populate normal data
		DatumProperties props = datum.getProperties();
		if ( props != null ) {
			populateGeneralDatumSamples(s, props, meta);
		}
		if ( datum instanceof ReadingDatum ) {
			ReadingDatum read = (ReadingDatum) datum;

			// populate reading (accumulating) data from stats when available
			DatumPropertiesStatistics stats = read.getStatistics();
			if ( stats != null ) {
				if ( s.getA() != null ) {
					s.getA().clear();
				}
				populateGeneralDatumSamplesAccumulatingStatistics(s, stats, meta);
				if ( read.getEndTimestamp() != null ) {
					s.putStatusSampleValue("timeZone", meta.getTimeZoneId());
					s.putStatusSampleValue("endDate",
							DateUtils.ISO_DATE_TIME_ALT_UTC.format(read.getEndTimestamp()));
					s.putStatusSampleValue("localEndDate",
							DateUtils.ISO_DATE_TIME_ALT_UTC.format(read.getEndTimestamp()
									.atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
				}
			}
		}
		if ( !s.isEmpty() ) {
			gnd.setSamples(s);
		}

		return gnd;
	}

	/**
	 * Create a new {@link GeneralLocationDatumMetadataMatch} out of an
	 * {@link ObjectDatumStreamMetadata}.
	 *
	 * @param meta
	 *        the metadata to convert
	 * @return the general metadata, or {@literal null} if {@code meta} is
	 *         {@literal null}
	 */
	public static GeneralNodeDatumMetadataMatch toGeneralNodeDatumMetadataMatch(
			ObjectDatumStreamMetadata meta) {
		if ( meta == null ) {
			return null;
		}
		GeneralNodeDatumMetadataMatch m = new GeneralNodeDatumMetadataMatch();
		m.setNodeId(meta.getObjectId());
		m.setSourceId(meta.getSourceId());
		m.setMetaJson(meta.getMetaJson());
		return m;
	}

	/**
	 * Create a new {@link GeneralNodeDatumAuxiliary} out of a
	 * {@link DatumAuxiliary}.
	 *
	 * @param datum
	 *        the datum to convert
	 * @param meta
	 *        the metadata
	 * @return the general datum auxiliary, or {@literal null} if either
	 *         {@code datum} or {@code meta} is {@literal null}
	 */
	public static GeneralNodeDatumAuxiliary toGeneralNodeDatumAuxiliary(DatumAuxiliary datum,
			ObjectDatumStreamMetadata meta) {
		if ( datum == null || meta == null ) {
			return null;
		}
		GeneralNodeDatumAuxiliary aux = new GeneralNodeDatumAuxiliary();
		populate(datum, meta, aux);
		return aux;
	}

	private static void populate(DatumAuxiliary datum, ObjectDatumStreamMetadata meta,
			GeneralNodeDatumAuxiliary aux) {
		aux.setNodeId(meta.getObjectId());
		aux.setSourceId(meta.getSourceId());
		aux.setCreated(datum.getTimestamp());
		aux.setType(datum.getType());
		if ( datum.getSamplesFinal() != null ) {
			DatumSamples s = new DatumSamples();
			s.setAccumulating(datum.getSamplesFinal().getAccumulating());
			aux.setSamplesFinal(s);
		}
		if ( datum.getSamplesStart() != null ) {
			DatumSamples s = new DatumSamples();
			s.setAccumulating(datum.getSamplesStart().getAccumulating());
			aux.setSamplesStart(s);
		}
		aux.setMeta(datum.getMetadata());
		if ( datum instanceof DatumAuxiliaryEntity ) {
			aux.setUpdated(((DatumAuxiliaryEntity) datum).getUpdated());
		}
	}

	/**
	 * Create a new {@link GeneralNodeDatumAuxiliaryFilterMatch} out of a
	 * {@link DatumAuxiliary}.
	 *
	 * @param datum
	 *        the datum
	 * @param meta
	 *        the metadata
	 * @return the match
	 */
	public static GeneralNodeDatumAuxiliaryFilterMatch toGeneralNodeDatumAuxiliaryFilterMatch(
			DatumAuxiliary datum, ObjectDatumStreamMetadata meta) {
		GeneralNodeDatumAuxiliaryMatch aux = new GeneralNodeDatumAuxiliaryMatch();
		populate(datum, meta, aux);
		aux.setLocalDateTime(
				datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime());
		return aux;
	}

	/**
	 * Create a new {@link ReportingGeneralLocationDatum} out of a
	 * {@link Datum}.
	 *
	 * @param datum
	 *        the datum to convert
	 * @param meta
	 *        the datum metadata
	 * @return the general datum, or {@literal null} if {@code datum} is
	 *         {@literal null} or {@link ObjectDatumStreamMetadata#getKind()} is
	 *         not {@code Location}
	 */
	public static ReportingGeneralLocationDatum toGeneralLocationDatum(Datum datum,
			ObjectDatumStreamMetadata meta) {
		if ( datum == null || meta.getKind() != ObjectDatumKind.Location ) {
			return null;
		}
		ZoneId zone = meta.getTimeZoneId() != null ? ZoneId.of(meta.getTimeZoneId()) : ZoneOffset.UTC;

		// use ReportingGeneralLocationDatum to support localDateTime property
		ReportingGeneralLocationDatum gnd = new ReportingGeneralLocationDatum();
		gnd.setCreated(datum.getTimestamp());
		gnd.setLocalDateTime(datum.getTimestamp().atZone(zone).toLocalDateTime());
		gnd.setLocationId(meta.getObjectId());
		gnd.setSourceId(meta.getSourceId());

		DatumSamples s = new DatumSamples();
		// populate normal data
		if ( datum instanceof AggregateDatum ) {
			DatumPropertiesStatistics stats = ((AggregateDatum) datum).getStatistics();
			populateGeneralDatumSamplesInstantaneousStatistics(s, stats, meta);
		}
		DatumProperties props = datum.getProperties();
		if ( props != null ) {
			populateGeneralDatumSamples(s, props, meta);
		}
		if ( datum instanceof ReadingDatum ) {
			// populate reading (accumulating) data from stats
			if ( s.getA() != null ) {
				s.getA().clear();
			}
			DatumPropertiesStatistics stats = ((ReadingDatum) datum).getStatistics();
			populateGeneralDatumSamplesAccumulatingStatistics(s, stats, meta);
		}
		if ( !s.isEmpty() ) {
			gnd.setSamples(s);
		}

		return gnd;
	}

	/**
	 * Create a new {@link GeneralLocationDatumMetadataMatch} out of an
	 * {@link ObjectDatumStreamMetadata}.
	 *
	 * @param meta
	 *        the metadata to convert
	 * @return the general metadata, or {@literal null} if {@code meta} is
	 *         {@literal null}
	 */
	public static GeneralLocationDatumMetadataMatch toGeneralLocationDatumMetadataMatch(
			ObjectDatumStreamMetadata meta) {
		if ( meta == null ) {
			return null;
		}
		GeneralLocationDatumMetadataMatch m = new GeneralLocationDatumMetadataMatch();
		m.setLocationId(meta.getObjectId());
		m.setSourceId(meta.getSourceId());
		m.setMetaJson(meta.getMetaJson());

		net.solarnetwork.domain.Location l = meta.getLocation();
		if ( l != null ) {
			m.setLocation(toSolarLocation(l));
		}
		return m;
	}

	/**
	 * Create a new {@link SolarLocation} out of a
	 * {@link net.solarnetwork.domain.Location}.
	 *
	 * @param l
	 *        the location to convert
	 * @return the solar location
	 * @since 1.5
	 */
	public static SolarLocation toSolarLocation(net.solarnetwork.domain.Location l) {
		if ( l instanceof SolarLocation ) {
			return (SolarLocation) l;
		}
		SolarLocation sl = new SolarLocation();
		sl.setCountry(l.getCountry());
		sl.setElevation(l.getElevation());
		sl.setLatitude(l.getLatitude());
		sl.setLocality(l.getLocality());
		sl.setLongitude(l.getLongitude());
		sl.setName(l.getName());
		sl.setPostalCode(l.getPostalCode());
		sl.setRegion(l.getRegion());
		sl.setStateOrProvince(l.getStateOrProvince());
		sl.setStreet(l.getStreet());
		sl.setTimeZoneId(l.getTimeZoneId());
		return sl;
	}

	/**
	 * Truncate a local date based on an {@link Aggregation}.
	 *
	 * @param date
	 *        the date to truncate
	 * @param agg
	 *        the aggregation to truncate to
	 * @return the new date
	 * @throws IllegalArgumentException
	 *         if {@code agg} is not supported
	 */
	public static LocalDateTime truncateDate(LocalDateTime date, Aggregation agg) {
		switch (agg) {
			case Hour:
				return date.truncatedTo(ChronoUnit.HOURS);

			case Day:
				return date.truncatedTo(ChronoUnit.DAYS);

			case Month:
				return date.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);

			case Year:
				return date.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);

			default:
				throw new IllegalArgumentException(format("The aggregation %s is not supported.", agg));
		}
	}

	/** A UUID namespace for URLs. */
	public static final UUID UUID_NAMESPACE_URL = UUID
			.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");

	/**
	 * Generate a virtual stream ID.
	 *
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @return the UUID, or {@literal null} if any argument is {@literal null}
	 */
	public static UUID virtualStreamId(Long objectId, String sourceId) {
		if ( objectId == null || sourceId == null ) {
			return null;
		}
		if ( !sourceId.isEmpty() && sourceId.charAt(0) == '/' ) {
			sourceId = sourceId.substring(1);
		}
		return v5UUID(UUID_NAMESPACE_URL, format("objid://obj/%d/%s", objectId, sourceId));
	}

	/**
	 * Generate a v5 UUID.
	 *
	 * <p>
	 * Adapted from https://stackoverflow.com/a/40230410
	 * </p>
	 *
	 * @param namespace
	 *        a UUID namespace, e.g. {@link #UUID_NAMESPACE_URL}
	 * @param name
	 *        the name value
	 * @return the UUID
	 */
	public static UUID v5UUID(UUID namespace, String name) {
		return v5UUID(namespace, name.getBytes(Charset.forName("UTF-8")));
	}

	private static UUID v5UUID(UUID namespace, byte[] name) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("SHA-1 not supported.");
		}
		md.update(toBytes(namespace));
		md.update(name);
		byte[] sha1Bytes = md.digest();
		sha1Bytes[6] &= (byte) 0x0f; /* clear version */
		sha1Bytes[6] |= (byte) 0x50; /* set to version 5 */
		sha1Bytes[8] &= (byte) 0x3f; /* clear variant */
		sha1Bytes[8] |= (byte) 0x80; /* set to IETF variant */
		return fromBytes(sha1Bytes);
	}

	private static UUID fromBytes(byte[] data) {
		// Based on the private UUID(bytes[]) constructor
		long msb = 0;
		long lsb = 0;
		assert data.length >= 16;
		for ( int i = 0; i < 8; i++ )
			msb = (msb << 8) | (data[i] & 0xff);
		for ( int i = 8; i < 16; i++ )
			lsb = (lsb << 8) | (data[i] & 0xff);
		return new UUID(msb, lsb);
	}

	private static byte[] toBytes(UUID uuid) {
		// inverted logic of fromBytes()
		byte[] out = new byte[16];
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		for ( int i = 0; i < 8; i++ )
			out[i] = (byte) ((msb >> ((7 - i) * 8)) & 0xff);
		for ( int i = 8; i < 16; i++ )
			out[i] = (byte) ((lsb >> ((15 - i) * 8)) & 0xff);
		return out;
	}

	/**
	 * Convert a {@link DatumRecordCounts} to a legacy
	 * {@link net.solarnetwork.central.datum.domain.DatumRecordCounts} instance.
	 *
	 * @param counts
	 *        the counts to convert
	 * @return the legacy instace, or {@literal null} if {@code counts} is
	 *         {@literal null}
	 */
	public static net.solarnetwork.central.datum.domain.DatumRecordCounts toRecordCounts(
			DatumRecordCounts counts) {
		if ( counts == null ) {
			return null;
		}
		net.solarnetwork.central.datum.domain.DatumRecordCounts c = new net.solarnetwork.central.datum.domain.DatumRecordCounts(
				counts.getDatumCount(), counts.getDatumHourlyCount(), counts.getDatumDailyCount(),
				counts.getDatumMonthlyCount());
		c.setDate(counts.getTimestamp());
		return c;
	}

	/**
	 * Convert an {@link ObjectDatumKind} to a
	 * {@link net.solarnetwork.domain.datum.ObjectDatumKind}.
	 *
	 * @param kind
	 *        the kind to convert
	 * @return the converted instance, or {@literal null} if {@code kind} is
	 *         {@literal null}
	 * @since 1.7
	 */
	public static net.solarnetwork.domain.datum.ObjectDatumKind toCommonObjectDatumKind(
			ObjectDatumKind kind) {
		switch (kind) {
			case Node:
				return net.solarnetwork.domain.datum.ObjectDatumKind.Node;

			case Location:
				return net.solarnetwork.domain.datum.ObjectDatumKind.Location;

			default:
				return null;
		}
	}

	/**
	 * Convert a {@link ObjectDatumStreamMetadata} to a
	 * {@link BasicObjectDatumStreamMetadata}.
	 *
	 * <p>
	 * <b>Note</b> that the {@link ObjectDatumStreamMetadata#getMetaJson()}
	 * value is <b>not</b> copied.
	 * </p>
	 *
	 * @param meta
	 *        the metadata to convert
	 * @return the converted instance, or {@literal null} if {@code meta} is
	 *         {@literal null}
	 * @since 1.7
	 */
	public static BasicObjectDatumStreamMetadata toCommonObjectDatumStreamMetadata(
			ObjectDatumStreamMetadata meta) {
		if ( meta == null ) {
			return null;
		}
		return new BasicObjectDatumStreamMetadata(meta.getStreamId(), meta.getTimeZoneId(),
				toCommonObjectDatumKind(meta.getKind()), meta.getObjectId(), meta.getSourceId(),
				meta.getLocation(), meta.propertyNamesForType(DatumSamplesType.Instantaneous),
				meta.propertyNamesForType(DatumSamplesType.Accumulating),
				meta.propertyNamesForType(DatumSamplesType.Status), null);
	}

	/**
	 * Convert {@code AuditDatumRollup} filter results to
	 * {@code AuditDatumRecordCounts} filter results.
	 *
	 * @param results
	 *        the results to conver
	 * @return the converted results, or {@literal null} if {@code results} is
	 *         {@literal null}
	 * @since 2.0
	 */
	public static FilterResults<AuditDatumRecordCounts, GeneralNodeDatumPK> toAuditDatumRecordCountsFilterResults(
			FilterResults<AuditDatumRollup, DatumPK> results) {
		if ( results == null ) {
			return null;
		}
		List<AuditDatumRecordCounts> counts = StreamSupport.stream(results.spliterator(), false)
				.map(e -> {
					AuditDatumRecordCounts c = new AuditDatumRecordCounts(e.getNodeId(), e.getSourceId(),
							e.getDatumCount(), e.getDatumHourlyCount(), e.getDatumDailyCount(),
							e.getDatumMonthlyCount());
					if ( e.getTimestamp() != null ) {
						c.setCreated(e.getTimestamp());
					}
					return c;
				}).collect(Collectors.toList());
		return new BasicFilterResults<>(counts, results.getTotalResults(), results.getStartingOffset(),
				results.getReturnedResultCount());
	}

}
