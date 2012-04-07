/* ==================================================================
 * UserNodeHardwareControl.java - Oct 2, 2011 10:04:51 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.user.domain;

import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.domain.HardwareControl;

/**
 * A user node hardware control configuration element.
 * 
 * <p>This defines a relationship between a node control and a HardwareControl entity.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class UserNodeHardwareControl extends BaseEntity {

	private static final long serialVersionUID = 9220118628409582403L;

	private Long nodeId;
	private String sourceId;
	private String name;
	private HardwareControl control;
	
	public Long getNodeId() {
		return nodeId;
	}
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}
	public HardwareControl getControl() {
		return control;
	}
	public void setControl(HardwareControl control) {
		this.control = control;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSourceId() {
		return sourceId;
	}
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
	
}
