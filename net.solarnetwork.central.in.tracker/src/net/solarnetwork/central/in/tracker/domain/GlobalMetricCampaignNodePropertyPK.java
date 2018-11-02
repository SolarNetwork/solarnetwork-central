/* ==================================================================
 * GlobalMetricCampaignNodePropertyPK.java - 2/11/2018 2:45:20 PM
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

import java.io.Serializable;
import java.util.Objects;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Primary key for a tracker campaign-specific property opt-in configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class GlobalMetricCampaignNodePropertyPK
		implements Serializable, Cloneable, Comparable<GlobalMetricCampaignNodePropertyPK> {

	private static final long serialVersionUID = 6816261289116649563L;

	private String campaignId;
	private Long nodeId;
	private String sourceId;
	private GeneralDatumSamplesType propertyType;
	private String propertyName;

	/**
	 * Default constructor.
	 */
	public GlobalMetricCampaignNodePropertyPK() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param campaignId
	 *        the campaign ID
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param propertyType
	 *        the property type
	 * @param propertyName
	 *        the property name
	 */
	public GlobalMetricCampaignNodePropertyPK(String campaignId, Long nodeId, String sourceId,
			GeneralDatumSamplesType propertyType, String propertyName) {
		super();
		this.campaignId = campaignId;
		this.nodeId = nodeId;
		this.sourceId = sourceId;
		this.propertyType = propertyType;
		this.propertyName = propertyName;
	}

	@Override
	public int compareTo(GlobalMetricCampaignNodePropertyPK o) {
		if ( o == null ) {
			return 1;
		}
		if ( o.campaignId == null ) {
			return 1;
		} else if ( campaignId == null ) {
			return -1;
		}
		int comparison = campaignId.compareTo(o.campaignId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.nodeId == null ) {
			return 1;
		} else if ( nodeId == null ) {
			return -1;
		}
		comparison = nodeId.compareTo(o.nodeId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.sourceId == null ) {
			return 1;
		} else if ( sourceId == null ) {
			return -1;
		}
		comparison = sourceId.compareToIgnoreCase(o.sourceId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.propertyType == null ) {
			return 1;
		} else if ( propertyType == null ) {
			return -1;
		}
		comparison = propertyType.compareTo(o.propertyType);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.propertyName == null ) {
			return 1;
		} else if ( propertyName == null ) {
			return -1;
		}
		return propertyName.compareToIgnoreCase(o.propertyName);
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(campaignId, nodeId, propertyName, propertyType, sourceId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof GlobalMetricCampaignNodePropertyPK) ) {
			return false;
		}
		GlobalMetricCampaignNodePropertyPK other = (GlobalMetricCampaignNodePropertyPK) obj;
		return Objects.equals(campaignId, other.campaignId) && Objects.equals(nodeId, other.nodeId)
				&& Objects.equals(propertyName, other.propertyName) && propertyType == other.propertyType
				&& Objects.equals(sourceId, other.sourceId);
	}

	@Override
	public String toString() {
		return "GlobalMetricCampaignNodePropertyPK{campaignId=" + campaignId + ",nodeId=" + nodeId
				+ ",sourceId=" + sourceId + ",propertyType=" + propertyType + ",propertyName="
				+ propertyName + "}";
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public GeneralDatumSamplesType getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(GeneralDatumSamplesType propertyType) {
		this.propertyType = propertyType;
	}

	public Character getPropertyTypeKey() {
		return (propertyType != null ? propertyType.toKey() : null);
	}

	public void setPropertyTypeKey(Character propertyTypeKey) {
		if ( propertyTypeKey == null ) {
			return;
		}
		try {
			this.propertyType = GeneralDatumSamplesType.valueOf(propertyTypeKey);
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

}
