/* ==================================================================
 * GlobalMetricNodeCampaignPropertyConfig.java - 1/11/2018 10:10:07 AM
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

package net.solarnetwork.central.in.tracker.domain;

import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;

/**
 * Configuration for a single metric in a global metric campaign.
 * 
 * @author matt
 * @version 1.0
 */
public class GlobalMetricNodeCampaignPropertyConfig extends GeneralDatumSamplePropertyConfig<String> {

	private NodeSourcePK nodeSource;

	/**
	 * Get the node source.
	 * 
	 * @return the node source
	 */
	public NodeSourcePK getNodeSource() {
		return nodeSource;
	}

	/**
	 * Set the node source.
	 * 
	 * @param nodeSource
	 *        the node source to set
	 */
	public void setNodeSource(NodeSourcePK nodeSource) {
		this.nodeSource = nodeSource;
	}

	/**
	 * Get the campaign ID.
	 * 
	 * <p>
	 * This is an alias for
	 * {@link GeneralDatumSamplePropertyConfig#getConfig()}.
	 * </p>
	 * 
	 * @return
	 */
	public String getCampaignId() {
		return getConfig();
	}

	/**
	 * Set the campaign ID.
	 * 
	 * <p>
	 * This is an alias for
	 * {@link GeneralDatumSamplePropertyConfig#setConfig(Object)}.
	 * </p>
	 * 
	 * @param id
	 *        the value to set
	 */
	public void setCompaingnId(String id) {
		setConfig(id);
	}

}
