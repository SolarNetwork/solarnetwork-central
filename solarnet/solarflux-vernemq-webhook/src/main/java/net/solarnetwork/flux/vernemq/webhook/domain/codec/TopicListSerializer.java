/* ==================================================================
 * TopicListSerializer.java - 16/01/2026 12:02:44 pm
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

package net.solarnetwork.flux.vernemq.webhook.domain.codec;

import net.solarnetwork.flux.vernemq.webhook.domain.TopicList;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serizlie {@link TopicList} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class TopicListSerializer extends StdSerializer<TopicList> {

	/**
	 * Constructor.
	 */
	public TopicListSerializer() {
		super(TopicList.class);
	}

	@Override
	public void serialize(TopicList value, JsonGenerator gen, SerializationContext provider)
			throws JacksonException {
		if ( value == null ) {
			gen.writeNull();
			return;
		}
		gen.writeStartArray();

		if ( value.getTopics() != null ) {
			for ( String t : value.getTopics() ) {
				gen.writeString(t);
			}
		}

		gen.writeEndArray();
	}

}
