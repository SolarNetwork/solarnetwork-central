/* ==================================================================
 * RemoteEventExecutor.java - Jun 30, 2011 1:03:01 PM
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

package net.solarnetwork.central.dras.ddrs;

import net.solarnetwork.central.dras.biz.EventExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FIXME
 * 
 * <p>TODO</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt></dt>
 *   <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class RemoteEventExecutor implements EventExecutor {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Override
	public EventExecutionReceipt executeEvent(EventExecutionRequest request) {
		log.info("EventExecutionRequest for event {}", request.getEvent().getId());
		return null;
	}

}
