/* ==================================================================
 * DogtagCertRequestInfo.java - Oct 14, 2014 8:46:57 PM
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

import java.net.URL;

/**
 * Details on a Dogtag CSR.
 * 
 * @author matt
 * @version 1.0
 */
public class DogtagCertRequestInfo {

	private String requestStatus;
	private URL requestURL;
	private URL certURL;

	public String getRequestStatus() {
		return requestStatus;
	}

	public void setRequestStatus(String requestStatus) {
		this.requestStatus = requestStatus;
	}

	public URL getRequestURL() {
		return requestURL;
	}

	public void setRequestURL(URL requestURL) {
		this.requestURL = requestURL;
	}

	public URL getCertURL() {
		return certURL;
	}

	public void setCertURL(URL certURL) {
		this.certURL = certURL;
	}

}
