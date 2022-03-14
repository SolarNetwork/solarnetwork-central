/* ==================================================================
 * Ec2VirtualMachine.java - 31/10/2017 7:11:15 AM
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

package net.solarnetwork.central.cloud.aws.domain;

import java.util.List;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Tag;
import net.solarnetwork.central.cloud.domain.VirtualMachine;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;
import net.solarnetwork.central.dao.BaseObjectEntity;

/**
 * EC2 implementation of {@link VirtualMachine}.
 * 
 * @author matt
 * @version 1.0
 */
public class Ec2VirtualMachine extends BaseObjectEntity<String> implements VirtualMachine {

	private static final long serialVersionUID = -4700896078284783343L;

	private final String displayName;
	private VirtualMachineState state = VirtualMachineState.Unknown;

	public Ec2VirtualMachine(String instanceId, String displayName) {
		super();
		setId(instanceId);
		this.displayName = displayName;
	}

	public Ec2VirtualMachine(Instance instance) {
		this(instance.getInstanceId(), displayNameForInstance(instance));
		this.state = virtualMachineStateForInstanceState(instance.getState());
	}

	/**
	 * Get a display name for an EC2 instance.
	 * 
	 * <p>
	 * This method will return the first {@literal Name} tag value, falling back
	 * to the instance ID if not found.
	 * </p>
	 * 
	 * @param instance
	 *        the instance to get the display name for
	 * @return the name, never {@literal null}
	 */
	public static final String displayNameForInstance(Instance instance) {
		List<Tag> tags = instance.getTags();
		if ( tags == null ) {
			return instance.getInstanceId();
		} else {
			return instance.getTags().stream().filter(t -> "name".equalsIgnoreCase(t.getKey()))
					.findFirst().map(t -> t.getValue()).orElse(instance.getInstanceId());
		}
	}

	/**
	 * Get a {@link VirtualMachineState} for a given EC2 {@code InstanceState}.
	 * 
	 * @param state
	 *        the state
	 * @return the state enum value, never {@literal null}
	 */
	public static final VirtualMachineState virtualMachineStateForInstanceState(InstanceState state) {
		if ( state == null ) {
			return VirtualMachineState.Unknown;
		}
		byte s = (byte) (state.getCode().intValue() & 0xFF);
		switch (s) {
			case 0:
				return VirtualMachineState.Starting;

			case 16:
				return VirtualMachineState.Running;

			case 32:
			case 64:
				return VirtualMachineState.Stopping;

			case 48:
				return VirtualMachineState.Terminated;

			case 80:
				return VirtualMachineState.Stopped;

			default:
				return VirtualMachineState.Unknown;
		}
	}

	@Override
	public String getDisplayName() {
		return displayName;
	}

	@Override
	public VirtualMachineState getState() {
		return state;
	}

	/**
	 * Set the machine state.
	 * 
	 * @param state
	 *        the state
	 */
	public void setState(VirtualMachineState state) {
		this.state = state;
	}

}
