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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.domain.SourceFilter;
import net.solarnetwork.central.datum.domain.UserFilter;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.DateRangeFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.LocalDateRangeFilter;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.NodeFilter;
import net.solarnetwork.central.domain.NodeMappingFilter;
import net.solarnetwork.central.domain.OptimizedQueryFilter;
import net.solarnetwork.central.domain.SourceMappingFilter;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.support.SearchFilter;
import net.solarnetwork.support.SearchFilter.CompareOperator;
import net.solarnetwork.support.SearchFilter.LogicOperator;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.JodaDateUtils;

/**
 * General datum utility methods.
 * 
 * @author matt
 * @version 1.0
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
			List<net.solarnetwork.central.domain.SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		if ( filter == null ) {
			return null;
		}
		BasicDatumCriteria c = new BasicDatumCriteria();
		List<? extends net.solarnetwork.central.domain.SortDescriptor> s = sortDescriptors;
		Integer m = max;
		Integer o = offset;
		String[] tags = null;

		if ( filter instanceof DatumFilterCommand ) {
			DatumFilterCommand f = (DatumFilterCommand) filter;
			// most common
			c.setNodeIds(f.getNodeIds());
			c.setLocationIds(f.getLocationIds());
			c.setLocation(locationFromFilter(f.getLocation()));
			c.setSourceIds(f.getSourceIds());
			c.setUserIds(f.getUserIds());
			c.setAggregation(f.getAggregation());
			c.setPartialAggregation(f.getPartialAggregation());
			c.setStartDate(JodaDateUtils.fromJodaToInstant(f.getStartDate()));
			c.setEndDate(JodaDateUtils.fromJodaToInstant(f.getEndDate()));
			c.setLocalStartDate(JodaDateUtils.fromJoda(f.getLocalStartDate()));
			c.setLocalEndDate(JodaDateUtils.fromJoda(f.getLocalEndDate()));
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
			if ( filter instanceof NodeFilter ) {
				c.setNodeIds(((NodeFilter) filter).getNodeIds());
			}
			if ( filter instanceof GeneralLocationDatumMetadataFilter ) {
				GeneralLocationDatumMetadataFilter gldmf = (GeneralLocationDatumMetadataFilter) filter;
				c.setLocationIds(gldmf.getLocationIds());
				c.setLocation(locationFromFilter(gldmf.getLocation()));
				tags = gldmf.getTags();
			} else if ( filter instanceof GeneralNodeDatumMetadataFilter ) {
				tags = ((GeneralNodeDatumMetadataFilter) filter).getTags();
			}
			if ( filter instanceof SourceFilter ) {
				c.setSourceIds(((SourceFilter) filter).getSourceIds());
			}
			if ( filter instanceof UserFilter ) {
				c.setUserIds(((UserFilter) filter).getUserIds());
			}
			if ( filter instanceof AggregationFilter ) {
				AggregationFilter f = (AggregationFilter) filter;
				c.setAggregation(f.getAggregation());
				c.setPartialAggregation(f.getPartialAggregation());
			}
			if ( filter instanceof DateRangeFilter ) {
				DateRangeFilter f = (DateRangeFilter) filter;
				c.setStartDate(JodaDateUtils.fromJodaToInstant(f.getStartDate()));
				c.setEndDate(JodaDateUtils.fromJodaToInstant(f.getEndDate()));
			}
			if ( filter instanceof LocalDateRangeFilter ) {
				LocalDateRangeFilter f = (LocalDateRangeFilter) filter;
				c.setLocalStartDate(JodaDateUtils.fromJoda(f.getLocalStartDate()));
				c.setLocalEndDate(JodaDateUtils.fromJoda(f.getLocalEndDate()));
			}
			if ( filter instanceof DatumRollupFilter ) {
				c.setDatumRollupTypes(((DatumRollupFilter) filter).getDatumRollupTypes());
			}
			if ( filter instanceof OptimizedQueryFilter ) {
				c.setWithoutTotalResultsCount(
						((OptimizedQueryFilter) filter).isWithoutTotalResultsCount());
			}
			if ( filter instanceof CombiningFilter ) {
				c.setCombiningType(((CombiningFilter) filter).getCombiningType());
			}
			if ( filter instanceof NodeMappingFilter ) {
				c.setObjectIdMappings(((NodeMappingFilter) filter).getNodeIdMappings());
			}
			if ( filter instanceof SourceMappingFilter ) {
				c.setSourceIdMappings(((SourceMappingFilter) filter).getSourceIdMappings());
			}
		}

		if ( s != null && !s.isEmpty() ) {
			List<SortDescriptor> sorts = new ArrayList<>(s.size());
			for ( net.solarnetwork.central.domain.SortDescriptor sd : s ) {
				sorts.add(new SimpleSortDescriptor(sd.getSortKey(), sd.isDescending()));
			}
			c.setSorts(sorts);
		}
		c.setMax(m);
		c.setOffset(o);

		if ( tags != null && tags.length > 0 ) {
			// turn tags into metadata query
			Map<String, Object> map = new LinkedHashMap<>(tags.length);
			for ( int i = 0; i < tags.length; i++ ) {
				map.put(String.valueOf(i), new SearchFilter("t", tags[i], CompareOperator.EQUAL));
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
		ObjectStreamCriteria result = null;
		if ( criteria instanceof BasicDatumCriteria ) {
			BasicDatumCriteria clone = ((BasicDatumCriteria) criteria).clone();
			clone.setStartDate(null);
			clone.setEndDate(null);
			clone.setLocalStartDate(null);
			clone.setLocalEndDate(null);
			result = clone;
		} else {
			BasicDatumCriteria c = new BasicDatumCriteria();
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
			result = c;
		}
		return result;
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
	 * Populate a {@link GeneralDatumSamples} instance with property values.
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
	public static void populateGeneralDatumSamples(GeneralDatumSamples s,
			GeneralDatumSamplesType propType, String[] propNames, Object[] propValues) {
		if ( propNames != null && propValues != null && propValues.length <= propNames.length ) {
			for ( int i = 0, len = propValues.length; i < len; i++ ) {
				s.putSampleValue(propType, propNames[i], propValues[i]);
			}
		}
	}

	/**
	 * Populate a {@link GeneralDatumSamples} instance with property values.
	 * 
	 * @param s
	 *        the samples instance to populate
	 * @param props
	 *        the properties
	 * @param meta
	 *        the metadata
	 */
	public static void populateGeneralDatumSamples(GeneralDatumSamples s, DatumProperties props,
			DatumStreamMetadata meta) {
		populateGeneralDatumSamples(s, GeneralDatumSamplesType.Instantaneous,
				meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				props.getInstantaneous());
		populateGeneralDatumSamples(s, GeneralDatumSamplesType.Accumulating,
				meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				props.getAccumulating());
		populateGeneralDatumSamples(s, GeneralDatumSamplesType.Status,
				meta.propertyNamesForType(GeneralDatumSamplesType.Status), props.getStatus());
		if ( props.getTagsLength() > 0 ) {
			Set<String> tags = new LinkedHashSet<>(props.getTagsLength());
			for ( String t : props.getTags() ) {
				tags.add(t);
			}
			s.setTags(tags);
		}
	}

	/**
	 * Populate a {@link GeneralDatumSamples} instance with instantaneous
	 * property statistics.
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
	public static void populateGeneralDatumSamplesInstantaneousStatistics(GeneralDatumSamples s,
			DatumPropertiesStatistics stats, ObjectDatumStreamMetadata meta) {
		final int len = stats.getInstantaneousLength();
		if ( len < 1 ) {
			return;
		}
		BigDecimal[][] iStats = stats.getInstantaneous();
		String[] propNames = meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous);
		if ( propNames == null || propNames.length > len ) {
			return;
		}
		for ( int i = 0; i < len; i++ ) {
			BigDecimal[] propStats = iStats[i]; // count, min, max
			if ( propStats == null || propStats.length < 3 ) {
				continue;
			}
			s.putSampleValue(GeneralDatumSamplesType.Instantaneous, propNames[i] + "_min", propStats[1]);
			s.putSampleValue(GeneralDatumSamplesType.Instantaneous, propNames[i] + "_max", propStats[2]);
		}
	}

	/**
	 * Populate a {@link GeneralDatumSamples} instance with accumulating
	 * property statistics.
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
	public static void populateGeneralDatumSamplesAccumulatingStatistics(GeneralDatumSamples s,
			DatumPropertiesStatistics stats, ObjectDatumStreamMetadata meta) {
		final int len = stats.getAccumulatingLength();
		if ( len < 1 ) {
			return;
		}
		BigDecimal[][] aStats = stats.getAccumulating();
		String[] propNames = meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating);
		if ( propNames == null || propNames.length > len ) {
			return;
		}
		for ( int i = 0; i < len; i++ ) {
			BigDecimal[] propStats = aStats[i]; // diff, start, end
			if ( propStats == null || propStats.length < 3 ) {
				continue;
			}
			s.putSampleValue(GeneralDatumSamplesType.Accumulating, propNames[i], propStats[0]);
			s.putSampleValue(GeneralDatumSamplesType.Instantaneous, propNames[i] + "_start",
					propStats[1]);
			s.putSampleValue(GeneralDatumSamplesType.Instantaneous, propNames[i] + "_end", propStats[2]);
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
		DateTimeZone zone = meta.getTimeZoneId() != null ? DateTimeZone.forID(meta.getTimeZoneId())
				: DateTimeZone.UTC;

		// use ReportingGeneralNodeDatum to support localDateTime property
		ReportingGeneralNodeDatum gnd = new ReportingGeneralNodeDatum();

		gnd.setCreated(new DateTime(datum.getTimestamp().toEpochMilli(), zone));
		gnd.setLocalDateTime(gnd.getCreated().toLocalDateTime());
		gnd.setNodeId(meta.getObjectId());
		gnd.setSourceId(meta.getSourceId());

		GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
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
				s.getA().clear();
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
		aux.setCreated(JodaDateUtils.toJoda(datum.getTimestamp(), meta.getTimeZoneId()));
		aux.setType(datum.getType());
		if ( datum.getSamplesFinal() != null ) {
			GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
			s.setAccumulating(datum.getSamplesFinal().getAccumulating());
			aux.setSamplesFinal(s);
		}
		if ( datum.getSamplesStart() != null ) {
			GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
			s.setAccumulating(datum.getSamplesStart().getAccumulating());
			aux.setSamplesStart(s);
		}
		aux.setMeta(datum.getMetadata());
		if ( datum instanceof DatumAuxiliaryEntity ) {
			aux.setUpdated(JodaDateUtils.toJoda(((DatumAuxiliaryEntity) datum).getUpdated(),
					meta.getTimeZoneId()));
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
		aux.setLocalDateTime(JodaDateUtils
				.toJoda(datum.getTimestamp().atZone(ZoneId.of(meta.getTimeZoneId())).toLocalDateTime()));
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
		DateTimeZone zone = meta.getTimeZoneId() != null ? DateTimeZone.forID(meta.getTimeZoneId())
				: DateTimeZone.UTC;

		// use ReportingGeneralLocationDatum to support localDateTime property
		ReportingGeneralLocationDatum gnd = new ReportingGeneralLocationDatum();
		gnd.setCreated(new DateTime(datum.getTimestamp().toEpochMilli(), zone));
		gnd.setLocalDateTime(gnd.getCreated().toLocalDateTime());
		gnd.setLocationId(meta.getObjectId());
		gnd.setSourceId(meta.getSourceId());

		GeneralLocationDatumSamples s = new GeneralLocationDatumSamples();
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
			s.getA().clear();
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
		return m;
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
		sha1Bytes[6] &= 0x0f; /* clear version */
		sha1Bytes[6] |= 0x50; /* set to version 5 */
		sha1Bytes[8] &= 0x3f; /* clear variant */
		sha1Bytes[8] |= 0x80; /* set to IETF variant */
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
		c.setDate(JodaDateUtils.toJoda(counts.getTimestamp(), ZoneOffset.UTC));
		return c;
	}

}