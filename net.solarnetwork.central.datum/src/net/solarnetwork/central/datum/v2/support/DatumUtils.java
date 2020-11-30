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

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRollupFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.domain.SourceFilter;
import net.solarnetwork.central.datum.domain.UserFilter;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.DateRangeFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.LocalDateRangeFilter;
import net.solarnetwork.central.domain.NodeFilter;
import net.solarnetwork.central.domain.OptimizedQueryFilter;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.JodaDateUtils;

/**
 * General datum utility methods.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class DatumUtils {

	private DatumUtils() {
		// don't construct me
	}

	/**
	 * Convert a filter to a datum criteria.
	 * 
	 * @param filter
	 *        the filter to convert
	 * @return the filter, or {@literal null} if {@code filter} is
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
	 * @return the filter, or {@literal null} if {@code filter} is
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

		if ( filter instanceof DatumFilterCommand ) {
			DatumFilterCommand f = (DatumFilterCommand) filter;
			// most common
			c.setNodeIds(f.getNodeIds());
			c.setLocationIds(f.getLocationIds());
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
				c.setLocationIds(((GeneralLocationDatumMetadataFilter) filter).getLocationIds());
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
	 * Create a new {@link ReportingGeneralNodeDatum} out of a {@link Datum}.
	 * 
	 * @param datum
	 *        the datum to convert
	 * @param meta
	 *        the datum metadata
	 * @return the general datum, or {@literal null} if {@code datum} is
	 *         {@literal null} or {@code meta} is not an instance of
	 *         {@link NodeDatumStreamMetadata}
	 */
	public static ReportingGeneralNodeDatum toGeneralNodeDatum(Datum datum, DatumStreamMetadata meta) {
		if ( datum == null || !(meta instanceof NodeDatumStreamMetadata) ) {
			return null;
		}
		NodeDatumStreamMetadata objMeta = (NodeDatumStreamMetadata) meta;
		DateTimeZone zone = objMeta.getTimeZoneId() != null ? DateTimeZone.forID(objMeta.getTimeZoneId())
				: DateTimeZone.UTC;

		// use ReportingGeneralNodeDatum to support localDateTime property
		ReportingGeneralNodeDatum gnd = new ReportingGeneralNodeDatum();
		gnd.setCreated(new DateTime(datum.getTimestamp().toEpochMilli(), zone));
		gnd.setLocalDateTime(gnd.getCreated().toLocalDateTime());
		gnd.setNodeId(objMeta.getObjectId());
		gnd.setSourceId(objMeta.getSourceId());

		DatumProperties props = datum.getProperties();
		if ( props != null ) {
			GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
			populateGeneralDatumSamples(s, props, meta);
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
	 * @retur nthe general datum auziliary, or {@literal null} if either
	 *        {@code datum} or {@code meta} is {@literal null}
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
	 *         {@literal null} or {@code meta} is not an instance of
	 *         {@link LocationDatumStreamMetadata}
	 */
	public static ReportingGeneralLocationDatum toGeneralLocationDatum(Datum datum,
			DatumStreamMetadata meta) {
		if ( datum == null || !(meta instanceof LocationDatumStreamMetadata) ) {
			return null;
		}
		LocationDatumStreamMetadata objMeta = (LocationDatumStreamMetadata) meta;
		DateTimeZone zone = objMeta.getTimeZoneId() != null ? DateTimeZone.forID(objMeta.getTimeZoneId())
				: DateTimeZone.UTC;

		// use ReportingGeneralLocationDatum to support localDateTime property
		ReportingGeneralLocationDatum gnd = new ReportingGeneralLocationDatum();
		gnd.setCreated(new DateTime(datum.getTimestamp().toEpochMilli(), zone));
		gnd.setLocalDateTime(gnd.getCreated().toLocalDateTime());
		gnd.setLocationId(objMeta.getObjectId());
		gnd.setSourceId(objMeta.getSourceId());

		DatumProperties props = datum.getProperties();
		if ( props != null ) {
			GeneralLocationDatumSamples s = new GeneralLocationDatumSamples();
			populateGeneralDatumSamples(s, props, meta);
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

}
