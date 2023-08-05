/* ==================================================================
 * Dnp3TestUtils.java - 5/08/2023 4:21:41 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import net.solarnetwork.central.security.CertificateUtils;

/**
 * Helpers for DNP3 testing.
 * 
 * @author matt
 * @version 1.0
 */
public final class Dnp3TestUtils {

	private Dnp3TestUtils() {
		// not available
	}

	/**
	 * Load certificates from a classpath resource.
	 * 
	 * @param name
	 *        the classpath resource name
	 * @return the certificates
	 */
	public static X509Certificate[] certificatesFromResource(String name) {
		return CertificateUtils.parsePemCertificates(new InputStreamReader(
				Dnp3TestUtils.class.getResourceAsStream(name), StandardCharsets.UTF_8));
	}

}
