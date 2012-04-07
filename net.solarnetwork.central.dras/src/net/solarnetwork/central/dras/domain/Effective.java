/* ==================================================================
 * Effective.java - Jun 2, 2011 5:52:08 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;

import org.joda.time.DateTime;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * A time-stamp based effective data flag.
 * 
 * @author matt
 * @version $Revision$
 */
public class Effective extends BaseEntity implements Cloneable, Serializable {

	private static final long serialVersionUID = -6933711267396032883L;

	private Long creator;
	private DateTime effectiveDate;
	
	/**
	 * Default constructor.
	 */
	public Effective() {
		super();
	}
	
	/**
	 * Construct with an ID.
	 * @param id the ID
	 */
	public Effective(Long id) {
		super();
		setId(id);
	}
	
	public Long getCreator() {
		return creator;
	}
	public void setCreator(Long creator) {
		this.creator = creator;
	}
	public DateTime getEffectiveDate() {
		return effectiveDate;
	}
	public void setEffectiveDate(DateTime effectiveDate) {
		this.effectiveDate = effectiveDate;
	}
	
}
