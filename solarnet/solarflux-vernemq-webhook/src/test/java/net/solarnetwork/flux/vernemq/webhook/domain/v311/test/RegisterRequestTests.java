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

import static net.solarnetwork.flux.vernemq.webhook.support.JsonUtils.JSON_MAPPER;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link RegisterRequest} class.
 * 
 * @author matt
 */
public class RegisterRequestTests extends TestSupport {

	@Test
	public void parseFull() throws IOException {
		RegisterRequest req = JSON_MAPPER.readValue(classResourceAsBytes("auth_on_register-01.json"),
				RegisterRequest.class);

		// THEN
		// @formatter:off
		then(req)
			.returns(false, from(RegisterRequest::getCleanSession))
			.returns("clientid", from(RegisterRequest::getClientId))
			.returns("", from(RegisterRequest::getMountpoint))
			.returns("password", from(RegisterRequest::getPassword))
			.returns("127.0.0.1", from(RegisterRequest::getPeerAddress))
			.returns(8888, from(RegisterRequest::getPeerPort))
			.returns("username", from(RegisterRequest::getUsername))
			;
		// @formatter:on
	}

	@Test
	public void parseFull_v5() throws IOException {
		RegisterRequest req = JSON_MAPPER.readValue(classResourceAsBytes("auth_on_register-v5-01.json"),
				RegisterRequest.class);

		// THEN
		// @formatter:off
		then(req)
			.returns(false, from(RegisterRequest::getCleanSession))
			.returns("clientid", from(RegisterRequest::getClientId))
			.returns("", from(RegisterRequest::getMountpoint))
			.returns("password", from(RegisterRequest::getPassword))
			.returns("127.0.0.1", from(RegisterRequest::getPeerAddress))
			.returns(8888, from(RegisterRequest::getPeerPort))
			.returns("username", from(RegisterRequest::getUsername))
			;
		// @formatter:on
	}

}
