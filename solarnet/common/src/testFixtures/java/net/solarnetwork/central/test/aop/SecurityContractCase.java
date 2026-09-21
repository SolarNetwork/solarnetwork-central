/* ==================================================================
 * SecurityContractCase.java - 22/09/2026 8:39:25 am
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

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.test.tenant.TestActor;

/**
 * A security contract case: one invocation of an API method and the actors
 * allowed to make it.
 *
 * @param <T>
 *        the API type
 * @param name
 *        the case display name
 * @param method
 *        the API method the case invokes
 * @param call
 *        the invocation
 * @param allowed
 *        the actors allowed to make the invocation; all other actors must be
 *        denied
 * @param setup
 *        optional setup to run against a new proxy before the invocation,
 *        for example to stub mocks the aspect uses to look up entities
 * @param targetInvokedOnDeny
 *        {@code true} if the proxy target may be invoked even when access is
 *        denied, for example when the aspect checks the returned value
 * @author matt
 * @version 1.0
 */
public record SecurityContractCase<T>(String name, Method method, ApiCall<? super T> call,
		Set<TestActor> allowed, @Nullable Consumer<? super SecuredProxy<T>> setup,
		boolean targetInvokedOnDeny) {

	/**
	 * Test if an actor is allowed to make the invocation.
	 *
	 * @param actor
	 *        the actor
	 * @return {@code true} if the actor is allowed
	 */
	public boolean isAllowed(TestActor actor) {
		return allowed.contains(actor);
	}

	/**
	 * Test if the case depends only on the invocation arguments.
	 *
	 * <p>
	 * Such cases do not rely on stubbed mocks, and so can also verify
	 * application service beans.
	 * </p>
	 *
	 * @return {@code true} if the case does not have any setup
	 */
	public boolean argumentsOnly() {
		return setup == null;
	}

}
