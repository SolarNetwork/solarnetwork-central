/* ==================================================================
 * EffectiveCollection.java - Jun 2, 2011 9:00:25 PM
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.util.SerializeIgnore;

/**
 * A collection of objects relating to some other object, all versioned 
 * by a specific {@link Effective} entity.
 * 
 * <p>This is a container for collections relating to an entity, for example
 * members of a group. In that example, {@link #getObject()} returns
 * the group entity and {@link #getCollection()} returns the members of
 * that group.</p>
 * 
 * <p>Note the {@link Collection} instance used <em>must</em> implement
 * {@link Serializable}.</p>
 * 
 * @param <E> the collection element type
 * @param <T> the versioned object type
 * @author matt
 * @version $Revision$
 */
public class EffectiveCollection<T extends Entity<?>, E extends Member> {

	private Effective effective;
	private T object;
	private Collection<E> collection;

	public EffectiveCollection(Effective effective, T object, 
			Collection<E> collection) {
		this.effective = effective;
		this.object = object;
		this.collection = collection;
	}

	/**
	 * Get a mapping of member types to a collection of those members.
	 * 
	 * <p>This is useful when {@link #getCollection()} is a heterogeneous 
	 * Collection. The keys in the returned maps are the simple class names
	 * of the associated member objects.</p>
	 * 
	 * @return Map
	 */
	public Map<String, Collection<Long>> getMemberMap() {
		if ( collection == null ) {
			return null;
		}
		Map<String, Collection<Long>> result 
			= new LinkedHashMap<String, Collection<Long>>();
		for ( E member : collection ) {
			final String key = member.getClass().getSimpleName();
			if ( !result.containsKey(key) ) {
				result.put(key, new ArrayList<Long>(collection.size()));
			}
			result.get(key).add(member.getId());
		}
		return result;
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
	
	@SerializeIgnore
	public Collection<E> getCollection() {
		return collection;
	}
	public void setCollection(Collection<E> collection) {
		this.collection = collection;
	}

}
