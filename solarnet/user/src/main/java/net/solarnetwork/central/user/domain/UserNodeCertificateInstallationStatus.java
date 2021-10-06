/* ==================================================================
 * UserNodeCertificateInstallationStatus.java - 21/07/2016 7:22:02 AM
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

/**
 * The status of a network certificate install process.
 * 
 * @author matt
 * @version 1.0
 */
public enum UserNodeCertificateInstallationStatus {

	/** A request to install the certificate on a node has been queued. */
	RequestQueued,

	/**
	 * The request to install the certificate has been received by the node.
	 */
	RequestReceived,

	/**
	 * The certificate has been installed successfully on the node.
	 */
	Installed,

	/** The node declined the certificate installation request. */
	Declined,

}
