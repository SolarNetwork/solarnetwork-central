/* ==================================================================
 * UserNodeTransfer.java - Apr 20, 2015 4:46:53 PM
 *
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import java.io.Serial;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.BaseObjectEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A node ownership transfer request. This entity is associated with the node
 * requesting the transfer. The request is sent to the email address provided on
 * this entity.
 *
 * @author matt
 * @version 3.0
 */
public class UserNodeTransfer extends BaseObjectEntity<UserNodeTransfer, UserNodePK>
		implements UserRelatedEntity<UserNodeTransfer, UserNodePK> {

	@Serial
	private static final long serialVersionUID = -1316805739552206861L;

	private String email;

	private User user;
	private SolarNode node;

	/**
	 * Default constructor.
	 */
	public UserNodeTransfer() {
		super();
		setId(new UserNodePK());
	}

	/**
	 * Construct with values.
	 *
	 * @param userId
	 *        The user ID.
	 * @param nodeId
	 *        The node ID.
	 * @param email
	 *        The email.
	 */
	public UserNodeTransfer(Long userId, Long nodeId, String email) {
		super();
		setUserId(userId);
		setNodeId(nodeId);
		setEmail(email);
	}

	/**
	 * Get the email of the requested new owner of the node.
	 *
	 * @return The email address.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Set the email of the requested new owner of the node.
	 *
	 * @param email
	 *        The email address to set.
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Convenience getter for {@link UserNodePK#getNodeId()}.
	 *
	 * @return the nodeId
	 */
	public Long getNodeId() {
		UserNodePK id = getId();
		return (id == null ? null : id.getNodeId());
	}

	/**
	 * Convenience setter for {@link UserNodePK#setNodeId(Long)}.
	 *
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		UserNodePK id = getId();
		if ( id == null ) {
			id = new UserNodePK();
			setId(id);
		}
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link UserNodePK#getUserId()}.
	 *
	 * @return the userId
	 */
	@Override
	public Long getUserId() {
		UserNodePK id = getId();
		return (id == null ? null : id.getUserId());
	}

	/**
	 * Convenience setter for {@link UserNodePK#setUserId(Long)}.
	 *
	 * @param userId
	 *        the userId to set
	 */
	public void setUserId(Long userId) {
		UserNodePK id = getId();
		if ( id == null ) {
			id = new UserNodePK();
			setId(id);
		}
		id.setUserId(userId);
	}

	@JsonIgnore
	@SerializeIgnore
	@Override
	public UserNodePK getId() {
		return super.getId();
	}

	@Override
	public String toString() {
		return "UserNodeTransfer{" + getId() + "}";
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public SolarNode getNode() {
		return node;
	}

	public void setNode(SolarNode node) {
		this.node = node;
	}

}
