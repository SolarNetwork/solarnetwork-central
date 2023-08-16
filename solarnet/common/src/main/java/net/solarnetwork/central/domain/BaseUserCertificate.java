/* ==================================================================
 * UserCertificate.java - 5/08/2023 11:05:09 am
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

package net.solarnetwork.central.domain;

import static net.solarnetwork.central.security.CertificateUtils.canonicalSubjectDn;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.service.CertificateException;

/**
 * Base user-related certificate entity.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseUserCertificate<C extends BaseUserCertificate<C>>
		extends BaseUserModifiableEntity<C, UserStringCompositePK> {

	private static final long serialVersionUID = -8325998663783331582L;

	private X509Certificate certificate;

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
	public BaseUserCertificate(UserStringCompositePK id, Instant created) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
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
	public BaseUserCertificate(Long userId, String subjectDn, Instant created) {
		super(new UserStringCompositePK(userId, subjectDn), created);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code subjectDn} value will be extracted from the certificate.
	 * </p>
	 * 
	 * @param user
	 *        ID the user ID
	 * @param certificate
	 *        the certificate
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseUserCertificate(Long userId, X509Certificate certificate, Instant created) {
		super(new UserStringCompositePK(userId, canonicalSubjectDn(certificate)), created);
		setCertificate(certificate);
	}

	@Override
	public void copyTo(C entity) {
		super.copyTo(entity);
		entity.setCertificate(certificate);
	}

	/**
	 * Test if this entity has the same property values as another.
	 * 
	 * <p>
	 * The {@code id}, {@code created}, and {@code modified} properties are not
	 * compared.
	 * </p>
	 * 
	 * @param other
	 *        the entity to compare to
	 * @return {@literal true} if the properties of this entity are equal to the
	 *         other's
	 */
	@Override
	public boolean isSameAs(C other) {
		return (super.isSameAs(other) && Objects.equals(this.certificate, other.getCertificate()));
	}

	/**
	 * Get the certificate subject DN.
	 * 
	 * @return the subject DN
	 */
	public String getSubjectDn() {
		UserStringCompositePK pk = getId();
		return (pk != null ? pk.getEntityId() : null);
	}

	/**
	 * Get the certificate.
	 * 
	 * @return the certificate
	 */
	public X509Certificate getCertificate() {
		return certificate;
	}

	/**
	 * Set the certificate.
	 * 
	 * @param certificate
	 *        the certificate to set
	 */
	public void setCertificate(X509Certificate certificate) {
		this.certificate = certificate;
	}

	/**
	 * Get the certificate expiration date.
	 * 
	 * @return the expiration date, or {@literal null} if the certificate is not
	 *         set
	 */
	public Instant getExpires() {
		final X509Certificate cert = getCertificate();
		if ( cert == null ) {
			return null;
		}
		return cert.getNotAfter().toInstant();
	}

	/**
	 * Get the DER-encoded certificate data.
	 * 
	 * @return the certificate data
	 */
	public byte[] certificateData() {
		final X509Certificate cert = getCertificate();
		try {
			return (cert != null ? cert.getEncoded() : null);
		} catch ( CertificateEncodingException e ) {
			throw new CertificateException(e);
		}
	}

}
