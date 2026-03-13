/* ==================================================================
 * DogtagAgentCertRequestInfo.java - Oct 15, 2014 7:00:52 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.pki.dogtag;

import org.jspecify.annotations.Nullable;

/**
 * Agent info on a certificate request in Dogtag.
 * 
 * @author matt
 * @version 1.0
 */
public class DogtagAgentCertRequestInfo {

	private @Nullable String requestorName;
	private @Nullable String requestorEmail;
	private @Nullable String subjectDn;
	private @Nullable String csr;

	public final @Nullable String getRequestorName() {
		return requestorName;
	}

	public final void setRequestorName(@Nullable String requestorName) {
		this.requestorName = requestorName;
	}

	public final @Nullable String getRequestorEmail() {
		return requestorEmail;
	}

	public final void setRequestorEmail(@Nullable String requestorEmail) {
		this.requestorEmail = requestorEmail;
	}

	public final @Nullable String getSubjectDn() {
		return subjectDn;
	}

	public final void setSubjectDn(@Nullable String subjectDn) {
		this.subjectDn = subjectDn;
	}

	public final @Nullable String getCsr() {
		return csr;
	}

	public final void setCsr(@Nullable String csr) {
		this.csr = csr;
	}

}
