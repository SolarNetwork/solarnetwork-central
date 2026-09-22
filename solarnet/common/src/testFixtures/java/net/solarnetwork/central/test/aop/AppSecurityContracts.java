/* ==================================================================
 * AppSecurityContracts.java - 22/09/2026 7:31:08 pm
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

package net.solarnetwork.central.test.aop;

import static java.util.stream.Collectors.toCollection;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.springframework.context.ApplicationContext;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * The security contracts of the service APIs of an application.
 *
 * <p>
 * This verifies that an application applies its security aspects to the
 * service beans that its controllers use, by running the contracts'
 * {@link SecurityContract#denyTests(Object)} against the beans of the
 * application context. The bean of each API is resolved by type, as it is for
 * dependency injection, so the bean designated as primary is used when there
 * are several.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class AppSecurityContracts {

	private record Entry<T>(Class<T> api, Function<TestTenants, SecurityContract<T>> contract) {

		private DynamicNode denyTests(ApplicationContext context, TestTenants tenants) {
			return dynamicContainer(api.getSimpleName(), Stream.of(this)
					.flatMap(e -> e.contract.apply(tenants).denyTests(context.getBean(e.api))));
		}

	}

	private final List<Entry<?>> entries = new ArrayList<>();

	/**
	 * Add a contract.
	 *
	 * @param <T>
	 *        the API type
	 * @param api
	 *        the API
	 * @param contract
	 *        the contract factory
	 * @return this instance, for method chaining
	 */
	public <T> AppSecurityContracts with(Class<T> api,
			Function<TestTenants, SecurityContract<T>> contract) {
		entries.add(new Entry<>(requireNonNullArgument(api, "api"),
				requireNonNullArgument(contract, "contract")));
		return this;
	}

	/**
	 * Get the APIs that have a contract.
	 *
	 * @return the APIs
	 */
	public Set<Class<?>> apis() {
		return entries.stream().map(Entry::api).collect(toCollection(LinkedHashSet::new));
	}

	/**
	 * Create dynamic tests that verify the denied actors of every contract
	 * against the application service beans.
	 *
	 * <p>
	 * The tenants must be present in the database.
	 * </p>
	 *
	 * @param context
	 *        the application context
	 * @param tenants
	 *        the tenants
	 * @return the tests, one container per API
	 */
	public Stream<DynamicNode> denyTests(ApplicationContext context, TestTenants tenants) {
		return entries.stream().map(e -> e.denyTests(context, tenants));
	}

}
