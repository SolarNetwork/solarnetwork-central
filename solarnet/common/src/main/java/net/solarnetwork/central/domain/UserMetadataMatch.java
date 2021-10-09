/* ==================================================================
 * UserMetadataMatch.java - 11/11/2016 5:40:31 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
 * A "match" to a {@link UserMetadataEntity}.
 * 
 * Although this class extends {@link UserMetadataEntity} that is merely an
 * implementation detail.
 * 
 * @author matt
 * @version 2.0
 * @since 1.23
 */
public class UserMetadataMatch extends UserMetadataEntity implements UserMetadataFilterMatch {

	private static final long serialVersionUID = -5455447804909429786L;

}
