/* ==================================================================
 * GeneralLocationDatumMetadataDao.java - Oct 17, 2014 3:01:44 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao;

import java.util.Set;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;

/**
 * DAO API for {@link GeneralLocationDatumMetadata}.
 * 
 * @author matt
 * @version 1.1
 */
public interface GeneralLocationDatumMetadataDao
		extends GenericDao<GeneralLocationDatumMetadata, LocationSourcePK>,
		FilterableDao<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK, GeneralLocationDatumMetadataFilter> {

	/**
	 * Get all available location + source ID combinations for a given set of
	 * location IDs matching a metadata search filter.
	 * 
	 * The metadata filter must be expressed in LDAP search filter style, using
	 * JSON pointer style paths for keys, for example {@code (/m/foo=bar)},
	 * {@code (t=foo)}, or {@code (&(&#47;**&#47;foo=bar)(t=special))}.
	 * 
	 * @param locationIds
	 *        the location IDs to search for
	 * @param metadataFilter
	 *        A metadata search filter, in LDAP search filter syntax.
	 * @return the distinct location ID and source IDs combinations that match
	 *         the given filter (never <em>null</em>)
	 * @since 1.1
	 */
	Set<LocationSourcePK> getFilteredSources(Long[] locationIds, String metadataFilter);

}
