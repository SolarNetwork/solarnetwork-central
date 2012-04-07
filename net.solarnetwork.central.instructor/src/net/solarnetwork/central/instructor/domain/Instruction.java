/* ==================================================================
 * Instruction.java - Mar 1, 2011 11:21:25 AM
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

package net.solarnetwork.central.instructor.domain;

import java.util.ArrayList;
import java.util.List;

import net.solarnetwork.central.domain.BaseEntity;

import org.joda.time.DateTime;

/**
 * Domain object for an individual instruction.
 * 
 * @author matt
 * @version $Revision$
 */
public class Instruction extends BaseEntity  {

	private static final long serialVersionUID = 4799093764907658857L;

	private String topic;
	private DateTime instructionDate;
	private InstructionState state = InstructionState.Unknown;
	private List<InstructionParameter> parameters;

	/**
	 * Default constructor.
	 */
	public Instruction() {
		super();
	}
	
	/**
	 * Construct with data.
	 * 
	 * @param topic the topic
	 * @param instructionDate the instruction date
	 */
	public Instruction(String topic, DateTime instructionDate) {
		super();
		this.topic = topic;
		this.instructionDate = instructionDate;
	}
	
	/**
	 * Remove all parameters.
	 */
	public void clearParameters() {
		parameters.clear();
	}
	
	/**
	 * Add a parameter value.
	 * 
	 * @param key the key
	 * @param value the value
	 */
	public void addParameter(String key, String value) {
		if ( parameters == null ) {
			parameters = new ArrayList<InstructionParameter>(5);
		}
		parameters.add(new InstructionParameter(key, value));
	}

	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public DateTime getInstructionDate() {
		return instructionDate;
	}
	public void setInstructionDate(DateTime instructionDate) {
		this.instructionDate = instructionDate;
	}
	public InstructionState getState() {
		return state;
	}
	public void setState(InstructionState state) {
		this.state = state;
	}
	public List<InstructionParameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<InstructionParameter> parameters) {
		this.parameters = parameters;
	}

}
