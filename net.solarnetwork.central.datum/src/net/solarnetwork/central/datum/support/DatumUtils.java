/* ==================================================================
 * DatumUtils.java - Feb 13, 2012 2:52:39 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.util.ClassUtils;

/**
 * Utilities for Datum domain classes.
 * 
 * @author matt
 * @version $Revision$
 */
public final class DatumUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DatumUtils.class);
	
	// can't construct me
	private DatumUtils() {
		super();
	}

	/**
	 * Get a {@link NodeDatum} {@link Class} for a given name.
	 * 
	 * <p>If {@code name} contains a period, it will be treated as a fully-qualified
	 * class name. Otherwise a FQCN will be constructed as residing in the same
	 * package as {@link NodeDatum} named by capitalizing {@code name} and appending
	 * {@code Datum} to the end. For example, a {@code name} value of <em>power</em>
	 * would result in a class named {@code PowerDatum} in the same package as
	 * {@link NodeDatum} (e.g. {@code net.solarnetwork.central.datum.domain.PowerDatum}).
	 * 
	 * @param name the node datum class name
	 * @return the class, or <em>null</em> if not available
	 */
	public static Class<? extends NodeDatum> nodeDatumClassForName(String name) {
		if ( name == null ) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		if ( name.indexOf('.') < 0 ) {
			buf.append(NodeDatum.class.getPackage().getName());
			buf.append('.');
			
			// fix case and append "Datum"
			name = name.toLowerCase();
			buf.append(name.substring(0, 1).toUpperCase());
			if ( name.length() > 1 ) {
				buf.append(name.substring(1));
			}
			buf.append("Datum");
		} else {
			// contains a period, so treat as FQCN
			buf.append(name);
		}
		Class<? extends NodeDatum> result = null;
		try {
			result = ClassUtils.loadClass(name, NodeDatum.class);
		} catch ( RuntimeException e ) {
			LOG.debug("Exception loading NodeDatum class {}", name, e);
		}
		return result;
	}
	
}
