/* ==================================================================
 * PropertySerializerCellProcessor.java - 23/04/2018 9:18:29 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.standard;

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.cellprocessor.ift.DateCellProcessor;
import org.supercsv.util.CsvContext;
import net.solarnetwork.codec.PropertySerializer;

/**
 * {@link CellProcessor} that delegates to a {@link PropertySerializer}.
 * 
 * @author matt
 * @version 2.0
 */
public class PropertySerializerCellProcessor extends CellProcessorAdaptor implements DateCellProcessor {

	private final PropertySerializer propertySerializer;

	/**
	 * Constructor.
	 * 
	 * @param propertySerializer
	 *        the serializer to delegate to
	 */
	public PropertySerializerCellProcessor(PropertySerializer propertySerializer) {
		super();
		this.propertySerializer = propertySerializer;
	}

	/**
	 * Construct with another processor.
	 * 
	 * @param propertySerializer
	 *        the property serializer to delegate to
	 * @param next
	 *        the next processor
	 */
	public PropertySerializerCellProcessor(PropertySerializer propertySerializer, CellProcessor next) {
		super(next);
		this.propertySerializer = propertySerializer;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object execute(Object value, CsvContext context) {
		return propertySerializer.serialize(context.getRowSource(),
				String.valueOf(context.getColumnNumber()), value);
	}

}
