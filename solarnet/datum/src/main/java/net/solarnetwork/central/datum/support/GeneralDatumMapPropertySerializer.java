/* ==================================================================
 * GeneralDatumMapPropertySerializer.java - 20/02/2026 8:56:34 am
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

package net.solarnetwork.central.datum.support;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.codec.PropertySerializer;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.StringUtils;

/**
 * Serialize a {@link GeneralDatum} to a {@code Map}.
 *
 * <p>
 * The {@link StreamDatum} API is also supported to include a {@code streamId}
 * property.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class GeneralDatumMapPropertySerializer implements PropertySerializer {

	/**
	 * Constructor.
	 */
	public GeneralDatumMapPropertySerializer() {
		super();
	}

	@Override
	public Object serialize(Object data, String propertyName, Object propertyValue) {
		final GeneralDatum datum = (GeneralDatum) propertyValue;
		Map<String, Object> props = new LinkedHashMap<>(8);
		if ( datum.getTimestamp() != null ) {
			props.put("created", datum.getTimestamp());
		}
		if ( datum.getKind() != null ) {
			props.put("kind", datum.getKind());
		}
		if ( datum.getObjectId() != null ) {
			props.put("objectId", datum.getObjectId());
		}
		if ( datum.getSourceId() != null ) {
			props.put("sourceId", datum.getSourceId());
		}
		if ( datum instanceof StreamDatum sd ) {
			if ( sd.getStreamId() != null ) {
				props.put("streamId", sd.getStreamId());
			}
		}
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

	private static void addProps(Map<String, Object> props, Map<String, ?> data) {
		if ( data == null ) {
			return;
		}
		for ( Map.Entry<String, ?> me : data.entrySet() ) {
			if ( !props.containsKey(me.getKey()) && me.getValue() != null ) {
				props.put(me.getKey(), me.getValue());
			}
		}
	}

}
