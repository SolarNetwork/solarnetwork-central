/* ==================================================================
 * DelegatingDatumMaintenanceBiz.java - 10/04/2019 11:24:21 am
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

import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;

/**
 * Implementation of {@link DatumMaintenanceBiz} that delegates to another
 * {@link DatumMaintenanceBiz}, designed primarily for use with AOP.
 * 
 * @author matt
 * @version 1.0
 * @since 1.38
 */
public class DelegatingDatumMaintenanceBiz implements DatumMaintenanceBiz {

	private final DatumMaintenanceBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingDatumMaintenanceBiz(DatumMaintenanceBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void markDatumAggregatesStale(GeneralNodeDatumFilter criteria) {
		delegate.markDatumAggregatesStale(criteria);
	}

}
