/* ==================================================================
 * CloudControlLocalizedServiceInfo.java - 3/11/2025 4:59:07 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.domain;

import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.settings.ConfigurableLocalizedServiceInfo;

/**
 * Localized service information for cloud control services.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudControlLocalizedServiceInfo extends ConfigurableLocalizedServiceInfo {

	/**
	 * Tell if the service requires a {@link CloudDatumStreamConfiguration} for
	 * the
	 * {@link CloudControlService#dataValues(net.solarnetwork.central.domain.UserLongCompositePK, java.util.Map)
	 * dataValues()} method to function.
	 *
	 * <p>
	 * A cloud datum stream service might require service properties like
	 * credentials that operate at the cloud datum stream level, rather than the
	 * cloud integration level. If that is the case this method will return
	 * {@code true} and the ID of the datum stream configuration to use can be
	 * provided on the {@link CloudControlService#DATUM_STREAM_ID_FILTER} filter
	 * key.
	 * </p>
	 *
	 * @return {@code true} if this service requires a datum stream for the
	 *         {@link CloudControlService#dataValues(net.solarnetwork.central.domain.UserLongCompositePK, java.util.Map)
	 *         dataValues()} method to function
	 */
	boolean isDataValuesRequireDatumStream();

}
