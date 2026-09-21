/* ==================================================================
 * SecurityContract.java - 22/09/2026 8:39:25 am
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

import static net.solarnetwork.central.test.aop.ApiMethods.apiMethods;
import static net.solarnetwork.central.test.aop.ApiMethods.sameSignature;
import static net.solarnetwork.central.test.aop.ApiMethods.signature;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenCode;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.BDDMockito.then;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * The security contract of an API: for every API method, which actors are
 * allowed to invoke it.
 *
 * <p>
 * A contract is a table of cases, one or more per API method, plus explicit
 * exemptions for methods that do not need securing. Every case targets the data
 * of {@link TestTenants#a()} and lists the actors of
 * {@link TestTenants#actors()} that must be allowed; all other actors must be
 * denied with an {@link AuthorizationException}.
 * </p>
 *
 * <p>
 * The {@link #dynamicTests(Supplier)} method verifies the contract against
 * proxies with the security aspects applied, and verifies that every API method
 * has a case or an exemption, so new API methods cannot be added without
 * deciding how they are secured.
 * </p>
 *
 * @param <T>
 *        the API type
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public final class SecurityContract<T> {

	private final Class<T> api;
	private final TestTenants tenants;
	private final List<SecurityContractCase<T>> cases;
	private final List<Exemption> exemptions;

	/**
	 * An exemption of API methods from the contract.
	 *
	 * @param methodName
	 *        the exempt method name
	 * @param method
	 *        the exempt method, or {@code null} to exempt all methods named
	 *        {@code methodName}
	 * @param reason
	 *        why the method does not need securing
	 */
	public record Exemption(String methodName, @Nullable Method method, String reason) {

		/**
		 * Test if this exemption applies to a method.
		 *
		 * @param m
		 *        the method
		 * @return {@code true} if the method is exempt
		 */
		public boolean matches(Method m) {
			return (method != null ? sameSignature(method, m) : methodName.equals(m.getName()));
		}

	}

	private SecurityContract(Class<T> api, TestTenants tenants, List<SecurityContractCase<T>> cases,
			List<Exemption> exemptions) {
		super();
		this.api = api;
		this.tenants = tenants;
		this.cases = Collections.unmodifiableList(cases);
		this.exemptions = Collections.unmodifiableList(exemptions);
	}

	/**
	 * Start building a contract for an API.
	 *
	 * @param <T>
	 *        the API type
	 * @param api
	 *        the API interface
	 * @param tenants
	 *        the tenants
	 * @return the builder
	 */
	public static <T> Builder<T> forApi(Class<T> api, TestTenants tenants) {
		return new Builder<>(api, tenants);
	}

	/**
	 * Get the contract cases.
	 *
	 * @return the cases
	 */
	public List<SecurityContractCase<T>> cases() {
		return cases;
	}

	/**
	 * Get the exemptions.
	 *
	 * @return the exemptions
	 */
	public List<Exemption> exemptions() {
		return exemptions;
	}

	/**
	 * Get coverage problems.
	 *
	 * <p>
	 * A problem is an API method without a case or exemption, an exemption for
	 * a method that does not exist, or an exempt method that also has cases.
	 * </p>
	 *
	 * @return the problems, or an empty list if there are none
	 */
	public List<String> coverageProblems() {
		final List<String> problems = new ArrayList<>();
		final List<Method> methods = apiMethods(api);
		for ( Method m : methods ) {
			final boolean hasCase = cases.stream().anyMatch(c -> sameSignature(c.method(), m));
			final boolean exempt = exemptions.stream().anyMatch(e -> e.matches(m));
			if ( !hasCase && !exempt ) {
				problems.add("No case or exemption for %s".formatted(signature(m)));
			} else if ( hasCase && exempt ) {
				problems.add("Both cases and exemption for %s".formatted(signature(m)));
			}
		}
		for ( Exemption e : exemptions ) {
			if ( methods.stream().noneMatch(e::matches) ) {
				problems.add("Exemption for unknown method %s".formatted(e.methodName()));
			}
		}
		return problems;
	}

	/**
	 * Create dynamic tests that verify the contract.
	 *
	 * <p>
	 * The tests verify coverage, then run every case as every actor of
	 * {@link TestTenants#actors()}, each against a new proxy from
	 * {@code proxies}. Denied actors must get an {@link AuthorizationException}
	 * without the proxy target being invoked; allowed actors must reach the
	 * proxy target.
	 * </p>
	 *
	 * @param proxies
	 *        a supplier of new proxies, whose target must be a Mockito mock
	 * @return the tests
	 */
	public Stream<DynamicNode> dynamicTests(Supplier<SecuredProxy<T>> proxies) {
		final List<DynamicNode> nodes = new ArrayList<>(cases.size() + 1);
		nodes.add(coverageTest());
		for ( SecurityContractCase<T> c : cases ) {
			final List<DynamicTest> tests = new ArrayList<>(tenants.actors().size());
			for ( TestActor actor : tenants.actors() ) {
				if ( c.isAllowed(actor) ) {
					tests.add(dynamicTest("allowed: " + actor, () -> verifyAllowed(c, actor, proxies)));
				} else {
					tests.add(dynamicTest("denied: " + actor,
							() -> verifyDenied(c, actor, proxies, true)));
				}
			}
			nodes.add(dynamicContainer(c.name(), tests));
		}
		return nodes.stream();
	}

	/**
	 * Create dynamic tests that verify only the denied actors of cases that
	 * depend only on invocation arguments.
	 *
	 * <p>
	 * This is designed to verify application service beans, whose target is not
	 * a mock, and whose aspects use real DAOs; the tenants must be present in
	 * the database.
	 * </p>
	 *
	 * @param service
	 *        the application service to verify
	 * @return the tests
	 * @see SecurityContractCase#argumentsOnly()
	 */
	public Stream<DynamicNode> denyTests(T service) {
		final List<DynamicNode> nodes = new ArrayList<>(cases.size());
		final Supplier<SecuredProxy<T>> proxies = () -> new SecuredProxy<>(service, service, Map.of());
		for ( SecurityContractCase<T> c : cases ) {
			if ( !c.argumentsOnly() ) {
				continue;
			}
			final List<DynamicTest> tests = new ArrayList<>(tenants.actors().size());
			for ( TestActor actor : tenants.actors() ) {
				if ( !c.isAllowed(actor) ) {
					tests.add(dynamicTest("denied: " + actor,
							() -> verifyDenied(c, actor, proxies, false)));
				}
			}
			nodes.add(dynamicContainer(c.name(), tests));
		}
		return nodes.stream();
	}

	private DynamicTest coverageTest() {
		return dynamicTest("covers all %s methods".formatted(api.getSimpleName()), () -> {
			// @formatter:off
			and.then(coverageProblems())
				.as("Every %s method has a security case or exemption", api.getSimpleName())
				.isEmpty()
				;
			// @formatter:on
		});
	}

	private static <T> SecuredProxy<T> newProxy(SecurityContractCase<T> c,
			Supplier<SecuredProxy<T>> proxies) {
		final SecuredProxy<T> p = proxies.get();
		if ( c.setup() != null ) {
			c.setup().accept(p);
		}
		return p;
	}

	private static <T> void verifyDenied(SecurityContractCase<T> c, TestActor actor,
			Supplier<SecuredProxy<T>> proxies, boolean verifyTarget) {
		final SecuredProxy<T> p = newProxy(c, proxies);
		try {
			actor.become();
			// @formatter:off
			thenExceptionOfType(AuthorizationException.class)
				.as("%s is denied %s", actor, c.name())
				.isThrownBy(() -> c.call().invoke(p.proxy()))
				;
			// @formatter:on
		} finally {
			SecurityContextHolder.clearContext();
		}
		if ( verifyTarget && !c.targetInvokedOnDeny() ) {
			then(p.target()).shouldHaveNoInteractions();
		}
	}

	private static <T> void verifyAllowed(SecurityContractCase<T> c, TestActor actor,
			Supplier<SecuredProxy<T>> proxies) {
		final SecuredProxy<T> p = newProxy(c, proxies);
		try {
			actor.become();
			// @formatter:off
			thenCode(() -> c.call().invoke(p.proxy()))
				.as("%s is allowed %s", actor, c.name())
				.doesNotThrowAnyException()
				;
			// @formatter:on
		} finally {
			SecurityContextHolder.clearContext();
		}
		// @formatter:off
		and.then(Mockito.mockingDetails(p.target()).getInvocations())
			.as("%s reaches the target service for %s", actor, c.name())
			.isNotEmpty()
			;
		// @formatter:on
	}

	/**
	 * Builder for {@link SecurityContract}.
	 *
	 * @param <T>
	 *        the API type
	 */
	public static final class Builder<T> {

		private final Class<T> api;
		private final TestTenants tenants;
		private final List<SecurityContractCase<T>> cases = new ArrayList<>();
		private final List<Exemption> exemptions = new ArrayList<>();

		private Builder(Class<T> api, TestTenants tenants) {
			super();
			this.api = requireNonNullArgument(api, "api");
			this.tenants = requireNonNullArgument(tenants, "tenants");
		}

		/**
		 * Get the tenants.
		 *
		 * @return the tenants
		 */
		public TestTenants tenants() {
			return tenants;
		}

		/**
		 * Add a case that requires read access to tenant A's user.
		 *
		 * <p>
		 * The call must pass tenant A's user ID. The allowed actors are those
		 * allowed by {@code AuthorizationSupport.requireUserReadAccess()}: A's
		 * user, A's user tokens (policy restrictions do not apply), and A's
		 * node. A's data token is denied because it has no user metadata
		 * policy.
		 * </p>
		 *
		 * @param call
		 *        the invocation
		 * @return this builder
		 */
		public Builder<T> userRead(ApiCall<? super T> call) {
			final TestTenant a = tenants.a();
			return add(call, a.userActor(), a.tokenActor(), a.restrictedTokenActor(), a.nodeActor());
		}

		/**
		 * Add a case that requires write access to tenant A's user.
		 *
		 * <p>
		 * The call must pass tenant A's user ID. The allowed actors are those
		 * allowed by {@code AuthorizationSupport.requireUserWriteAccess()}: A's
		 * user and A's user tokens (policy restrictions do not apply).
		 * </p>
		 *
		 * @param call
		 *        the invocation
		 * @return this builder
		 */
		public Builder<T> userWrite(ApiCall<? super T> call) {
			final TestTenant a = tenants.a();
			return add(call, a.userActor(), a.tokenActor(), a.restrictedTokenActor());
		}

		/**
		 * Add a case that requires read access to tenant A's private node.
		 *
		 * <p>
		 * The call must pass tenant A's {@link TestTenant#privateNodeId()}. The
		 * allowed actors are those allowed by
		 * {@code AuthorizationSupport.requireNodeReadAccess()}: A's user, A's
		 * unrestricted tokens, and the node itself. A's restricted token is
		 * denied because its policy does not include the node.
		 * </p>
		 *
		 * @param call
		 *        the invocation
		 * @return this builder
		 */
		public Builder<T> nodeRead(ApiCall<? super T> call) {
			final TestTenant a = tenants.a();
			return add(call, a.userActor(), a.tokenActor(), a.dataTokenActor(), a.nodeActor());
		}

		/**
		 * Add a case that requires write access to tenant A's private node.
		 *
		 * <p>
		 * The call must pass tenant A's {@link TestTenant#privateNodeId()}. The
		 * allowed actors are those allowed by
		 * {@code AuthorizationSupport.requireNodeWriteAccess()}: A's user, A's
		 * unrestricted user token, and the node itself.
		 * </p>
		 *
		 * @param call
		 *        the invocation
		 * @return this builder
		 */
		public Builder<T> nodeWrite(ApiCall<? super T> call) {
			final TestTenant a = tenants.a();
			return add(call, a.userActor(), a.tokenActor(), a.nodeActor());
		}

		/**
		 * Add a case with a custom set of allowed actors.
		 *
		 * @param call
		 *        the invocation
		 * @param allowed
		 *        the allowed actors; all others must be denied
		 * @return this builder
		 */
		public Builder<T> allowing(ApiCall<? super T> call, TestActor... allowed) {
			return add(call, allowed);
		}

		/**
		 * Exempt an API method from the contract.
		 *
		 * <p>
		 * The exemption applies to all overloads of the method.
		 * </p>
		 *
		 * @param methodName
		 *        the method name
		 * @param reason
		 *        why the method does not need securing
		 * @return this builder
		 */
		public Builder<T> exempt(String methodName, String reason) {
			exemptions.add(new Exemption(requireNonNullArgument(methodName, "methodName"), null,
					requireNonNullArgument(reason, "reason")));
			return this;
		}

		/**
		 * Exempt one API method from the contract.
		 *
		 * <p>
		 * Use this to exempt one overload of a method.
		 * </p>
		 *
		 * @param call
		 *        an invocation of the method to exempt
		 * @param reason
		 *        why the method does not need securing
		 * @return this builder
		 */
		public Builder<T> exempt(ApiCall<? super T> call, String reason) {
			final Method method = ApiMethods.invokedMethod(api, call);
			exemptions.add(
					new Exemption(method.getName(), method, requireNonNullArgument(reason, "reason")));
			return this;
		}

		/**
		 * Allow additional actors in the last added case.
		 *
		 * @param actors
		 *        the actors to allow
		 * @return this builder
		 */
		public Builder<T> alsoAllow(TestActor... actors) {
			final SecurityContractCase<T> c = last();
			final Set<TestActor> allowed = new LinkedHashSet<>(c.allowed());
			allowed.addAll(Arrays.asList(actors));
			return replaceLast(new SecurityContractCase<>(c.name(), c.method(), c.call(),
					Set.copyOf(allowed), c.setup(), c.targetInvokedOnDeny()));
		}

		/**
		 * Deny additional actors in the last added case.
		 *
		 * @param actors
		 *        the actors to deny
		 * @return this builder
		 */
		public Builder<T> alsoDeny(TestActor... actors) {
			final SecurityContractCase<T> c = last();
			final Set<TestActor> allowed = new LinkedHashSet<>(c.allowed());
			allowed.removeAll(Arrays.asList(actors));
			return replaceLast(new SecurityContractCase<>(c.name(), c.method(), c.call(),
					Set.copyOf(allowed), c.setup(), c.targetInvokedOnDeny()));
		}

		/**
		 * Add a label to the last added case name.
		 *
		 * <p>
		 * This helps distinguish multiple cases for the same method.
		 * </p>
		 *
		 * @param label
		 *        the label
		 * @return this builder
		 */
		public Builder<T> as(String label) {
			final SecurityContractCase<T> c = last();
			return replaceLast(new SecurityContractCase<>(c.name() + " [" + label + "]", c.method(),
					c.call(), c.allowed(), c.setup(), c.targetInvokedOnDeny()));
		}

		/**
		 * Configure setup to run against each new proxy before the last added
		 * case is invoked.
		 *
		 * <p>
		 * Use this to stub the mocks the aspects use to look up entities. Cases
		 * with setup are not included in
		 * {@link SecurityContract#denyTests(Object)}.
		 * </p>
		 *
		 * @param setup
		 *        the setup
		 * @return this builder
		 */
		public Builder<T> given(Consumer<? super SecuredProxy<T>> setup) {
			final SecurityContractCase<T> c = last();
			return replaceLast(new SecurityContractCase<>(c.name(), c.method(), c.call(), c.allowed(),
					requireNonNullArgument(setup, "setup"), c.targetInvokedOnDeny()));
		}

		/**
		 * Mark the last added case as one whose target may be invoked even when
		 * access is denied, for example when the aspect checks the returned
		 * value.
		 *
		 * @return this builder
		 */
		public Builder<T> targetInvokedOnDeny() {
			final SecurityContractCase<T> c = last();
			return replaceLast(new SecurityContractCase<>(c.name(), c.method(), c.call(), c.allowed(),
					c.setup(), true));
		}

		/**
		 * Build the contract.
		 *
		 * @return the contract
		 */
		public SecurityContract<T> build() {
			return new SecurityContract<>(api, tenants, new ArrayList<>(cases),
					new ArrayList<>(exemptions));
		}

		private Builder<T> add(ApiCall<? super T> call, TestActor... allowed) {
			final Method method = ApiMethods.invokedMethod(api, call);
			cases.add(new SecurityContractCase<>(signature(method), method, call, Set.of(allowed), null,
					false));
			return this;
		}

		private SecurityContractCase<T> last() {
			if ( cases.isEmpty() ) {
				throw new IllegalStateException("No case has been added.");
			}
			return cases.getLast();
		}

		private Builder<T> replaceLast(SecurityContractCase<T> c) {
			cases.set(cases.size() - 1, c);
			return this;
		}

	}

}
