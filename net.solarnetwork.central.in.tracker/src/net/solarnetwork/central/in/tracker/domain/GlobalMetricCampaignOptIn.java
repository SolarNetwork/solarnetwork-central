/* ==================================================================
 * GlobalMetricCampaignOptIn.java - 1/11/2018 10:10:07 AM
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
import org.joda.time.DateTime;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Configuration for a single metric in a global metric campaign.
 * 
 * @author matt
 * @version 1.0
 */
public class GlobalMetricCampaignOptIn
		implements Entity<GlobalMetricCampaignNodePropertyPK>, Cloneable, Serializable {

	private static final long serialVersionUID = 1260890211611683916L;

	private GlobalMetricCampaignNodePropertyPK id = new GlobalMetricCampaignNodePropertyPK();
	private DateTime created;

	/**
	 * Default constructor.
	 */
	public GlobalMetricCampaignOptIn() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	public GlobalMetricCampaignOptIn(GlobalMetricCampaignNodePropertyPK id, DateTime created) {
		super();
		this.id = id;
		this.created = created;
	}

	@Override
	public int compareTo(GlobalMetricCampaignNodePropertyPK o) {
		if ( id == null ) {
			return -1;
		} else if ( o == null ) {
			return 1;
		}
		return id.compareTo(o);
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public GlobalMetricCampaignNodePropertyPK getId() {
		return id;
	}

	public void setId(GlobalMetricCampaignNodePropertyPK id) {
		this.id = id;
	}

	@Override
	public DateTime getCreated() {
		return created;
	}

	public void setCreated(DateTime created) {
		this.created = created;
	}

	/**
	 * Convenience getter for
	 * {@link GlobalMetricCampaignNodePropertyPK#getCampaignId()}.
	 * 
	 * @return the campaign ID
	 */
	public String getCampaignId() {
		return (id == null ? null : id.getCampaignId());
	}

	/**
	 * Convenience setter for
	 * {@link GlobalMetricCampaignNodePropertyPK#setCampaignId(String)}.
	 * 
	 * @param campaignId
	 *        the campaign ID to set
	 */
	public void setCampaignId(String campaignId) {
		if ( id == null ) {
			id = new GlobalMetricCampaignNodePropertyPK();
		}
		id.setCampaignId(campaignId);
	}

	/**
	 * Convenience getter for
	 * {@link GlobalMetricCampaignNodePropertyPK#getNodeId()}.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return (id == null ? null : id.getNodeId());
	}

	/**
	 * Convenience setter for
	 * {@link GlobalMetricCampaignNodePropertyPK#setNodeId(Long)}.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		if ( id == null ) {
			id = new GlobalMetricCampaignNodePropertyPK();
		}
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for
	 * {@link GlobalMetricCampaignNodePropertyPK#getSourceId()}.
	 * 
	 * @return the sourceId
	 */
	public String getSourceId() {
		return (id == null ? null : id.getSourceId());
	}

	/**
	 * Convenience setter for
	 * {@link GlobalMetricCampaignNodePropertyPK#setSourceId(String)}.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		if ( id == null ) {
			id = new GlobalMetricCampaignNodePropertyPK();
		}
		id.setSourceId(sourceId);
	}

	/**
	 * Convenience getter for
	 * {@link GlobalMetricCampaignNodePropertyPK#getPropertyType()}.
	 * 
	 * @return the property type
	 */
	public GeneralDatumSamplesType getPropertyType() {
		return (id == null ? null : id.getPropertyType());
	}

	/**
	 * Convenience setter for
	 * {@link GlobalMetricCampaignNodePropertyPK#setPropertyType(GeneralDatumSamplesType)}.
	 * 
	 * @param propertyType
	 *        the property type to set
	 */
	public void setPropertyType(GeneralDatumSamplesType propertyType) {
		if ( id == null ) {
			id = new GlobalMetricCampaignNodePropertyPK();
		}
		id.setPropertyType(propertyType);
	}

	/**
	 * Convenience getter for {@link GeneralDatumSamplesType#toKey()}.
	 * 
	 * @return the property type key
	 */
	public char getPropertyTypeKey() {
		GeneralDatumSamplesType type = (id == null ? GeneralDatumSamplesType.Instantaneous
				: id.getPropertyType());
		return type.toKey();
	}

	/**
	 * Convenience setter for
	 * {@link GlobalMetricCampaignNodePropertyPK#setPropertyType(GeneralDatumSamplesType)}
	 * using a key value.
	 * 
	 * @param propertyTypeKey
	 *        the property type key to set
	 */
	public void setPropertyTypeKey(char propertyTypeKey) {
		GeneralDatumSamplesType propertyType = GeneralDatumSamplesType.Instantaneous;
		try {
			propertyType = GeneralDatumSamplesType.valueOf(propertyTypeKey);
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		if ( id == null ) {
			id = new GlobalMetricCampaignNodePropertyPK();
		}
		id.setPropertyType(propertyType);
	}

	/**
	 * Convenience getter for
	 * {@link GlobalMetricCampaignNodePropertyPK#getPropertyName()}.
	 * 
	 * @return the property name
	 */
	public String getPropertyName() {
		return (id == null ? null : id.getPropertyName());
	}

	/**
	 * Convenience setter for
	 * {@link GlobalMetricCampaignNodePropertyPK#setPropertyName(String)}.
	 * 
	 * @param propertyName
	 *        the property name to set
	 */
	public void setPropertyName(String propertyName) {
		if ( id == null ) {
			id = new GlobalMetricCampaignNodePropertyPK();
		}
		id.setPropertyName(propertyName);
	}

}
