/* ==================================================================
 * GeneralLocationDatumComponents.java - 13/11/2018 7:49:41 AM
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

package net.solarnetwork.central.datum.domain;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Extension of {@link GeneralLocationDatum} to facilitate serializing into
 * sample components rather than inline sample data.
 * 
 * @author matt
 * @version 2.0
 * @since 1.30
 */
public class GeneralLocationDatumComponents extends GeneralLocationDatum {

	private static final long serialVersionUID = -4036964378594823693L;

	/**
	 * Default constructor.
	 */
	public GeneralLocationDatumComponents() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the datum to copy
	 */
	public GeneralLocationDatumComponents(GeneralLocationDatum other) {
		super();
		setCreated(other.getCreated());
		setPosted(other.getPosted());
		setLocationId(other.getLocationId());
		setSourceId(other.getSourceId());
		setSamples(other.getSamples());
	}

	@JsonUnwrapped
	public DatumSamples getSampleComponents() {
		return getSamples();
	}

	/**
	 * This implementation returns {@literal null} so the data is not unwrapped
	 * during serialization.
	 */
	@Override
	public Map<String, ?> getSampleData() {
		return null;
	}

}
