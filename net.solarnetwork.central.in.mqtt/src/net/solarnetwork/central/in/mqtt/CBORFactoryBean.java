/* ==================================================================
 * CBORFactoryBean.java - 17/12/2019 11:33:12 am
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

package net.solarnetwork.central.in.mqtt;

import org.springframework.beans.factory.FactoryBean;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

/**
 * Factory bean for {@link CBORFactory}.
 * 
 * @author matt
 * @version 1.0
 */
public class CBORFactoryBean implements FactoryBean<CBORFactory> {

	private final CBORFactory factory;

	/**
	 * Constructor.
	 * 
	 * @param backwardsCompatibility
	 *        {@literal true} if CBOR &lt; v2.10 compatibility mode should be
	 *        enabled
	 */
	public CBORFactoryBean(boolean backwardsCompatibility) {
		super();
		this.factory = createFactory(backwardsCompatibility);
	}

	private CBORFactory createFactory(boolean backwardsCompatibility) {
		CBORFactory fac = new CBORFactory();
		// try to support normal release JAR, which does not have our special feature
		CBORParser.Feature[] features = CBORParser.Feature.values();
		if ( features != null && features.length > 0 ) {
			for ( CBORParser.Feature f : features ) {
				if ( "CBOR_BIG_DECIMAL_EXPONENT_NEGATE".equals(f.name()) ) {
					if ( backwardsCompatibility ) {
						fac.disable(f);
					} else {
						fac.enable(f);
					}
				}
			}
		}
		return fac;
	}

	@Override
	public CBORFactory getObject() throws Exception {
		return factory;
	}

	@Override
	public Class<?> getObjectType() {
		return CBORFactory.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
