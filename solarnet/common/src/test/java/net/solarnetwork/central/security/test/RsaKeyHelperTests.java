/* ==================================================================
 * RsaKeyHelperTests.java - 22/03/2025 10:59:52 am
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

package net.solarnetwork.central.security.test;

import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.then;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.security.RsaKeyHelper;

/**
 * Test cases for the {@link RsaKeyHelper}.
 * 
 * @author matt
 * @version 1.0
 */
public class RsaKeyHelperTests {

	@Test
	public void loadRsaPrivateKeyPemFile() {
		// GIVEN
		// ssh-keygen -m PEM -t rsa -b 1024 -f test-rsa-private-key-01.pem

		// WHEN
		KeyPair result = RsaKeyHelper
				.parseKeyPair(utf8StringResource("test-rsa-private-key-01.pem", getClass()));

		// THEN
		// @formatter:off
		then(result).as("KeyPair decoded").isNotNull();
		
		then(result.getPrivate().getEncoded()).as("Private key available").isNotEmpty();
		
		then(result.getPrivate().getAlgorithm()).as("Private key is RSA").isEqualTo("RSA");
		// @formatter:on
	}

}
