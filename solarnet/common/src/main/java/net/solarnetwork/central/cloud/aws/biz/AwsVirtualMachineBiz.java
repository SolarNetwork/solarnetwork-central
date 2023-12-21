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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.cloud.aws.domain.Ec2VirtualMachine;
import net.solarnetwork.central.cloud.biz.VirtualMachineBiz;
import net.solarnetwork.central.cloud.domain.VirtualMachine;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;
import net.solarnetwork.service.support.BasicIdentifiable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateChange;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TagDescription;

/**
 * AWS SDK implementation of {@link VirtualMachineBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class AwsVirtualMachineBiz extends BasicIdentifiable implements VirtualMachineBiz {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Ec2Client client;

	/**
	 * Constructor.
	 * 
	 * @param region
	 *        the AWS region name
	 * @param accessKey
	 *        the access key
	 * @param accessKeySecret
	 *        the access key secret
	 */
	public AwsVirtualMachineBiz(String region, String accessKey, String accessKeySecret) {
		this(Region.of(region), StaticCredentialsProvider
				.create(AwsBasicCredentials.create(accessKey, accessKeySecret)));
	}

	/**
	 * Constructor.
	 * 
	 * @param region
	 *        the AWS region
	 * @param credentialsProvider
	 *        the credentials provider
	 */
	public AwsVirtualMachineBiz(Region region, AwsCredentialsProvider credentialsProvider) {
		super();
		setUid(UUID.randomUUID().toString());
		// @formatter:off
		client = Ec2Client.builder()
				.region(requireNonNullArgument(region, "region"))
				.credentialsProvider(requireNonNullArgument(credentialsProvider, "credentialsProvider"))
				.build();
		// @formatter:on
	}

	@Override
	public VirtualMachine virtualMachineForName(String name) {
		DescribeTagsRequest tagReq = DescribeTagsRequest.builder()
				.filters(Filter.builder().name("key").values("Name").build(),
						Filter.builder().name("resource-type").values("instance").build())
				.build();
		DescribeTagsResponse tagRes;
		while ( true ) {
			tagRes = client.describeTags(tagReq);
			for ( TagDescription tag : tagRes.tags() ) {
				if ( name.equalsIgnoreCase(tag.value()) ) {
					String instanceId = tag.resourceId();
					return new Ec2VirtualMachine(instanceId, tag.value());
				}
			}
			if ( tagReq.nextToken() == null ) {
				break;
			}
			tagReq = tagReq.toBuilder().nextToken(tagRes.nextToken()).build();
		}

		return null;
	}

	@Override
	public Iterable<VirtualMachine> virtualMachinesForIds(Set<String> ids) {
		DescribeInstancesRequest req = DescribeInstancesRequest.builder().instanceIds(ids).build();
		DescribeInstancesResponse res;
		List<VirtualMachine> results = new ArrayList<>(ids.size());
		while ( true ) {
			res = client.describeInstances(req);
			for ( Reservation reservation : res.reservations() ) {
				for ( Instance inst : reservation.instances() ) {
					results.add(new Ec2VirtualMachine(inst));
				}
			}
			if ( res.nextToken() == null ) {
				break;
			}
			req = req.toBuilder().nextToken(res.nextToken()).build();
		}
		return results;
	}

	@Override
	public Map<String, VirtualMachineState> stateForVirtualMachines(Set<String> machineIds) {
		DescribeInstanceStatusRequest req = DescribeInstanceStatusRequest.builder()
				.instanceIds(machineIds).includeAllInstances(true).build();
		DescribeInstanceStatusResponse res;
		Map<String, VirtualMachineState> result = new LinkedHashMap<>(machineIds.size());
		while ( true ) {
			res = client.describeInstanceStatus(req);
			for ( InstanceStatus status : res.instanceStatuses() ) {
				result.put(status.instanceId(),
						Ec2VirtualMachine.virtualMachineStateForInstanceState(status.instanceState()));
			}
			if ( res.nextToken() == null ) {
				break;
			}
			req = req.toBuilder().nextToken(res.nextToken()).build();
		}
		return result;
	}

	@Override
	public void changeVirtualMachinesState(Set<String> machineIds, VirtualMachineState desiredState) {
		List<InstanceStateChange> results = null;
		if ( desiredState == VirtualMachineState.Running ) {
			StartInstancesRequest req = StartInstancesRequest.builder().instanceIds(machineIds).build();
			StartInstancesResponse res = client.startInstances(req);
			results = res.startingInstances();
		} else if ( desiredState == VirtualMachineState.Stopped ) {
			StopInstancesRequest req = StopInstancesRequest.builder().instanceIds(machineIds).build();
			StopInstancesResponse res = client.stopInstances(req);
			results = res.stoppingInstances();
		} else {
			throw new IllegalArgumentException("Desired state not supported: " + desiredState);
		}
		log.info("Changed EC2 instances {} desired state to {}: {}", machineIds, desiredState, results);
	}

}
