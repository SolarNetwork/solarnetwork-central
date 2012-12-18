/* ==================================================================
 * UserNodeAuthToken.java - Dec 18, 2012 2:57:11 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * A node authorization token.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeAuthToken extends BaseAuthToken {

	private static final long serialVersionUID = 2367702119162997221L;

	private Long nodeId;

	/**
	 * Default constructor.
	 */
	public UserNodeAuthToken() {
		super();
	}

	/**
	 * Create a new, active token.
	 * 
	 * @param token
	 *        the token value
	 * @param nodeId
	 *        the node ID
	 * @param secret
	 *        the secret
	 */
	public UserNodeAuthToken(String token, Long nodeId, String secret) {
		super(token, secret);
		setNodeId(nodeId);
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

}
