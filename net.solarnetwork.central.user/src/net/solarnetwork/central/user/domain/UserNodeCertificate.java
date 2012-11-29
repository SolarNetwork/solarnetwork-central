/* ==================================================================
 * UserNodeCertificate.java - Nov 29, 2012 8:19:09 PM
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

import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.domain.SolarNode;

/**
 * A user node certificate.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeCertificate extends BaseEntity {

	private static final long serialVersionUID = -4169844309969371159L;

	private byte[] certificate;
	private UserNodeCertificateStatus status;

	private User user;
	private SolarNode node;

	public byte[] getCertificate() {
		return certificate;
	}

	public void setCertificate(byte[] certificate) {
		this.certificate = certificate;
	}

	public UserNodeCertificateStatus getStatus() {
		return status;
	}

	public void setStatus(UserNodeCertificateStatus status) {
		this.status = status;
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
