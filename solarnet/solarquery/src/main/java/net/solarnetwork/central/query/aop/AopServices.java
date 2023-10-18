/* ==================================================================
 * AopServices.java - 18/10/2023 10:13:03 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.aop;

/**
 * Marker interface for AOP service package.
 * 
 * @author matt
 * @version 1.0
 */
public interface AopServices {

	/** A profile name for excluding AOP security services. */
	String WITHOUT_AOP_SECURITY = "without-aop-security";

	/** A profile name for including AOP security services. */
	String WITH_AOP_SECURITY = "!" + WITHOUT_AOP_SECURITY;

}
