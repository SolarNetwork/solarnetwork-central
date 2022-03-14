/* ==================================================================
 * GeneralLocationDatumMapPropertySerializer.java - Oct 17, 2014 2:31:27 PM
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
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingDatum;
import net.solarnetwork.codec.PropertySerializer;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.util.StringUtils;

/**
 * Serialize a {@link GeneralLocationDatum} to a {@code Map}. The
 * {@link ReportingDatum} API is also supported (those properties will be added
 * to the output if a {@link GeneralLocationDatum} subclass implements that
 * interface).
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralLocationDatumMapPropertySerializer implements PropertySerializer {

	@Override
	public Object serialize(Object data, String propertyName, Object propertyValue) {
		GeneralLocationDatum datum = (GeneralLocationDatum) propertyValue;
		Map<String, Object> props = new LinkedHashMap<String, Object>(8);
		props.put("created", datum.getCreated());
		if ( datum instanceof ReportingDatum ) {
			ReportingDatum rd = (ReportingDatum) datum;
			props.put("localDate", rd.getLocalDate());
			props.put("localTime", rd.getLocalTime());
		}
		props.put("locationId", datum.getLocationId());
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
