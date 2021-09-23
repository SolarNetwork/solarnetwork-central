/* ==================================================================
 * GeneralNodeDatumMapPropertySerializer.java - Sep 5, 2014 7:12:44 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.ReadingDatum;
import net.solarnetwork.central.datum.domain.ReportingDatum;
import net.solarnetwork.codec.PropertySerializer;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.util.StringUtils;

/**
 * Serialize a {@link GeneralNodeDatum} to a {@code Map}.
 * 
 * <p>
 * The {@link ReportingDatum} API is also supported (those properties will be
 * added to the output if a {@link GeneralNodeDatum} subclass implements that
 * interface). Similarly the {@link ReadingDatum} API is supported and all start
 * and final properties will be added to the resulting map, adding the
 * {@link ReadingDatum#START_PROPERTY_SUFFIX} and
 * {@link ReadingDatum#FINAL_PROPERTY_SUFFIX} property name suffix as
 * appropriate.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralNodeDatumMapPropertySerializer implements PropertySerializer {

	@Override
	public Object serialize(Object data, String propertyName, Object propertyValue) {
		GeneralNodeDatum datum = (GeneralNodeDatum) propertyValue;
		Map<String, Object> props = new LinkedHashMap<String, Object>(8);
		props.put("created", datum.getCreated());
		if ( datum instanceof ReportingDatum ) {
			ReportingDatum rd = (ReportingDatum) datum;
			props.put("localDate", rd.getLocalDate());
			props.put("localTime", rd.getLocalTime());
		}
		if ( datum instanceof ReadingDatum ) {
			ReadingDatum rd = (ReadingDatum) datum;
			Map<String, ?> dataStart = rd.getSampleDataStart();
			if ( dataStart != null ) {
				for ( Map.Entry<String, ?> me : dataStart.entrySet() ) {
					String key = me.getKey();
					if ( key == null ) {
						continue;
					}
					props.put(key + ReadingDatum.START_PROPERTY_SUFFIX, me.getValue());
				}
			}

			Map<String, ?> dataFinal = rd.getSampleDataFinal();
			if ( dataFinal != null ) {
				for ( Map.Entry<String, ?> me : dataFinal.entrySet() ) {
					String key = me.getKey();
					if ( key == null ) {
						continue;
					}
					props.put(key + ReadingDatum.FINAL_PROPERTY_SUFFIX, me.getValue());
				}
			}
		}
		props.put("nodeId", datum.getNodeId());
		props.put("sourceId", datum.getSourceId());

		DatumSamples samples = datum.getSamples();
		if ( samples != null ) {
			addProps(props, samples.getInstantaneous());
			addProps(props, samples.getAccumulating());
			addProps(props, samples.getStatus());
			String tagString = StringUtils.delimitedStringFromCollection(samples.getTags(), ";");
			if ( tagString != null ) {
				props.put("tags", tagString);
			}
		}
		return props;
	}

	private void addProps(Map<String, Object> props, Map<String, ?> data) {
		if ( data == null ) {
			return;
		}
		for ( Map.Entry<String, ?> me : data.entrySet() ) {
			if ( !props.containsKey(me.getKey()) ) {
				props.put(me.getKey(), me.getValue());
			}
		}
	}

}
