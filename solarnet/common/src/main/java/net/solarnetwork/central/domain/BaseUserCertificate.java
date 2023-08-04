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
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * Base user-related certificate entity.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseUserCertificate<C extends BaseUserCertificate<C>>
		extends BasicEntity<UserStringCompositePK>
		implements Entity<UserStringCompositePK>, UserRelatedEntity<UserStringCompositePK>,
		CopyingIdentity<UserStringCompositePK, C>, Differentiable<C>, Serializable, Cloneable {

	private static final long serialVersionUID = -1255883832923942269L;

	private Instant modified;
	private boolean enabled;
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

	@SuppressWarnings("unchecked")
	@Override
	public C clone() {
		return (C) super.clone();
	}

	@Override
	public void copyTo(C entity) {
		entity.setModified(modified);
		entity.setEnabled(enabled);
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
	public boolean isSameAs(C other) {
		// @formatter:off
		return (this.enabled == other.isEnabled() 
				&& Objects.equals(this.certificate, other.getCertificate()));
		// @formatter:on
	}

	@Override
	public boolean differsFrom(C other) {
		return !isSameAs(other);
	}

	@Override
	public Long getUserId() {
		UserStringCompositePK pk = getId();
		return (pk != null ? pk.getUserId() : null);
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
	 * Get the modification date.
	 * 
	 * @return the modified date
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 * 
	 * @param modified
	 *        the modified date to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	/**
	 * Get the enabled flag.
	 * 
	 * @return {@literal true} if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag.
	 * 
	 * @param enabled
	 *        the value to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

}
