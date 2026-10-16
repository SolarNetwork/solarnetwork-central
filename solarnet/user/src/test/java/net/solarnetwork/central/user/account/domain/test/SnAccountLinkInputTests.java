/* ==================================================================
 * SnAccountLinkInputTests.java - 15/10/2026 6:02:41 pm
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

package net.solarnetwork.central.user.account.domain.test;

import static org.assertj.core.api.BDDAssertions.and;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.account.domain.SnAccountLinkInput;
import net.solarnetwork.central.user.account.domain.SnLinkedAccountInfo;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Test cases for the {@link SnAccountLinkInput} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class SnAccountLinkInputTests {

	private static final SnLinkedAccountInfo PARENT = new SnLinkedAccountInfo(1L, "Parent",
			"parent@localhost");

	private static SnAccountLinkInput input() {
		final SnAccountLinkInput input = new SnAccountLinkInput();
		input.setUsername("parent@localhost");
		input.setPassword("secret");
		return input;
	}

	@Test
	public void verified() {
		// GIVEN
		final SnAccountLinkInput input = input();

		// WHEN
		input.verified(PARENT);

		// THEN
		// @formatter:off
		and.then(input.getVerifiedAccount())
			.as("Verified account saved")
			.isSameAs(PARENT)
			;
		and.then(input.getPassword())
			.as("Password cleared once verified")
			.isNull()
			;
		and.then(input.getUsername())
			.as("Username kept")
			.isEqualTo("parent@localhost")
			;
		// @formatter:on
	}

	@Test
	public void setUsername_same() {
		// GIVEN
		final SnAccountLinkInput input = input();
		input.verified(PARENT);

		// WHEN
		input.setUsername(" Parent@Localhost ");

		// THEN
		// @formatter:off
		and.then(input.getVerifiedAccount())
			.as("Verification kept when the same email is submitted again")
			.isSameAs(PARENT)
			;
		// @formatter:on
	}

	@Test
	public void setUsername_changed() {
		// GIVEN
		final SnAccountLinkInput input = input();
		input.verified(PARENT);

		// WHEN
		input.setUsername("other@localhost");

		// THEN
		// @formatter:off
		and.then(input.getVerifiedAccount())
			.as("Verification cleared when the email changes")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void setUsername_cleared() {
		// GIVEN
		final SnAccountLinkInput input = input();
		input.verified(PARENT);

		// WHEN
		input.setUsername(null);

		// THEN
		// @formatter:off
		and.then(input.getVerifiedAccount())
			.as("Verification cleared when the email is removed")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void serialize() throws Exception {
		// GIVEN
		final SnAccountLinkInput input = input();

		// WHEN
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(byos)) {
			out.writeObject(input);
		}
		final SnAccountLinkInput result;
		try (ObjectInputStream in = new ObjectInputStream(
				new ByteArrayInputStream(byos.toByteArray()))) {
			result = (SnAccountLinkInput) in.readObject();
		}

		// THEN
		// @formatter:off
		and.then(result.getUsername())
			.as("Username serialized")
			.isEqualTo("parent@localhost")
			;
		and.then(result.getPassword())
			.as("Password not serialized, so it is never stored in a persisted session")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void serialize_verified() throws Exception {
		// GIVEN
		final SnAccountLinkInput input = input();
		input.verified(PARENT);

		// WHEN
		final ByteArrayOutputStream byos = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(byos)) {
			out.writeObject(input);
		}
		final SnAccountLinkInput result;
		try (ObjectInputStream in = new ObjectInputStream(
				new ByteArrayInputStream(byos.toByteArray()))) {
			result = (SnAccountLinkInput) in.readObject();
		}

		// THEN
		// @formatter:off
		and.then(result.getVerifiedAccount())
			.as("Verified account serialized")
			.isEqualTo(PARENT)
			;
		// @formatter:on
	}

	@Test
	public void json() throws Exception {
		// GIVEN
		final String json = """
				{"username":"parent@localhost","password":"secret",
				"verifiedAccount":{"userId":2,"name":"Other","email":"other@localhost"}}
				""";

		// WHEN
		SnAccountLinkInput result = JsonUtils.JSON_OBJECT_MAPPER.readValue(json,
				SnAccountLinkInput.class);

		// THEN
		// @formatter:off
		and.then(result.getPassword())
			.as("Password read from JSON")
			.isEqualTo("secret")
			;
		and.then(result.getVerifiedAccount())
			.as("Verified account can not be set from JSON")
			.isNull()
			;
		// @formatter:on
	}

}
