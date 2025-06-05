/* ==================================================================
 * BulkUploadResult.java - Aug 25, 2014 11:38:20 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.web;

import java.util.List;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * Result object for bulk upload operations.
 * 
 * @author matt
 * @version 2.0
 */
public class BulkUploadResult {

	private List<Object> datum;
	private List<NodeInstruction> instructions;

	/**
	 * Default constructor.
	 */
	public BulkUploadResult() {
		super();
	}

	/**
	 * Get the datum list.
	 * 
	 * @return the datum
	 */
	public List<Object> getDatum() {
		return datum;
	}

	/**
	 * Set the datum list.
	 * 
	 * @param datum
	 *        the list to set
	 */
	public void setDatum(List<Object> datum) {
		this.datum = datum;
	}

	/**
	 * Get the instruction list.
	 * 
	 * @return the instructions
	 */
	public List<NodeInstruction> getInstructions() {
		return instructions;
	}

	/**
	 * Set the instructions list.
	 * 
	 * @param instructions
	 *        the list to set
	 */
	public void setInstructions(List<NodeInstruction> instructions) {
		this.instructions = instructions;
	}

}
