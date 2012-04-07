/* ==================================================================
 * EffectiveObject.java - Jun 2, 2011 7:16:46 PM
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

/**
 * An object versioned by a specific {@link Effective} entity.
 * 
 * @param <T> the versioned object
 * @author matt
 * @version $Revision$
 */
public class EffectiveObject<T extends Serializable> implements Serializable {

	private static final long serialVersionUID = 7907404355481141009L;

	private Effective effective;
	private T object;
	
	public EffectiveObject(Effective effective, T object) {
		this.effective = effective;
		this.object = object;
	}

	public Effective getEffective() {
		return effective;
	}

	public void setEffective(Effective effective) {
		this.effective = effective;
	}

	public T getObject() {
		return object;
	}

	public void setObject(T object) {
		this.object = object;
	}
	
}
