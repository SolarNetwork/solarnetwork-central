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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.TagDescription;
import net.solarnetwork.central.cloud.aws.domain.Ec2VirtualMachine;
import net.solarnetwork.central.cloud.biz.VirtualMachineBiz;
import net.solarnetwork.central.cloud.domain.VirtualMachine;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;

/**
 * AWS SDK implementation of {@link VirtualMachineBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class AwsVirtualMachineBiz implements VirtualMachineBiz {

	private String uid = UUID.randomUUID().toString();
	private String groupUid;
	private String displayName;
	private String region = "us-west-2";
	private String accessKey;
	private String secretKey;

	private final Logger log = LoggerFactory.getLogger(getClass());

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

	private AmazonEC2 getEc2Client() {
		AmazonEC2 client = ec2Client;
		if ( client == null ) {
			throw new RuntimeException("No EC2 client configured");
		}
		return client;
	}

	@Override
	public VirtualMachine virtualMachineForName(String name) {
		AmazonEC2 client = getEc2Client();
		DescribeTagsRequest tagReq = new DescribeTagsRequest().withFilters(
				new Filter("key").withValues("Name"),
				new Filter("resource-type").withValues("instance"));
		DescribeTagsResult tagRes;
		do {
			tagRes = client.describeTags(tagReq);
			for ( TagDescription tag : tagRes.getTags() ) {
				if ( name.equalsIgnoreCase(tag.getValue()) ) {
					String instanceId = tag.getResourceId();
					return new Ec2VirtualMachine(instanceId, tag.getValue());
				}
			}
			tagReq.setNextToken(tagRes.getNextToken());
		} while ( tagRes.getNextToken() != null );

		return null;
	}

	@Override
	public Iterable<VirtualMachine> virtualMachinesForIds(Set<String> ids) {
		AmazonEC2 client = getEc2Client();
		DescribeInstancesRequest req = new DescribeInstancesRequest().withInstanceIds(ids);
		DescribeInstancesResult res;
		List<VirtualMachine> results = new ArrayList<>(ids.size());
		do {
			res = client.describeInstances(req);
			for ( Reservation reservation : res.getReservations() ) {
				for ( Instance inst : reservation.getInstances() ) {
					results.add(new Ec2VirtualMachine(inst));
				}
			}
			req.setNextToken(res.getNextToken());
		} while ( res.getNextToken() != null );
		return results;
	}

	@Override
	public Map<String, VirtualMachineState> stateForVirtualMachines(Set<String> machineIds) {
		AmazonEC2 client = getEc2Client();
		DescribeInstanceStatusRequest req = new DescribeInstanceStatusRequest()
				.withInstanceIds(machineIds).withIncludeAllInstances(true);
		DescribeInstanceStatusResult res;
		Map<String, VirtualMachineState> result = new LinkedHashMap<>(machineIds.size());
		do {
			res = client.describeInstanceStatus(req);
			for ( InstanceStatus status : res.getInstanceStatuses() ) {
				result.put(status.getInstanceId(), Ec2VirtualMachine
						.virtualMachineStateForInstanceState(status.getInstanceState()));
			}
			req.setNextToken(res.getNextToken());
		} while ( res.getNextToken() != null );
		return result;
	}

	@Override
	public void changeVirtualMachinesState(Set<String> machineIds, VirtualMachineState desiredState) {
		AmazonEC2 client = getEc2Client();
		List<InstanceStateChange> results = null;
		if ( desiredState == VirtualMachineState.Running ) {
			StartInstancesRequest req = new StartInstancesRequest().withInstanceIds(machineIds);
			StartInstancesResult res = client.startInstances(req);
			results = res.getStartingInstances();
		} else if ( desiredState == VirtualMachineState.Stopped ) {
			StopInstancesRequest req = new StopInstancesRequest().withInstanceIds(machineIds);
			StopInstancesResult res = client.stopInstances(req);
			results = res.getStoppingInstances();
		} else {
			throw new IllegalArgumentException("Desired state not supported: " + desiredState);
		}
		log.info("Changed EC2 instances {} desired state to {}: {}", machineIds, desiredState, results);
	}

	@Override
	public String getUid() {
		return uid;
	}

	/**
	 * Set the service unique ID.
	 * 
	 * @param uid
	 *        the unique ID
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getGroupUid() {
		return groupUid;
	}

	/**
	 * Set the service group unique ID.
	 * 
	 * @param groupUid
	 *        the group ID
	 */
	public void setGroupUid(String groupUid) {
		this.groupUid = groupUid;
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Set the service display name.
	 * 
	 * @param displayName
	 *        the display name
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
