/* ========================================================================
 * Copyright 2018 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.config;

import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;

/**
 * Configuration for application-level caching.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * A cache name to use for lists of {@link Actor} objects.
   */
  public static final String ACTOR_CACHE_NAME = "Actor";

  @Autowired(required = false)
  private javax.cache.CacheManager cacheManager;

  @Value("${cache.actor.ttl:900}")
  private int actorCacheSeconds = 900;

  /**
   * Get the actor cache.
   * 
   * @return the actor cache
   */
  @Bean
  @Qualifier("actor")
  @Profile("!default")
  public Cache<String, Actor> actorCache() {
    if (cacheManager == null) {
      return null;
    }
    return cacheManager.createCache(ACTOR_CACHE_NAME, actorCacheConfiguration());
  }

  // CHECKSTYLE IGNORE LineLength FOR NEXT 1 LINE
  private javax.cache.configuration.Configuration<String, Actor> actorCacheConfiguration() {
    MutableConfiguration<String, Actor> conf = new MutableConfiguration<>();
    conf.setExpiryPolicyFactory(
        CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, actorCacheSeconds)));
    conf.setStoreByValue(false);
    return conf;
  }

}
