/* ==================================================================
 * EntityComparator.java - 25/03/2020 6:49:14 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.biz.dao;

import java.time.Instant;
import java.util.Comparator;
import net.solarnetwork.central.datum.domain.BasePK;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;

/**
 * Compare entities by date, key, source.
 * 
 * @author matt
 * @version 2.0
 * @since 2.1
 */
public final class EntityComparator implements Comparator<BasePK> {

	@Override
	public int compare(BasePK o1, BasePK o2) {
		Instant ts1;
		Instant ts2;
		Long key1;
		Long key2;
		String s1;
		String s2;
		if ( o1 instanceof GeneralNodeDatumPK ) {
			GeneralNodeDatumPK pk1 = (GeneralNodeDatumPK) o1;
			ts1 = pk1.getCreated();
			key1 = pk1.getNodeId();
			s1 = pk1.getSourceId();
		} else {
			GeneralLocationDatumPK pk1 = (GeneralLocationDatumPK) o1;
			ts1 = pk1.getCreated();
			key1 = pk1.getLocationId();
			s1 = pk1.getSourceId();
		}
		if ( o2 instanceof GeneralNodeDatumPK ) {
			GeneralNodeDatumPK pk2 = (GeneralNodeDatumPK) o2;
			ts2 = pk2.getCreated();
			key2 = pk2.getNodeId();
			s2 = pk2.getSourceId();
		} else {
			GeneralLocationDatumPK pk2 = (GeneralLocationDatumPK) o2;
			ts2 = pk2.getCreated();
			key2 = pk2.getLocationId();
			s2 = pk2.getSourceId();
		}
		int result = ts1.compareTo(ts2);
		if ( result != 0 ) {
			return result;
		}
		result = key1.compareTo(key2);
		if ( result != 0 ) {
			return result;
		}
		return s1.compareTo(s2);
	}

}
