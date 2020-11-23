/* ==================================================================
 * MetadataKind.java - 6/11/2020 3:38:49 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

/**
 * The type of metadata to parse.
 * 
 * <p>
 * This type is used in situations where the actual metadata kind might be
 * determined at runtime.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public enum MetadataKind {
	/** Node metadata. */
	Node,

	/** Location metadata. */
	Location,

	/** Dynamically determined metadata. */
	Dynamic;
}
