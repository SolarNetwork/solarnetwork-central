/* ==================================================================
 * UserSecretDaoConfig.java - 22/03/2025 2:23:40 pm
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

import java.security.KeyPair;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.user.dao.DefaultUserSecretAccessDao;
import net.solarnetwork.central.user.dao.UserKeyPairEntityDao;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.central.user.dao.jdbc.JdbcUserKeyPairEntityDao;
import net.solarnetwork.central.user.dao.jdbc.JdbcUserSecretEntityDao;
import net.solarnetwork.central.user.domain.UserSecretEntity;

/**
 * Configuration for user secret DAO implementations.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetUserConfiguration.USER_SECRETS)
@Configuration(proxyBeanMethods = false)
public class UserSecretDaoConfig implements SolarNetUserConfiguration {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private SecretsBiz secretsBiz;

	@Autowired(required = false)
	@Qualifier(USER_SECRET)
	private Cache<UserStringStringCompositePK, UserSecretEntity> userSecretCache;

	@Autowired(required = false)
	@Qualifier(USER_KEYPAIR)
	private Cache<UserStringCompositePK, KeyPair> userKeyPairCache;

	/**
	 * The user key pair DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public UserKeyPairEntityDao userKeyPairEntityDao() {
		return new JdbcUserKeyPairEntityDao(jdbcOperations);
	}

	/**
	 * The user secret DAO.
	 *
	 * @return the DAO
	 */
	@Bean
	public UserSecretEntityDao userSecretEntityDao() {
		return new JdbcUserSecretEntityDao(jdbcOperations);
	}

	/**
	 * The user secret access DAO.
	 * 
	 * @param secretEncryptionSalt
	 *        the encryption salt to use
	 * @param keyPairDao
	 *        the key pair DAO
	 * @param secretDao
	 *        the secret DAO
	 * @return the access DAO
	 */
	@Bean
	public DefaultUserSecretAccessDao userSecretAccessDao(
			@Value("${app.user.secret.biz.secret-encryption-salt}") String secretEncryptionSalt,
			UserKeyPairEntityDao keyPairDao, UserSecretEntityDao secretDao) {
		DefaultUserSecretAccessDao dao = new DefaultUserSecretAccessDao(secretsBiz, keyPairDao,
				secretDao, secretEncryptionSalt);

		dao.setKeyPairCache(userKeyPairCache);
		dao.setSecretCache(userSecretCache);

		return dao;
	}

}
