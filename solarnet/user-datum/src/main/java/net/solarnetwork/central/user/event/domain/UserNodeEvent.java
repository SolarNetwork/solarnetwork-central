/* ==================================================================
 * UserNodeEvent.java - 8/06/2020 4:23:50 pm
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

package net.solarnetwork.central.user.event.domain;

import java.time.Instant;
import java.util.UUID;
import net.solarnetwork.central.user.dao.UserNodeRelatedEntity;
import net.solarnetwork.dao.BasicUuidEntity;

/**
 * The combination of a {@link UserNodeEventHookConfiguration} and a
 * {@link UserNodeEventTask}.
 * 
 * @author matt
 * @version 1.1
 */
public class UserNodeEvent extends BasicUuidEntity implements UserNodeRelatedEntity<UUID> {

	private static final long serialVersionUID = -7055529796513860954L;

	private UserNodeEventHookConfiguration config;
	private UserNodeEventTask task;

	public UserNodeEvent(UUID id, Instant created) {
		super(id, created);
	}

	@Override
	public Long getUserId() {
		return (task != null ? task.getUserId() : null);
	}

	@Override
	public Long getNodeId() {
		return (task != null ? task.getNodeId() : null);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserNodeEvent{");
		builder.append(getId());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the configuration.
	 * 
	 * @return the config
	 */
	public UserNodeEventHookConfiguration getConfig() {
		return config;
	}

	/**
	 * Set the configuration.
	 * 
	 * @param config
	 *        the config to set
	 */
	public void setConfig(UserNodeEventHookConfiguration config) {
		this.config = config;
	}

	/**
	 * Get the task.
	 * 
	 * @return the task
	 */
	public UserNodeEventTask getTask() {
		return task;
	}

	/**
	 * Set the task.
	 * 
	 * @param task
	 *        the task to set
	 */
	public void setTask(UserNodeEventTask task) {
		this.task = task;
	}

}
