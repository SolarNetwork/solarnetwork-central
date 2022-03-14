/* ==================================================================
 * DelegatingDatumAuxiliaryBiz.java - 4/02/2019 9:09:14 am
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

package net.solarnetwork.central.datum.support;

import java.util.List;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;

/**
 * Implementation of {@link DatumAuxiliaryBiz} that delegates to another
 * {@link DatumAuxiliaryBiz}, designed primarily for use with AOP.
 * 
 * @author matt
 * @version 1.1
 * @since 1.35
 */
public class DelegatingDatumAuxiliaryBiz implements DatumAuxiliaryBiz {

	private final DatumAuxiliaryBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingDatumAuxiliaryBiz(DatumAuxiliaryBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public GeneralNodeDatumAuxiliary getGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
		return delegate.getGeneralNodeDatumAuxiliary(id);
	}

	@Override
	public void storeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliary datum) {
		delegate.storeGeneralNodeDatumAuxiliary(datum);
	}

	@Override
	public boolean moveGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK from,
			GeneralNodeDatumAuxiliary to) {
		return delegate.moveGeneralNodeDatumAuxiliary(from, to);
	}

	@Override
	public void removeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
		delegate.removeGeneralNodeDatumAuxiliary(id);
	}

	@Override
	public FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> findGeneralNodeDatumAuxiliary(
			GeneralNodeDatumAuxiliaryFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return delegate.findGeneralNodeDatumAuxiliary(criteria, sortDescriptors, offset, max);
	}

}
