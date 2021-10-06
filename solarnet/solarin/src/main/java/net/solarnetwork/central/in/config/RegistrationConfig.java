/* ==================================================================
 * RegistrationConfig.java - 7/10/2021 11:45:47 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.config;

import static net.solarnetwork.central.user.config.RegistrationBizConfig.USER_REGISTRATION;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.Validator;
import net.solarnetwork.central.security.DelegatingPasswordEncoder;
import net.solarnetwork.central.user.biz.dao.UserValidator;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Registration configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class RegistrationConfig {

	@Value("${app.node.certificate.expire-days:720}")
	public int certificateExpireDays = 720;

	@Value("${app.node.certificate.signature-alg:SHA256WithRSA}")
	public String signatureAlgorithm = "SHA256WithRSA";

	@Bean
	@Qualifier(USER_REGISTRATION)
	public Validator userRegistrationValidator() {
		return new UserValidator();
	}

	@Bean
	@Qualifier(USER_REGISTRATION)
	@SuppressWarnings("deprecation")
	public DelegatingPasswordEncoder userRegistrationPasswordEncoder() {
		Map<String, PasswordEncoder> encoders = new LinkedHashMap<>(2);
		encoders.put("$2a$", new BCryptPasswordEncoder(12, new SecureRandom()));
		encoders.put("{SHA}", new net.solarnetwork.central.security.LegacyPasswordEncoder());
		return new DelegatingPasswordEncoder(encoders);
	}

	@Bean
	@Qualifier(USER_REGISTRATION)
	public BCCertificateService certificateService() {
		BCCertificateService service = new BCCertificateService();
		service.setCertificateExpireDays(certificateExpireDays);
		service.setSignatureAlgorithm(signatureAlgorithm);
		return service;
	}

}
