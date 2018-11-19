/* ==================================================================
 * GeneralNodeDatumComponents.java - 13/11/2018 7:25:54 AM
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
import net.solarnetwork.domain.GeneralDatumSamples;

/**
 * Extension of {@link GeneralNodeDatum} to facilitate serializing into sample
 * components rather than inline sample data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.30
 */
public class GeneralNodeDatumComponents extends GeneralNodeDatum {

	private static final long serialVersionUID = 131116648784886973L;

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumComponents() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the datum to copy
	 */
	public GeneralNodeDatumComponents(GeneralNodeDatum other) {
		super();
		setCreated(other.getCreated());
		setPosted(other.getPosted());
		setNodeId(other.getNodeId());
		setSourceId(other.getSourceId());
		setSamples(other.getSamples());
	}

	@JsonUnwrapped
	public GeneralDatumSamples getSampleComponents() {
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
