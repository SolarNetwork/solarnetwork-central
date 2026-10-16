/* ==================================================================
 * SnLinkedAccountInfo.java - 15 Oct 2026 12:03:22 pm
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

package net.solarnetwork.central.user.account.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serializable;
import net.solarnetwork.central.user.domain.User;

/**
 * Information about a linked account.
 *
 * @author matt
 * @version 1.0
 */
public record SnLinkedAccountInfo(Long userId, String name, String email) implements Serializable {

	/**
	 * Create an instance from a {@link User}.
	 *
	 * @param user
	 *        the user
	 * @return the new info
	 * @throws IllegalStateException
	 *         if the user's name or email is {@code null}
	 */
	public static SnLinkedAccountInfo forUser(User user) {
		return new SnLinkedAccountInfo(user.id(), nonnull(user.getName(), "Name"),
				nonnull(user.getEmail(), "Email"));
	}

}
