/* ==================================================================
 * UserDnp3Controller.java - 7/08/2023 10:24:40 am
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.central.dnp3.config.SolarNetDnp3Configuration.DNP3;
import static net.solarnetwork.domain.Result.success;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.security.CertificateUtils;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.CertificateException;

/**
 * Web service API for DNP3 management.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(DNP3)
@GlobalExceptionRestController
@RestController("v1Dnp3Controller")
@RequestMapping(value = { "/u/sec/dnp3", "/api/v1/sec/user/dnp3" })
public class UserDnp3Controller {

	private final UserDnp3Biz userDnp3Biz;

	/**
	 * Constructor.
	 * 
	 * @param userDnp3Biz
	 *        the user DNP3 service (optional)
	 */

	public UserDnp3Controller(@Autowired(required = false) UserDnp3Biz userDnp3Biz) {
		super();
		this.userDnp3Biz = userDnp3Biz;
	}

	/**
	 * Get the {@link UserDnp3Biz}.
	 * 
	 * @return the service; never {@literal null}
	 * @throws UnsupportedOperationException
	 *         if the service is not available
	 */
	private UserDnp3Biz userDnp3Biz() {
		if ( userDnp3Biz == null ) {
			throw new UnsupportedOperationException("DNP3 service not available.");
		}
		return userDnp3Biz;
	}

	/**
	 * Import a trusted issuer certificate for the current user.
	 * 
	 * @param data
	 *        the input
	 * @return the parsed certificate configurations
	 */
	@RequestMapping(method = POST, value = "/trusted-issuer-certs", consumes = MULTIPART_FORM_DATA_VALUE)
	public Result<Collection<TrustedIssuerCertificate>> saveTrustedIssuerCertificate(
			@RequestPart("file") MultipartFile data) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		CertificateFactory cf = CertificateUtils.x509CertificateFactory();
		Collection<? extends Certificate> certs;
		try {
			certs = cf.generateCertificates(data.getInputStream());
		} catch ( java.security.cert.CertificateException | IOException e ) {
			throw new CertificateException("Error parsing certificate data.", e);
		}
		if ( certs == null || certs.isEmpty() ) {
			return success();
		}
		X509Certificate[] x509Certs = certs.stream().map(X509Certificate.class::cast)
				.toArray(X509Certificate[]::new);
		Collection<TrustedIssuerCertificate> result = userDnp3Biz().saveTrustedIssuerCertificates(userId,
				x509Certs);
		return success(result);
	}

	/**
	 * List trusted issuer certificates for the current user.
	 * 
	 * @param criteria
	 *        the optional criteria; if not provided then list all certificates
	 *        for the active user
	 * @return the results
	 */
	@RequestMapping(method = GET, value = "/trusted-issuer-certs")
	public Result<FilterResults<TrustedIssuerCertificate, UserStringCompositePK>> listTrustedIssuerCertificates(
			final BasicFilter criteria) {
		final Long userId = SecurityUtils.getCurrentActorUserId();
		return success(userDnp3Biz().trustedIssuerCertificatesForUser(userId, criteria));
	}

}
