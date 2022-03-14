/* ==================================================================
 * ArrayJoinCellProcessorTests.java - 1/02/2019 7:40:30 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.standard.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;
import net.solarnetwork.central.datum.export.standard.ArrayJoinCellProcessor;

/**
 * Test cases for the {@link ArrayJoinCellProcessor} class.
 * 
 * @author matt
 * @version 2.0
 */
public class ArrayJoinCellProcessorTests {

	private static final CsvContext ANON_CONTEXT = new CsvContext(1, 2, 3);

	private CellProcessor processor;
	private CellProcessor processorChain;

	@Before
	public void setUp() {
		processor = new ArrayJoinCellProcessor(",");
		processorChain = new ArrayJoinCellProcessor(",", new IdentityCellProcessor());
	}

	@Test
	public void joinStringArray() {
		String[] input = new String[] { "foo", "bar" };
		String expected = "foo,bar";
		assertThat("Array joined", processor.execute(input, ANON_CONTEXT), equalTo(expected));
		assertThat("Array joined", processorChain.execute(input, ANON_CONTEXT), equalTo(expected));
	}

	@Test
	public void joinSingletonStringArray() {
		String[] input = new String[] { "foo" };
		String expected = "foo";
		assertThat("Array joined", processor.execute(input, ANON_CONTEXT), equalTo(expected));
		assertThat("Array joined", processorChain.execute(input, ANON_CONTEXT), equalTo(expected));
	}

	@Test
	public void joinEmptyStringArray() {
		String[] input = new String[0];
		String expected = "";
		assertThat("Array joined", processor.execute(input, ANON_CONTEXT), equalTo(expected));
		assertThat("Array joined", processorChain.execute(input, ANON_CONTEXT), equalTo(expected));
	}
}
