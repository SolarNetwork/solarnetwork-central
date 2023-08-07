/* ==================================================================
 * TrustedIssuerCertificate.java - 5/08/2023 11:41:20 am
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

package net.solarnetwork.central.dnp3.domain;

import java.security.cert.X509Certificate;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.BaseUserCertificate;
import net.solarnetwork.central.domain.UserStringCompositePK;

/**
 * A DNP3 account-wide trusted certificate issuer certificate.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "certificate" })
@JsonPropertyOrder({ "userId", "subjectDn", "created", "modified", "enabled", "expires" })
public class TrustedIssuerCertificate extends BaseUserCertificate<TrustedIssuerCertificate> {

	private static final long serialVersionUID = 4567846203298487079L;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TrustedIssuerCertificate(UserStringCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        ID the user ID
	 * @param subjectDn
	 *        the normalized certificate subject DN
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TrustedIssuerCertificate(Long userId, String subjectDn, Instant created) {
		super(userId, subjectDn, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        ID the user ID
	 * @param subjectDn
	 *        the normalized certificate subject DN
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public TrustedIssuerCertificate(Long userId, X509Certificate certificate, Instant created) {
		super(userId, certificate, created);
	}

	@Override
	public TrustedIssuerCertificate copyWithId(UserStringCompositePK id) {
		var copy = new TrustedIssuerCertificate(id, getCreated());
		copyTo(copy);
		return copy;
	}

}
