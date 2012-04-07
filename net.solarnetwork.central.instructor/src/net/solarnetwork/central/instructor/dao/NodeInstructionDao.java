/* ==================================================================
 * NodeInstructionDao.java - Sep 29, 2011 6:47:11 PM
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

package net.solarnetwork.central.instructor.dao;

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * DAO API for {@link NodeInstruction}.
 * 
 * @author matt
 * @version $Revision$
 */
public interface NodeInstructionDao extends GenericDao<NodeInstruction, Long>,
FilterableDao<EntityMatch, Long, InstructionFilter> {

}
