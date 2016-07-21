/* ==================================================================
 * BasicUserNodeCertificateRenewal.java - 21/07/2016 7:36:56 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;

/**
 * Basic implementation of {@link UserNodeCertificateRenewal}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicUserNodeCertificateRenewal extends NetworkAssociationDetails
		implements UserNodeCertificateRenewal {

	private static final long serialVersionUID = 3537089462856128834L;

	private UserNodeCertificateInstallationStatus installationStatus;

	/**
	 * Default constructor.
	 */
	public BasicUserNodeCertificateRenewal() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the NetworkAssociation to copy
	 */
	public BasicUserNodeCertificateRenewal(NetworkAssociation other) {
		super(other);
		if ( other instanceof BasicUserNodeCertificateRenewal ) {
			BasicUserNodeCertificateRenewal otherRenewal = (BasicUserNodeCertificateRenewal) other;
			setInstallationStatus(otherRenewal.getInstallationStatus());
		}
	}

	@Override
	public UserNodeCertificateInstallationStatus getInstallationStatus() {
		return installationStatus;
	}

	public void setInstallationStatus(UserNodeCertificateInstallationStatus installationStatus) {
		this.installationStatus = installationStatus;
	}

}
