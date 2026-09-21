/* ==================================================================
 * TestActor.java - 22/09/2026 8:39:25 am
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

package net.solarnetwork.central.test.tenant;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.security.SecurityUtils;

/**
 * A named actor that can become the active security principal.
 *
 * @author matt
 * @version 1.0
 */
public final class TestActor {

	/** The anonymous (unauthenticated) actor. */
	public static final TestActor ANONYMOUS = new TestActor("anonymous",
			SecurityContextHolder::clearContext);

	private final String name;
	private final Runnable authenticator;

	/**
	 * Constructor.
	 *
	 * @param name
	 *        the display name
	 * @param authenticator
	 *        the action that makes this actor the active security principal
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public TestActor(String name, Runnable authenticator) {
		super();
		this.name = requireNonNullArgument(name, "name");
		this.authenticator = requireNonNullArgument(authenticator, "authenticator");
	}

	/**
	 * Create a user actor.
	 *
	 * @param name
	 *        the display name
	 * @param userId
	 *        the user ID
	 * @param email
	 *        the user email
	 * @return the actor
	 */
	public static TestActor user(String name, Long userId, String email) {
		return new TestActor(name, () -> SecurityUtils.becomeUser(email, name, userId));
	}

	/**
	 * Create a security token actor.
	 *
	 * @param name
	 *        the display name
	 * @param token
	 *        the token
	 * @return the actor
	 */
	public static TestActor token(String name, TestToken token) {
		return new TestActor(name, () -> SecurityUtils.becomeToken(token.tokenId(), token.type(),
				token.userId(), token.policy()));
	}

	/**
	 * Create a node actor.
	 *
	 * @param name
	 *        the display name
	 * @param nodeId
	 *        the node ID
	 * @return the actor
	 */
	public static TestActor node(String name, Long nodeId) {
		return new TestActor(name, () -> SecurityUtils.becomeNode(nodeId));
	}

	/**
	 * Make this actor the active security principal.
	 */
	public void become() {
		SecurityContextHolder.clearContext();
		authenticator.run();
	}

	/**
	 * Get the display name.
	 *
	 * @return the name
	 */
	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

}
