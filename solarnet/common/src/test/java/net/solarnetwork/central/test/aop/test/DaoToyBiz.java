/* ==================================================================
 * DaoToyBiz.java - 22/09/2026 8:39:25 am
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

import net.solarnetwork.central.domain.Securable;

/**
 * A securable toy service implementation.
 *
 * @author matt
 * @version 1.0
 */
@Securable
public class DaoToyBiz implements ToyBiz {

	@Override
	public String userThing(Long userId) {
		return "user";
	}

	@Override
	public void saveUserThing(Long userId, String value) {
		// nothing
	}

	@Override
	public String nodeThing(Long nodeId) {
		return "node";
	}

	@Override
	public void saveNodeThing(Long nodeId, String value) {
		// nothing
	}

	@Override
	public String unguardedThing(Long userId) {
		return "unguarded";
	}

	@Override
	public String publicThing() {
		return "public";
	}

	@Override
	public String publicThing(String name) {
		return name;
	}

}
