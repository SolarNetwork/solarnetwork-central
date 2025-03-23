/* ==================================================================
 * UserSecretBizConfig.java - 23/03/2025 10:16:31 am
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

package net.solarnetwork.central.user.config;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import jakarta.validation.Validator;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.user.biz.dao.DaoUserSecretBiz;
import net.solarnetwork.central.user.dao.UserKeyPairEntityDao;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.service.CertificateService;

/**
 * Configuration for the user secret services.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetUserConfiguration.USER_SECRETS)
@Configuration(proxyBeanMethods = false)
public class UserSecretBizConfig implements SolarNetUserConfiguration {

	@Autowired
	private SecretsBiz secretsBiz;

	@Autowired
	private CertificateService certificateService;

	@Autowired
	private UserKeyPairEntityDao keyPairDao;

	@Autowired
	private UserSecretEntityDao secretDao;

	@Autowired
	private Validator validator;

	@Bean
	public DaoUserSecretBiz userSecretBiz(
			@Value("${app.user.secret.biz.key-pair-password-hmac-key}") String keyPairPasswordHmacKey,
			@Value("${app.user.secret.biz.secret-encryption-salt}") String secretEncryptionSalt) {
		DaoUserSecretBiz biz = new DaoUserSecretBiz(Clock.systemUTC(), secretsBiz, certificateService,
				keyPairDao, secretDao, keyPairPasswordHmacKey, secretEncryptionSalt);

		biz.setValidator(validator);

		return biz;
	}

}
