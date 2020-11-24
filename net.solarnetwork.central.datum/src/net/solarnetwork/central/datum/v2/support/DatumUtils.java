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

import java.util.LinkedHashSet;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * General datum utility methods.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class DatumUtils {

	/**
	 * 
	 */
	private DatumUtils() {
		// TODO Auto-generated constructor stub
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
		gnd.setCreated(new DateTime(datum.getTimestamp(), zone));
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

}
