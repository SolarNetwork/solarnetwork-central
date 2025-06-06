/* ==================================================================
 * MetadataFilter.java - 8/08/2019 10:21:48 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

/**
 * API for a metadata search filter.
 * 
 * @author matt
 * @version 1.0
 * @since 1.48
 */
public interface MetadataFilter extends Filter {

	/**
	 * Get a metadata search filter, in LDAP search filter syntax.
	 *
	 * <p>
	 * The metadata filter must be expressed in LDAP search filter style, using
	 * JSON pointer style paths for keys, for example {@code (/m/foo=bar)},
	 * {@code (t=foo)}, or <code>(&amp;(/&#42;&#42;/foo=bar)(t=special))</code>.
	 * </p>
	 * 
	 * @return the metadata filter to use (may be {@literal null})
	 */
	String getMetadataFilter();

}
