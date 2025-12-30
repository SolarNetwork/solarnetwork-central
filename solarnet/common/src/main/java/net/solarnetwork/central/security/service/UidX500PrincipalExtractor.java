/* ==================================================================
 * UidX500PrincipalExtractor.java - 29/12/2025 2:58:38 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.service;

import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.security.auth.x500.X500Principal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.util.Assert;

/**
 * Extract the principal from the {@code UID} DN attribute.
 * 
 * @author matt
 * @version 1.0
 */
public final class UidX500PrincipalExtractor implements X509PrincipalExtractor {

	/** The UID pattern. */
	public static final Pattern UID_SUBJECT_DN_PATTERN = Pattern.compile("UID=(.*?)(?:,|$)",
			Pattern.CASE_INSENSITIVE);

	/** A default instance. */
	public static final X509PrincipalExtractor INSTANCE = new UidX500PrincipalExtractor();

	/**
	 * Constructor.
	 */
	public UidX500PrincipalExtractor() {
		super();
	}

	@Override
	public Object extractPrincipal(X509Certificate cert) {
		Assert.notNull(cert, "clientCert cannot be null");
		X500Principal principal = cert.getSubjectX500Principal();
		String subjectDN = principal.getName(X500Principal.RFC2253);
		Matcher matcher = UID_SUBJECT_DN_PATTERN.matcher(subjectDN);
		if ( !matcher.find() ) {
			throw new BadCredentialsException("No UID was found in subject DN: " + subjectDN);
		}
		return matcher.group(1);
	}

}
