/* ==================================================================
 * RandomConfig.java - 25/10/2024 2:49:16 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.config;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Random data generation configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class RandomConfig {

	/** A qualifier for a "faster" but potentially less secure algorithm. */
	public static final String FAST = "fast";

	/**
	 * A default thread-safe strong random number generator.
	 * 
	 * @return a strong random number generator
	 */
	@Primary
	@Bean
	public RandomGenerator rng() {
		try {
			return SecureRandom.getInstanceStrong();
		} catch ( NoSuchAlgorithmException e ) {
			return new SecureRandom();
		}
	}

	/**
	 * A default thread-safe random number generator.
	 * 
	 * @return a random number generator
	 */
	@Qualifier(FAST)
	@Bean
	public RandomGenerator rngFast() {
		return new SecureRandom();
	}

}
