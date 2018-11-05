/* ==================================================================
 * Identity.java - Aug 8, 2010 7:42:21 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Revision$
 * ==================================================================
 */

package net.solarnetwork.central.domain;

/**
 * Common API for identity information in SolarNetwork participating services.
 * 
 * @author matt
 * @version 1.2
 * @param <PK>
 *        the primary data type that uniquely identifies the object
 * @deprecated in {@code 1.38}; use {@link net.solarnetwork.domain.Identity}
 *             instead
 */
@Deprecated
public interface Identity<PK> extends Comparable<PK>, net.solarnetwork.domain.Identity<PK> {

}
