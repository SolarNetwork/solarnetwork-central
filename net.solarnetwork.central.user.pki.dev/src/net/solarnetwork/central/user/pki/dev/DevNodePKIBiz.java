/* ==================================================================
 * DevNodePKIBiz.java - Jan 23, 2015 5:31:54 PM
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

package net.solarnetwork.central.user.pki.dev;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.user.biz.NodePKIBiz;
import net.solarnetwork.support.CertificateException;
import net.solarnetwork.support.CertificateService;

/**
 * Developer implementation of {@link NodePKIBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DevNodePKIBiz implements NodePKIBiz {

	private CertificateService certificateService;

	@Override
	public String submitCSR(X509Certificate certificate, PrivateKey privateKey) throws SecurityException {
		String csr = certificateService.generatePKCS10CertificateRequestString(certificate, privateKey);
		return null;
	}

	@Override
	public X509Certificate[] approveCSR(String requestID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public X509Certificate generateCertificate(String dn, PublicKey publicKey, PrivateKey privateKey)
			throws CertificateException {
		return certificateService.generateCertificate(dn, publicKey, privateKey);
	}

	@Override
	public String generatePKCS10CertificateRequestString(X509Certificate cert, PrivateKey privateKey)
			throws CertificateException {
		return certificateService.generatePKCS10CertificateRequestString(cert, privateKey);
	}

	@Override
	public String generatePKCS7CertificateChainString(X509Certificate[] chain)
			throws CertificateException {
		return certificateService.generatePKCS7CertificateChainString(chain);
	}

	@Override
	public X509Certificate[] parsePKCS7CertificateChainString(String pem) throws CertificateException {
		return certificateService.parsePKCS7CertificateChainString(pem);
	}

	public void setCertificateService(CertificateService certificateService) {
		this.certificateService = certificateService;
	}

}
