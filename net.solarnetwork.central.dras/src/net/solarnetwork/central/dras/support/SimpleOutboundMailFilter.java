/* ==================================================================
 * SimpleOutboundMailFilter.java - Jun 18, 2011 8:59:59 PM
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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import net.solarnetwork.central.dras.dao.OutboundMailFilter;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Simple implementation of {@link OutboundMailFilter}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleOutboundMailFilter implements OutboundMailFilter, Serializable {

	private static final long serialVersionUID = -3286055620881269771L;

	private String query;
	
	@Override
	public String getQuery() {
		return query;
	}

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> f = new LinkedHashMap<String, Object>();
		if ( query != null ) {
			f.put("query", query);
		}
		return f;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
