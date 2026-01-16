/* ========================================================================
 * Copyright 2018 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.domain.v311.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.flux.vernemq.webhook.support.JsonUtils.JSON_MAPPER;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link PublishRequest} class.
 * 
 * @author matt
 */
public class PublishRequestTests extends TestSupport {

	@Test
	public void parseFull() throws IOException {
		PublishRequest req = JSON_MAPPER.readValue(classResourceAsBytes("auth_on_publish-01.json"),
				PublishRequest.class);

		// THEN
		// @formatter:off
		then(req)
			.returns("clientid", from(PublishRequest::getClientId))
			.returns("", from(PublishRequest::getMountpoint))
			.returns("hello".getBytes(UTF_8), from(PublishRequest::getPayload))
			.returns(Qos.AtLeastOnce, from(PublishRequest::getQos))
			.returns(false, from(PublishRequest::getRetain))
			.returns("a/b", from(PublishRequest::getTopic))
			.returns("username", from(PublishRequest::getUsername))
			;
		// @formatter:on
	}

	@Test
	public void parseFull_v5() throws IOException {
		PublishRequest req = JSON_MAPPER.readValue(classResourceAsBytes("auth_on_publish-v5-01.json"),
				PublishRequest.class);

		// THEN
		// @formatter:off
		then(req)
			.returns("client-id", from(PublishRequest::getClientId))
			.returns("", from(PublishRequest::getMountpoint))
			.returns("message payload".getBytes(UTF_8), from(PublishRequest::getPayload))
			.returns(Qos.AtLeastOnce, from(PublishRequest::getQos))
			.returns(false, from(PublishRequest::getRetain))
			.returns("some/topic", from(PublishRequest::getTopic))
			.returns("username", from(PublishRequest::getUsername))
			;
		// @formatter:on
	}

}
