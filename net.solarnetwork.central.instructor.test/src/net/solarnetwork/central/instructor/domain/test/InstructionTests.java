/* ==================================================================
 * InstructionTests.java - 27/01/2021 4:59:33 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.domain.test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Test;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;

/**
 * Test cases for the {@link Instruction} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InstructionTests {

	@Test
	public void setParams() {
		// GIVEN
		Instruction instr = new Instruction("test", new DateTime());

		// WHEN
		Map<String, String> params = new LinkedHashMap<>(2);
		params.put("foo", "bar");
		params.put("bar", "bam");
		instr.setParams(params);

		// THEN
		List<InstructionParameter> list = instr.getParameters();
		assertThat("InstructionParameter instances created", list, hasSize(2));
		assertThat("Param 1 key", list.get(0).getName(), equalTo("foo"));
		assertThat("Param 1 val", list.get(0).getValue(), equalTo("bar"));
		assertThat("Param 2 key", list.get(1).getName(), equalTo("bar"));
		assertThat("Param 2 val", list.get(1).getValue(), equalTo("bam"));
	}

	@Test
	public void getParams() {
		// GIVEN
		Instruction instr = new Instruction("test", new DateTime());
		instr.addParameter("foo", "bar");
		instr.addParameter("bar", "bam");

		// WHEN
		Map<String, String> params = instr.getParams();

		// THEN
		assertThat("Parameters available", params,
				allOf(hasEntry("foo", "bar"), hasEntry("bar", "bam")));
	}

	@Test
	public void getParams_multiKey() {
		// GIVEN
		Instruction instr = new Instruction("test", new DateTime());
		instr.addParameter("foo", "bar");
		instr.addParameter("foo", "bam");

		// WHEN
		Map<String, String> params = instr.getParams();

		// THEN
		assertThat("Multiple parameters for same key merged", params, hasEntry("foo", "barbam"));
	}

}
