/* ==================================================================
 * GlobalMetricTrackerBiz.java - 1/11/2018 11:43:22 AM
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

package net.solarnetwork.central.in.tracker.biz;

import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.in.tracker.domain.GlobalMetricCampaign;
import net.solarnetwork.central.in.tracker.domain.GlobalMetricNodeCampaignPropertyConfig;

/**
 * API for integrating with the global metric tracker system.
 * 
 * @author matt
 * @version 1.0
 */
public interface GlobalMetricTrackerBiz {

	/**
	 * Get all available global metric campaigns.
	 * 
	 * @return the available campaigns, never {@literal null}
	 */
	Iterable<GlobalMetricCampaign> availableGlobalMetricCampaigns();

	/**
	 * Get the opt-in metric property configuration for a node campaign.
	 * 
	 * @param campaignId
	 *        the global metric campaign ID
	 * @param nodeSource
	 *        the node source
	 * @return the available configuration, never {@literal null}
	 */
	Iterable<GlobalMetricNodeCampaignPropertyConfig> optInConfigForNodeSource(String campaignId,
			NodeSourcePK nodeSource);

}
