/* ==================================================================
 * OwnedGeneralNodeDatum.java - 11/10/2022 7:55:41 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.util.ObjectUtils;

/**
 * Extension of {@link GeneralNodeDatum} with account ownership information.
 * 
 * @author matt
 * @version 1.0
 */
public class OwnedGeneralNodeDatum extends GeneralNodeDatum {

	private static final long serialVersionUID = 8431178252197830478L;

	private final Long userId;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user (owner) ID
	 */
	public OwnedGeneralNodeDatum(Long userId) {
		super();
		this.userId = ObjectUtils.requireNonNullArgument(userId, "userId");
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the userId
	 */
	@JsonIgnore
	public Long getUserId() {
		return userId;
	}

}
