/* ==================================================================
 * BasicDataConfiguration.java - 21/03/2018 11:39:38 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.domain.BasicIdentifiableConfiguration;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Basic implementation of {@link DataConfiguration}.
 *
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class BasicDataConfiguration extends BasicIdentifiableConfiguration
		implements DataConfiguration, Serializable {

	@Serial
	private static final long serialVersionUID = -4639476112147998835L;

	private AggregateGeneralNodeDatumFilter datumFilter;

	/**
	 * Default constructor.
	 */
	public BasicDataConfiguration() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the configuration to copy
	 */
	public BasicDataConfiguration(DataConfiguration other) {
		super(other);
		if ( other == null ) {
			return;
		}
		setDatumFilter(other.getDatumFilter());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicDataConfiguration{");
		if ( getName() != null ) {
			builder.append("name=");
			builder.append(getName());
			builder.append(", ");
		}
		if ( getServiceIdentifier() != null ) {
			builder.append("serviceIdentifier=");
			builder.append(getServiceIdentifier());
			builder.append(", ");
		}
		if ( datumFilter != null ) {
			builder.append("datumFilter=");
			builder.append(datumFilter);
			builder.append(", ");
		}
		Map<String, Object> props = getServiceProps();
		if ( props != null ) {
			builder.append("serviceProps=");
			Map<String, Object> maskedServiceProps = StringUtils.sha256MaskedMap(props,
					CollectionUtils.sensitiveNamesToMask(props.keySet()));
			builder.append(maskedServiceProps);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public AggregateGeneralNodeDatumFilter getDatumFilter() {
		return datumFilter;
	}

	@JsonDeserialize(as = DatumFilterCommand.class)
	public void setDatumFilter(AggregateGeneralNodeDatumFilter datumFilter) {
		this.datumFilter = datumFilter;
	}

}
