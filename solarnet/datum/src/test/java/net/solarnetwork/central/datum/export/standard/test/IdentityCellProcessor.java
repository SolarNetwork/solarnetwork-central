/* ==================================================================
 * IdentityCellProcessor.java - 1/02/2019 7:44:11 am
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

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

/**
 * CSV cell processor for testing.
 * 
 * @author matt
 * @version 1.0
 */
public class IdentityCellProcessor extends CellProcessorAdaptor {

	/**
	 * Constructor.
	 */
	public IdentityCellProcessor() {
		super();
	}

	/**
	 * Constructor with a chain.
	 * 
	 * @param next
	 *        the next processor in the chain
	 */
	public IdentityCellProcessor(CellProcessor next) {
		super(next);
	}

	@Override
	public <T> T execute(Object value, CsvContext context) {
		return next.execute(value, context);
	}

}
