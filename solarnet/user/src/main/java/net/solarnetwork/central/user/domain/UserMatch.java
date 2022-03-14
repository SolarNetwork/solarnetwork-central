/* ==================================================================
 * UserMatch.java - 21/08/2017 11:37:42 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

/**
 * A "match" to a {@link User} entity.
 * 
 * Although this class extends {@link User} that is merely an implementation
 * detail.
 * 
 * @author matt
 * @version 1.0
 * @since 1.25
 */
public class UserMatch extends User implements UserFilterMatch {

	private static final long serialVersionUID = -3527259710501547791L;

	/**
	 * Default constructor.
	 */
	public UserMatch() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param userId
	 *        the uesr ID
	 * @param email
	 *        the email
	 */
	public UserMatch(Long userId, String email) {
		super(userId, email);
	}

}
