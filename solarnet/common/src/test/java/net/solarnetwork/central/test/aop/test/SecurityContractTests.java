/* ==================================================================
 * SecurityContractTests.java - 22/09/2026 8:39:25 am
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

package net.solarnetwork.central.test.aop.test;

import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Test cases for the {@link SecurityContract} class.
 *
 * <p>
 * These tests run the dynamic tests of contracts for a toy service, to verify
 * the contract detects each kind of security problem.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class SecurityContractTests {

	private TestTenants tenants;

	/**
	 * The results of running dynamic tests.
	 *
	 * @param passed
	 *        the display names of the passed tests
	 * @param failed
	 *        the display names of the failed tests
	 */
	private record Results(List<String> passed, List<String> failed) {

	}

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
	}

	private static Results run(Stream<DynamicNode> nodes) {
		final Results results = new Results(new ArrayList<>(), new ArrayList<>());
		nodes.forEach(n -> run(n, "", results));
		return results;
	}

	private static void run(DynamicNode node, String prefix, Results results) {
		if ( node instanceof DynamicContainer c ) {
			c.getChildren().forEach(n -> run(n, prefix + c.getDisplayName() + " / ", results));
		} else if ( node instanceof DynamicTest t ) {
			final String name = prefix + t.getDisplayName();
			try {
				t.getExecutable().execute();
				results.passed().add(name);
			} catch ( Throwable e ) {
				results.failed().add(name);
			}
		}
	}

	private SecuredProxy<ToyBiz> toyProxy() {
		return securedProxy((ToyBiz) mock(DaoToyBiz.class),
				new ToySecurityAspect(tenants.ownershipDao()));
	}

	private SecurityContract.Builder<ToyBiz> toyContract() {
		final TestTenant a = tenants.a();
		// @formatter:off
		return SecurityContract.forApi(ToyBiz.class, tenants)
				.userRead(biz -> biz.userThing(a.userId()))
				.userWrite(biz -> biz.saveUserThing(a.userId(), "foo"))
				.nodeRead(biz -> biz.nodeThing(a.privateNodeId()))
				.nodeWrite(biz -> biz.saveNodeThing(a.privateNodeId(), "foo"))
				.exempt("publicThing", "no user data")
				;
		// @formatter:on
	}

	@Test
	public void satisfied() {
		// GIVEN
		final SecurityContract<ToyBiz> contract = toyContract()
				.exempt("unguardedThing", "for testing").build();

		// WHEN
		final Results results = run(contract.dynamicTests(this::toyProxy));

		// THEN
		// @formatter:off
		then(results.failed())
			.as("All tests pass")
			.isEmpty()
			;
		then(results.passed())
			.as("Coverage test plus test for every case and actor")
			.hasSize(1 + 4 * tenants.actors().size())
			;
		// @formatter:on
	}

	@Test
	public void missingCase() {
		// GIVEN
		final SecurityContract<ToyBiz> contract = toyContract().build();

		// WHEN
		final Results results = run(contract.dynamicTests(this::toyProxy));

		// THEN
		// @formatter:off
		then(contract.coverageProblems())
			.as("Method without case or exemption reported")
			.containsExactly("No case or exemption for unguardedThing(Long)")
			;
		then(results.failed())
			.as("Only the coverage test fails")
			.containsExactly("covers all ToyBiz methods")
			;
		// @formatter:on
	}

	@Test
	public void unknownExemption() {
		// GIVEN
		final SecurityContract<ToyBiz> contract = toyContract()
				.exempt("unguardedThing", "for testing").exempt("noSuchThing", "for testing")
				.build();

		// THEN
		// @formatter:off
		then(contract.coverageProblems())
			.as("Exemption for unknown method reported")
			.containsExactly("Exemption for unknown method noSuchThing")
			;
		// @formatter:on
	}

	@Test
	public void methodExemption() {
		// GIVEN
		final TestTenant a = tenants.a();
		// @formatter:off
		final SecurityContract<ToyBiz> contract = SecurityContract.forApi(ToyBiz.class, tenants)
				.userRead(biz -> biz.userThing(a.userId()))
				.userWrite(biz -> biz.saveUserThing(a.userId(), "foo"))
				.nodeRead(biz -> biz.nodeThing(a.privateNodeId()))
				.nodeWrite(biz -> biz.saveNodeThing(a.privateNodeId(), "foo"))
				.exempt("unguardedThing", "for testing")
				.exempt(biz -> biz.publicThing(), "no user data")
				.build();
		// @formatter:on

		// THEN
		// @formatter:off
		then(contract.coverageProblems())
			.as("Exemption for one overload does not exempt other overloads")
			.containsExactly("No case or exemption for publicThing(String)")
			;
		// @formatter:on
	}

	@Test
	public void unguardedMethod() {
		// GIVEN
		final TestTenant a = tenants.a();
		final SecurityContract<ToyBiz> contract = toyContract()
				.userRead(biz -> biz.unguardedThing(a.userId())).build();

		// WHEN
		final Results results = run(contract.dynamicTests(this::toyProxy));

		// THEN
		// @formatter:off
		then(results.failed())
			.as("Every actor that should be denied the unguarded method is not denied")
			.containsExactlyInAnyOrder(
					"unguardedThing(Long) / denied: anonymous",
					"unguardedThing(Long) / denied: A data token",
					"unguardedThing(Long) / denied: B user",
					"unguardedThing(Long) / denied: B user token",
					"unguardedThing(Long) / denied: B data token",
					"unguardedThing(Long) / denied: B node")
			;
		// @formatter:on
	}

	@Test
	public void weakerCheck() {
		// GIVEN
		final TestTenant a = tenants.a();

		// expect write access, but the aspect only requires read access
		final SecurityContract<ToyBiz> contract = SecurityContract.forApi(ToyBiz.class, tenants)
				.nodeWrite(biz -> biz.nodeThing(a.privateNodeId())).build();

		// WHEN
		final Results results = run(contract.dynamicTests(this::toyProxy));

		// THEN
		// @formatter:off
		then(results.failed())
			.as("The actor allowed read but not write access is not denied")
			.contains("nodeThing(Long) / denied: A data token")
			.doesNotContain("nodeThing(Long) / denied: B user")
			;
		// @formatter:on
	}

	@Test
	public void securableTarget() {
		// GIVEN
		final TestTenant a = tenants.a();
		// @formatter:off
		final SecurityContract<ToyBiz> contract = SecurityContract.forApi(ToyBiz.class, tenants)
				.userRead(biz -> biz.userThing(a.userId()))
				.userWrite(biz -> biz.saveUserThing(a.userId(), "foo"))
				.exempt("nodeThing", "for testing")
				.exempt("saveNodeThing", "for testing")
				.exempt("unguardedThing", "for testing")
				.exempt("publicThing", "for testing")
				.build();
		// @formatter:on

		// WHEN
		final Results results = run(
				contract.dynamicTests(() -> securedProxy((ToyBiz) mock(DaoToyBiz.class),
						new ToySecurableSecurityAspect(tenants.ownershipDao()))));

		// THEN
		// @formatter:off
		then(results.failed())
			.as("All tests pass with Securable target")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void securableTarget_notSecurable() {
		// GIVEN
		final TestTenant a = tenants.a();
		// @formatter:off
		final SecurityContract<ToyBiz> contract = SecurityContract.forApi(ToyBiz.class, tenants)
				.userRead(biz -> biz.userThing(a.userId()))
				.exempt("saveUserThing", "for testing")
				.exempt("nodeThing", "for testing")
				.exempt("saveNodeThing", "for testing")
				.exempt("unguardedThing", "for testing")
				.exempt("publicThing", "for testing")
				.build();
		// @formatter:on

		// WHEN
		final Results results = run(
				contract.dynamicTests(() -> securedProxy((ToyBiz) mock(UnsecurableToyBiz.class),
						new ToySecurableSecurityAspect(tenants.ownershipDao()))));

		// THEN
		// @formatter:off
		then(results.failed())
			.as("Aspect not applied to target without Securable annotation, so denied actors allowed")
			.hasSize(6)
			.allMatch(name -> name.startsWith("userThing(Long) / denied: "))
			;
		// @formatter:on
	}

}
