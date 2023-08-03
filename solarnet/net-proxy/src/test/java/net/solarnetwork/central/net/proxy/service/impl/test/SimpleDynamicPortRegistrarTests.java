/* ==================================================================
 * SimpleDynamicPortRegistrarTests.java - 3/08/2023 6:58:11 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.net.proxy.service.impl.test;

import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.net.proxy.service.impl.SimpleDynamicPortRegistrar;

/**
 * Test cases for the {@link SimpleDynamicPortRegistrar} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SimpleDynamicPortRegistrarTests {

	@Mock
	private Supplier<Integer> portSupplier;

	@Test
	public void allocatePort() {
		// GIVEN
		final SimpleDynamicPortRegistrar service = new SimpleDynamicPortRegistrar();

		// WHEN
		int port = service.reserveNewPort();

		// THEN
		then(port).as("Port allocated").isGreaterThan(0);
	}

	@Test
	public void allocatePort_custom() {
		// GIVEN
		final SimpleDynamicPortRegistrar service = new SimpleDynamicPortRegistrar(portSupplier);

		final Integer randomPort = Math.abs((int) UUID.randomUUID().getMostSignificantBits());

		given(portSupplier.get()).willReturn(randomPort);

		// WHEN
		int port = service.reserveNewPort();

		// THEN
		verify(portSupplier, description("Supplier called just once")).get();
		then(port).as("Port allocated from supplier").isEqualTo(randomPort);
	}

	@Test
	public void allocatePort_retry() {
		// GIVEN
		final SimpleDynamicPortRegistrar service = new SimpleDynamicPortRegistrar(portSupplier);

		final Integer randomPort = Math.abs((int) UUID.randomUUID().getMostSignificantBits());

		given(portSupplier.get()).willThrow(new RuntimeException("Fail")).willReturn(randomPort);

		// WHEN
		int port = service.reserveNewPort();

		// THEN
		verify(portSupplier, times(2).description("Supplier called once (fail) plus retry")).get();
		then(port).as("Port allocated from supplier").isEqualTo(randomPort);
	}

	@Test
	public void allocatePort_fail() {
		// GIVEN
		final int retries = 4;
		final SimpleDynamicPortRegistrar service = new SimpleDynamicPortRegistrar(portSupplier, retries);

		given(portSupplier.get()).willThrow(new RuntimeException("Fail"));

		// WHEN
		catchThrowableOfType(() -> {
			service.reserveNewPort();
		}, IllegalStateException.class);

		// THEN
		verify(portSupplier, times(retries + 1).description("Supplier called retries + 1 times")).get();
	}

}
