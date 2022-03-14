/* ==================================================================
 * SqsDestination.java - 16/06/2020 11:09:48 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import net.solarnetwork.central.RemoteServiceException;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.codec.JsonUtils;

/**
 * An SQS client and queue configuration.
 * 
 * @author matt
 * @version 2.0
 */
public final class SqsDestination {

	private static final Logger log = LoggerFactory.getLogger(SqsDestination.class);

	private final AmazonSQS client;
	private final String queueUrl;

	/**
	 * Constructor.
	 * 
	 * @param client
	 *        the client
	 * @param queueUrl
	 *        the queue URL
	 */
	public SqsDestination(AmazonSQS client, String queueUrl) {
		super();
		this.client = requireNonNullArgument(client, "client");
		this.queueUrl = requireNonNullArgument(queueUrl, "queueUrl");
	}

	public void sendJsonMessage(Object msg) {
		String json = JsonUtils.getJSONString(msg, null);
		try {
			client.sendMessage(queueUrl, json);
		} catch ( AmazonServiceException e ) {
			log.warn("AWS error: {}; HTTP code {}; AWS code {}; type {}; request ID {}", e.getMessage(),
					e.getStatusCode(), e.getErrorCode(), e.getErrorType(), e.getRequestId());
			throw new RemoteServiceException(
					String.format("Error publishing SQS message to queue [%s]", queueUrl), e);
		} catch ( AmazonClientException e ) {
			log.debug("Error communicating with AWS: {}", e.getMessage());
			throw new RepeatableTaskException("Error communicating with AWS", e);
		}

	}

	/**
	 * Get the SQS client.
	 * 
	 * @return the client, never {@literal null}
	 */
	public AmazonSQS getClient() {
		return client;
	}

	/**
	 * Get the SQS queue URL.
	 * 
	 * @return the queueUrl the queue URL
	 */
	public String getQueueUrl() {
		return queueUrl;
	}

}
