/* ==================================================================
 * AwsVirtualMachineBiz.java - 30/10/2017 7:56:31 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cloud.aws.biz;

import java.util.Map;
import java.util.Set;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import net.solarnetwork.central.cloud.biz.VirtualMachineBiz;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;

/**
 * AWS SDK implementation of {@link VirtualMachineBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class AwsVirtualMachineBiz implements VirtualMachineBiz {

	private String region = "us-west-2";
	private String accessKey;
	private String secretKey;

	private AmazonEC2 ec2Client;

	/**
	 * Call after all properties have been configured or changed.
	 */
	public void init() {
		AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(region);
		if ( accessKey != null && secretKey != null ) {
			builder.setCredentials(
					new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
		}
		ec2Client = builder.build();
	}

	@Override
	public Map<String, VirtualMachineState> stateForVirtualMachines(Set<String> machineIds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void changeVirtualMachinesState(Set<String> machineIds, VirtualMachineState state) {
		// TODO Auto-generated method stub

	}

	/**
	 * Set the AWS region.
	 * 
	 * @param region
	 *        the region; defaults to {@literal us-west-2}
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * Set the AWS access key.
	 * 
	 * @param accessKey
	 *        the access key
	 */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/**
	 * Set the AWS secret key.
	 * 
	 * @param secretKey
	 *        the secret key
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

}
