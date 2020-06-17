/* ==================================================================
 * StaleGeneralNodeDatumEventProducer.java - 15/06/2020 5:41:22 pm
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

package net.solarnetwork.central.datum.agg.event;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.solarnetwork.central.datum.biz.DatumAppEventProducer;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;

/**
 * {@link DatumAppEventProducer} for
 * {@link AggregateUpdatedEventInfo#AGGREGATE_UPDATED_TOPIC} events.
 * 
 * @author matt
 * @version 1.0
 */
public class StaleGeneralNodeDatumEventProducer extends
		BaseSettingsSpecifierLocalizedServiceInfoProvider<String> implements DatumAppEventProducer {

	private static final Set<String> PRODUCED_TOPICS = Collections
			.singleton(AggregateUpdatedEventInfo.AGGREGATE_UPDATED_TOPIC);

	/**
	 * Constructor.
	 */
	public StaleGeneralNodeDatumEventProducer() {
		super("net.solarnetwork.central.datum.agg.StaleGeneralNodeDatumEventProducer");
	}

	@Override
	public String getDisplayName() {
		return "Datum Aggregate Updates";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Collections.emptyList();
	}

	@Override
	public Set<String> getProducedDatumAppEventTopics() {
		return PRODUCED_TOPICS;
	}

}
