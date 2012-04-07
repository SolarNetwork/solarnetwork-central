/* ==================================================================
 * IbatisNodeInstructionDao.java - Sep 29, 2011 8:15:39 PM
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

package net.solarnetwork.central.instructor.dao.ibatis;

import net.solarnetwork.central.dao.ibatis.IbatisFilterableDaoSupport;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * Ibatis implementation of {@link NodeInstructionDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisNodeInstructionDao
extends IbatisFilterableDaoSupport<NodeInstruction, EntityMatch, InstructionFilter>
implements NodeInstructionDao {

	/**
	 * Default constructor.
	 */
	public IbatisNodeInstructionDao() {
		super(NodeInstruction.class, EntityMatch.class);
	}
	
	@Override
	protected Long handleInsert(NodeInstruction datum) {
		Long result = super.handleInsert(datum);
		handleRelation(result, datum.getParameters(), InstructionParameter.class, null);
		return result;
	}
}
