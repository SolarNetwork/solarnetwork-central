/* ==================================================================
 * JsonConfig.java - 16/01/2026 7:31:39 pm
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

package net.solarnetwork.flux.vernemq.webhook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.annotation.JsonInclude;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicList;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.codec.ResponseSerializer;
import net.solarnetwork.flux.vernemq.webhook.domain.codec.TopicListDeserializer;
import net.solarnetwork.flux.vernemq.webhook.domain.codec.TopicListSerializer;
import net.solarnetwork.flux.vernemq.webhook.domain.codec.TopicSettingsDeserializer;
import net.solarnetwork.flux.vernemq.webhook.domain.codec.TopicSubscriptionSettingDeserializer;
import net.solarnetwork.flux.vernemq.webhook.domain.codec.TopicSubscriptionSettingSerializer;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.DeliverRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.codec.DeliverRequestDeserializer;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.codec.PublishModifiersSerializer;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.codec.PublishRequestDeserializer;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.codec.RegisterModifiersSerializer;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.codec.RegisterRequestDeserializer;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.codec.SubscribeRequestDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * JSON configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class JsonConfig {

	/**
	 * Get the primary {@link JsonMapper} to use for JSON processing.
	 *
	 * @return the mapper
	 */
	@Primary
	@Bean
	public JsonMapper jsonMapper() {
		SimpleModule m = new SimpleModule("SolarFlux");
		m.addSerializer(new ResponseSerializer());
		m.addSerializer(new TopicListSerializer());
		m.addSerializer(new TopicListSerializer());
		m.addSerializer(new TopicSubscriptionSettingSerializer());
		m.addSerializer(new PublishModifiersSerializer());
		m.addSerializer(new RegisterModifiersSerializer());

		m.addDeserializer(TopicList.class, new TopicListDeserializer());
		m.addDeserializer(TopicSettings.class, TopicSettingsDeserializer.INSTANCE);
		m.addDeserializer(TopicSubscriptionSetting.class, TopicSubscriptionSettingDeserializer.INSTANCE);
		m.addDeserializer(DeliverRequest.class, new DeliverRequestDeserializer());
		m.addDeserializer(PublishRequest.class, new PublishRequestDeserializer());
		m.addDeserializer(RegisterRequest.class, new RegisterRequestDeserializer());
		m.addDeserializer(SubscribeRequest.class, new SubscribeRequestDeserializer());

		return JsonMapper.builder()
				.changeDefaultPropertyInclusion(
						incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
				.changeDefaultPropertyInclusion(
						incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL))
				.addModule(m).build();
	}

}
