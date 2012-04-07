/* ==================================================================
 * CriteriaSupport.java - Jun 10, 2011 8:21:57 PM
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

package net.solarnetwork.central.dras.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;

import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dao.ObjectCriteria.JoinType;
import net.solarnetwork.central.dao.ObjectCriteria.MatchType;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.support.ObjectSearchFilters;
import net.solarnetwork.central.support.SimpleSortDescriptor;

/**
 * Base criteria object with additional search support.
 * 
 * <p>The use of {@link LazyList} internally has been done with 
 * with using this as a web command object in mind.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class CriteriaSupport<T extends Filter> extends ObjectSearchFilters<T> {

	@SuppressWarnings("unchecked")
	private List<SortDescriptor> sortDescriptors = 
		LazyList.decorate(new ArrayList<SortDescriptor>(), 
		FactoryUtils.instantiateFactory(SimpleSortDescriptor.class));
	
	/**
	 * Construct a criteria object out of a single filter, using 
	 * {@link JoinType#AND} and {@link MatchType#EQUAL}.
	 * 
	 * @param filter the object to filter on
	 */
	public CriteriaSupport(T filter) {
		super(filter);
	}

	public List<SortDescriptor> getSortDescriptors() {
		return sortDescriptors;
	}
	public void setSortDescriptors(List<SortDescriptor> sortDescriptors) {
		this.sortDescriptors = sortDescriptors;
	}

}
