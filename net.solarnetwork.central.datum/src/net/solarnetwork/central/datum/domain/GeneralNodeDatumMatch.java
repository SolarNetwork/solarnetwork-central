/* ==================================================================
 * GeneralNodeDatumMatch.java - Aug 22, 2014 7:04:33 AM
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

package net.solarnetwork.central.datum.domain;


/**
 * A "match" to a {@link GeneralNodeDatum}.
 * 
 * <p>
 * Although this class extends {@link GeneralNodeDatum} that is merely an
 * implementation detail. Often instances of this class represent aggregated
 * data values and not actual datum entities.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumMatch extends GeneralNodeDatum implements GeneralNodeDatumFilterMatch {

	private static final long serialVersionUID = 6894637498151159318L;

}
