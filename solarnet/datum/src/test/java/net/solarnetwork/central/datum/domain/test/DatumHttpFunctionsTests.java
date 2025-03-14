/* ==================================================================
 * DatumHttpFunctionsTests.java - 13/03/2025 3:00:28 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain.test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.DatumHttpFunctions;

/**
 * Test cases for the {@link DatumHttpFunctions} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumHttpFunctionsTests implements DatumHttpFunctions {

	@Test
	public void httpBasic() {
		// GIVEN
		final String username = randomString();
		final String password = randomString();

		// WHEN
		String result = httpBasic(username, password);

		// THEN
		// @formatter:off
		then(result)
			.as("HTTP Basic encoded")
			.isEqualTo("Basic " + Base64.getEncoder().encodeToString("%s:%s".formatted(username, password).getBytes(ISO_8859_1)))
			;
		// @formatter:on
	}

	@Test
	public void httpBasic_null() {
		// GIVEN

		// WHEN
		String result = httpBasic(null, null);

		// THEN
		// @formatter:off
		then(result)
			.as("HTTP Basic encoded, using 'null' for null values")
			.isEqualTo("Basic " + Base64.getEncoder().encodeToString("null:null".getBytes(ISO_8859_1)))
			;
		// @formatter:on
	}

}
