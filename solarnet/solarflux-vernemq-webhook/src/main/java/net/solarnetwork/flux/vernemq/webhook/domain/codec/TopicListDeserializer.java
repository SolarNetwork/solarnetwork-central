/* ==================================================================
 * TopicListDeserializer.java - 16/01/2026 12:24:28 pm
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

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicList;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Deserialize {@link TopicList} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class TopicListDeserializer extends StdDeserializer<TopicList> {

	/**
	 * Constructor.
	 */
	public TopicListDeserializer() {
		super(TopicList.class);
	}

	@Override
	public TopicList deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
		JsonToken t = p.currentToken();
		if ( t == JsonToken.VALUE_NULL ) {
			return null;
		} else if ( !p.isExpectedStartArrayToken() ) {
			throw MismatchedInputException.from(p, "Unable to parse TopicList (not an array)");
		}

		List<String> topics = new ArrayList<>(2);

		while ( (t = p.nextToken()) != JsonToken.END_ARRAY ) {
			topics.add(p.getString());
		}

		return new TopicList(topics);
	}

}
