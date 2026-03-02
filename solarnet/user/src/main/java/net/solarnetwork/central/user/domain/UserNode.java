/* ==================================================================
 * UserNode.java - Jan 29, 2010 11:46:09 AM
 *
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;

/**
 * A solar node with user details.
 *
 * <p>
 * This object augments a {@link SolarNode} with additional information that
 * nodes themselves are not concerned with, but users are. This allows the
 * {@link SolarNode} object to remain lightweight.
 * </p>
 *
 * @author matt
 * @version 1.5
 */
public class UserNode extends BaseEntity implements UserRelatedEntity<Long> {

	@Serial
	private static final long serialVersionUID = -3247965742224565205L;

	private @Nullable String description;
	private @Nullable String name;
	private boolean requiresAuthorization = false;

	private User user;
	private SolarNode node;

	// transient
	private @Nullable UserNodeCertificate certificate;
	private @Nullable UserNodeTransfer transfer;

	/**
	 * Construct for a user and node.
	 *
	 * @param id
	 *        the ID (node ID)
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 1.5
	 */
	@SuppressWarnings("NullAway")
	public UserNode(Long id) {
		super();
		setId(requireNonNullArgument(id, "id"));
	}

	/**
	 * Construct for a user and node.
	 *
	 * @param user
	 *        the user
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 1.5
	 */
	public UserNode(User user) {
		this(user, new SolarNode());
	}

	/**
	 * Construct for a user and node.
	 *
	 * @param user
	 *        the user
	 * @param node
	 *        the node
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNode(User user, SolarNode node) {
		super();
		this.user = requireNonNullArgument(user, "user");
		this.node = requireNonNullArgument(node, "node");
	}

	public final @Nullable String getDescription() {
		return description;
	}

	public final void setDescription(@Nullable String description) {
		this.description = description;
	}

	public final @Nullable String getName() {
		return name;
	}

	public final void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * Get the node ID and associated name, if available, as a string. If no
	 * name is available, this method returns just the node ID.
	 *
	 * @return The node ID and name as a string.
	 * @since 1.3
	 */
	public final String getIdAndName() {
		StringBuilder buf = new StringBuilder();
		if ( node != null ) {
			buf.append(node.getId());
		}
		if ( name != null && !name.isEmpty() ) {
			buf.append(" - ").append(name);
		}
		return buf.toString();
	}

	public final User getUser() {
		return user;
	}

	public final void setUser(User user) {
		this.user = requireNonNullArgument(user, "user");
	}

	@Override
	public final Long getUserId() {
		return nonnull(user.getId(), "user.id");
	}

	public final SolarNode getNode() {
		return node;
	}

	public final void setNode(SolarNode node) {
		this.node = requireNonNullArgument(node, "node");
	}

	public final @Nullable UserNodeCertificate getCertificate() {
		return certificate;
	}

	public final void setCertificate(@Nullable UserNodeCertificate certificate) {
		this.certificate = certificate;
	}

	public final boolean isRequiresAuthorization() {
		return requiresAuthorization;
	}

	public final void setRequiresAuthorization(boolean requiresAuthorization) {
		this.requiresAuthorization = requiresAuthorization;
	}

	/**
	 * Exposed as a top-level property so that it can be marshalled to clients.
	 *
	 * @return the location, or {@literal null}
	 */
	public final @Nullable SolarLocation getNodeLocation() {
		return (node != null ? node.getLocation() : null);
	}

	public final @Nullable UserNodeTransfer getTransfer() {
		return transfer;
	}

	public final void setTransfer(@Nullable UserNodeTransfer transfer) {
		this.transfer = transfer;
	}

}
