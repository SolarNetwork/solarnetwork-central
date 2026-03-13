/* ==================================================================
 * SqsProperties.java - 29/04/2025 4:31:40 pm
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

package net.solarnetwork.central.support;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.util.ClassUtils;

/**
 * Service properties for the SQS integration.
 * 
 * @author matt
 * @version 1.0
 */
public class SqsProperties {

	private @Nullable String region;
	private @Nullable String queueName;
	private @Nullable String accessKey;
	private @Nullable String secretKey;
	private @Nullable String url;

	/**
	 * Constructor.
	 */
	public SqsProperties() {
		super();
	}

	/**
	 * Create an instance from service properties.
	 * 
	 * <p>
	 * The {@code serviceProperties} keys should be the JavaBean property names
	 * of this class.
	 * </p>
	 * 
	 * @param serviceProperties
	 *        the service properties
	 * @return the new instance, never {@code null}
	 */
	public static SqsProperties ofServiceProperties(Map<String, ?> serviceProperties) {
		SqsProperties props = new SqsProperties();
		ClassUtils.setBeanProperties(props, serviceProperties, true);
		return props;
	}

	/**
	 * Test if the configuration appears valid.
	 * 
	 * <p>
	 * This simply tests for non-null property values, except for {@code url}.
	 * </p>
	 * 
	 * @return {@literal true} if the configuration appears valid
	 */
	public boolean isValid() {
		return (region != null && !region.trim().isEmpty() && queueName != null
				&& !queueName.trim().isEmpty() && accessKey != null && secretKey != null);
	}

	/**
	 * Get the SQS queue region.
	 * 
	 * @return the SQS queue region
	 */
	public final @Nullable String getRegion() {
		return region;
	}

	/**
	 * Set the SQS queue region.
	 * 
	 * @param region
	 *        the SQS queue region
	 */
	public final void setRegion(@Nullable String region) {
		this.region = region;
	}

	/**
	 * Get the SQS queue name.
	 * 
	 * @return the queue name
	 */
	public final @Nullable String getQueueName() {
		return queueName;
	}

	/**
	 * Set the SQS queue name.
	 * 
	 * @param queueName
	 *        the queue name to set
	 */
	public final void setQueueName(@Nullable String queueName) {
		this.queueName = queueName;
	}

	/**
	 * Get the AWS access key.
	 * 
	 * @return the access key
	 */
	public final @Nullable String getAccessKey() {
		return accessKey;
	}

	/**
	 * Set the AWS access key.
	 * 
	 * @param accessKey
	 *        the key to use
	 */
	public final void setAccessKey(@Nullable String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * Get the AWS secret key.
	 * 
	 * @return the AWS secret key
	 */
	public final @Nullable String getSecretKey() {
		return secretKey;
	}

	/**
	 * Set the AWS secret key.
	 * 
	 * @param secretKey
	 *        the AWS secret key to use
	 */
	public final void setSecretKey(@Nullable String secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * Set a queue URL.
	 * 
	 * @return the queue URL
	 */
	public final @Nullable String getUrl() {
		return url;
	}

	/**
	 * Get a queue URL.
	 * 
	 * @param url
	 *        the url to set
	 */
	public final void setUrl(@Nullable String url) {
		this.url = url;
	}

}
