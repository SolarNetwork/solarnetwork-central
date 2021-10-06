/* ==================================================================
 * NodePKIBiz.java - Oct 14, 2014 9:16:50 AM
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

package net.solarnetwork.central.user.biz;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import net.solarnetwork.service.CertificateService;

/**
 * API for managing SolarNode PKI from within SolarUser.
 * 
 * @author matt
 * @version 2.0
 */
public interface NodePKIBiz extends CertificateService {

	/**
	 * Submit a certificate signing request (CSR) and obtain a unique request
	 * ID. The active security user will be used for the CSR requester details.
	 * 
	 * @param certificate
	 *        the certificate to sign and submit to the certification authority
	 *        (CA)
	 * @param privateKey
	 *        the private key to sign the certificate with
	 * @return a unique ID from the CA
	 * @throws net.solarnetwork.central.security.SecurityException
	 *         if the active user is not available
	 */
	String submitCSR(final X509Certificate certificate, final PrivateKey privateKey)
			throws net.solarnetwork.central.security.SecurityException;

	/**
	 * Approve a certificate signing request (CSR) and obtain the certificate
	 * chain. The active security user details must match the requester details
	 * for the given {@code requestID}.
	 * 
	 * @param requestID
	 *        the request ID to approve
	 * @return the certificate, and the rest of the certificates in the chain
	 * @throws net.solarnetwork.central.security.SecurityException
	 *         if the active user is not available
	 */
	X509Certificate[] approveCSR(String requestID)
			throws net.solarnetwork.central.security.SecurityException;

	/**
	 * Submit a request to renew a certificate. The active security user details
	 * must match the certificate details.
	 * 
	 * @param certificate
	 *        The certificate to renew.
	 * @return a unique ID from the CA
	 * @throws net.solarnetwork.central.security.SecurityException
	 *         if the active user is not available
	 */
	String submitRenewalRequest(final X509Certificate certificate)
			throws net.solarnetwork.central.security.SecurityException;

}
