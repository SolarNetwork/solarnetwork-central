/* ==================================================================
 * SecretsConfig.java - 27/08/2022 3:39:29 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.config;

import static net.solarnetwork.central.biz.SecretsBiz.SECRETS;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.biz.SimpleSecretsBiz;
import net.solarnetwork.central.cloud.aws.biz.AwsSecretsBiz;
import net.solarnetwork.central.cloud.domain.CloudAccessSettings;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Secret management configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SecretsConfig {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@ConfigurationProperties(prefix = "app.secrets.cache")
	public CacheSettings secretsCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(SECRETS)
	public Cache<String, String> secretsCache() {
		CacheSettings settings = secretsCacheSettings();
		return settings.createCache(cacheManager, String.class, String.class, SECRETS);
	}

	@Configuration
	@Profile("aws-secrets")
	public static class AwsSecretsConfig {

		@Bean
		@ConfigurationProperties(prefix = "app.secrets.aws")
		public CloudAccessSettings awsSecretsSettings() {
			return new CloudAccessSettings();
		}

		@Bean
		public SecretsBiz awsSecretsBiz(@Qualifier(SECRETS) Cache<String, String> cache) {
			CloudAccessSettings settings = awsSecretsSettings();
			AwsSecretsBiz biz = new AwsSecretsBiz(settings.getRegion(), settings.getAccessToken(),
					settings.getAccessSecret());
			biz.setSecretCache(cache);
			return biz;
		}

	}

	@Configuration
	@Profile("!aws-secrets")
	public static class SimpleSecretsConfig {

		@Value("${app.secrets.simple.dir:var/secrets}")
		private Path dir = Paths.get("var/secrets");

		@Value("${app.secrets.simple.password:Secret.123}")
		private String password = "Secret.123";

		@Bean
		public SecretsBiz simpleSecretsBiz() {
			return new SimpleSecretsBiz(dir, password);
		}

	}

}
