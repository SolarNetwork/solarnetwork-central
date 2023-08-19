/* ==================================================================
 * TrustedIssuerCertificateInput.java - 18/08/2023 2:58:32 pm
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

package net.solarnetwork.central.user.dnp3.domain;

import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserStringCompositePK;

/**
 * DTO for DNP3 trusted issuer certificate configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class TrustedIssuerCertificateInput
		extends BaseDnp3ConfigurationInput<TrustedIssuerCertificate, UserStringCompositePK> {

	@NotNull
	@NotBlank
	@Size(max = 512)
	private String subjectDn;

	@Override
	public TrustedIssuerCertificate toEntity(UserStringCompositePK id, Instant date) {
		TrustedIssuerCertificate conf = new TrustedIssuerCertificate(id, date);
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(TrustedIssuerCertificate conf) {
		super.populateConfiguration(conf);
	}

	/**
	 * Get the subject distinguished name.
	 * 
	 * @return the subject distinguished name
	 */
	public String getSubjectDn() {
		return subjectDn;
	}

	/**
	 * Set the identifier
	 * 
	 * @param subjectDn
	 *        the subject distinguished name to set
	 */
	public void setSubjectDn(String subjectDn) {
		this.subjectDn = subjectDn;
	}

}
