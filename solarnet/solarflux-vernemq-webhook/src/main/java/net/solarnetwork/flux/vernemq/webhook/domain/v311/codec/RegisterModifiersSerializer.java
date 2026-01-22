/* ==================================================================
 * RegisterModifiersSerializer.java - 16/01/2026 12:14:47 pm
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

package net.solarnetwork.flux.vernemq.webhook.domain.v311.codec;

import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterModifiers;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serialize {@link RegisterModifiers} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class RegisterModifiersSerializer extends StdSerializer<RegisterModifiers> {

	/**
	 * Constructor.
	 */
	public RegisterModifiersSerializer() {
		super(RegisterModifiers.class);
	}

	@Override
	public void serialize(RegisterModifiers value, JsonGenerator gen, SerializationContext provider)
			throws JacksonException {
		if ( value == null ) {
			gen.writeNull();
			return;
		}
		gen.writeStartObject();

		if ( value.getSubscriberId() != null ) {
			gen.writeStringProperty("subscriber_id", value.getSubscriberId());
		}
		if ( value.getRegView() != null ) {
			gen.writeStringProperty("reg_view", value.getRegView());
		}
		if ( value.getCleanSession() != null ) {
			gen.writeBooleanProperty("clean_session", value.getCleanSession());
		}
		if ( value.getMaxMessageSize() != null ) {
			gen.writeNumberProperty("max_message_size", value.getMaxMessageSize());
		}
		if ( value.getMaxMessageRate() != null ) {
			gen.writeNumberProperty("max_message_rate", value.getMaxMessageRate());
		}
		if ( value.getMaxInflightMessages() != null ) {
			gen.writeNumberProperty("max_inflight_messages", value.getMaxInflightMessages());
		}
		if ( value.getRetryInterval() != null ) {
			gen.writeNumberProperty("retry_interval", value.getRetryInterval());
		}
		if ( value.getUpgradeQos() != null ) {
			gen.writeBooleanProperty("upgrade_qos", value.getUpgradeQos());
		}

		gen.writeEndObject();
	}

}
