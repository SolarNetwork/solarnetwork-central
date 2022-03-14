/* ==================================================================
 *SqsDestinationProperties.java - 15/06/2020 6:37:43 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.dest.sqs;

import java.util.Map;
import net.solarnetwork.util.ClassUtils;

/**
 * Service properties for the SQS node event hook destination.
 * 
 * @author matt
 * @version 1.0
 */
public class SqsDestinationProperties {

	private String region;
	private String queueName;
	private String accessKey;
	private String secretKey;

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
	 * @return the new instance, never {@literal null}
	 */
	public static SqsDestinationProperties ofServiceProperties(Map<String, ?> serviceProperties) {
		SqsDestinationProperties props = new SqsDestinationProperties();
		ClassUtils.setBeanProperties(props, serviceProperties, true);
		return props;
	}

	/**
	 * Test if the configuration appears valid.
	 * 
	 * <p>
	 * This simply tests for non-null property values.
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
	public String getRegion() {
		return region;
	}

	/**
	 * Set the SQS queue region.
	 * 
	 * @param region
	 *        the SQS queue region
	 */
	public synchronized void setRegion(String region) {
		this.region = region;
	}

	/**
	 * Get the SQS queue name.
	 * 
	 * @return the queue name
	 */
	public String getQueueName() {
		return queueName;
	}

	/**
	 * Set the SQS queue name.
	 * 
	 * @param queueName
	 *        the queue name to set
	 */
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	/**
	 * Get the AWS access key.
	 * 
	 * @return the access key
	 */
	public String getAccessKey() {
		return accessKey;
	}

	/**
	 * Set the AWS access key.
	 * 
	 * @param accessKey
	 *        the key to use
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * Get the AWS secret key.
	 * 
	 * @return the AWS secret key
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * Set the AWS secret key.
	 * 
	 * @param secretKey
	 *        the AWS secret key to use
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

}
