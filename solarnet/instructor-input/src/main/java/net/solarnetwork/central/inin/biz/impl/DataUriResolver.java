/* ==================================================================
 * DataUriResolver.java - 5/03/2024 9:06:30 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.inin.biz.impl;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.resource.BinaryResource;
import net.sf.saxon.resource.DataURIScheme;
import net.sf.saxon.resource.UnparsedTextResource;
import net.sf.saxon.trans.XPathException;

/**
 * Implementation of {@link URIResolver} that supports {@code data:} style URIs.
 *
 * <p>
 * Any URI other than {@code data:} ones will result in an exception.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DataUriResolver implements URIResolver {

	/**
	 * Constructor.
	 */
	public DataUriResolver() {
		super();
	}

	@Override
	public Source resolve(String href, String base) throws TransformerException {
		URI uri;
		try {
			uri = new URI(href);
		} catch ( URISyntaxException e ) {
			throw new XPathException("Invalid URI: " + e.getMessage());
		}
		if ( "data".equals(uri.getScheme()) ) {
			Resource resource;
			try {
				resource = DataURIScheme.decode(uri);
			} catch ( IllegalArgumentException e ) {
				throw new XPathException("Invalid URI using 'data' scheme: " + e.getMessage());
			}
			Source source = null;
			if ( resource instanceof BinaryResource r ) {
				byte[] contents = r.getData();
				var is = new InputSource(new ByteArrayInputStream(contents));
				source = new SAXSource(is);
				source.setSystemId(uri.toString());
			} else if ( resource instanceof UnparsedTextResource r ) {
				var reader = new StringReader(r.getContent());
				source = new SAXSource(new InputSource(reader));
				source.setSystemId(uri.toString());
			}
			if ( source != null ) {
				return source;
			}
		}
		throw new UnsupportedOperationException(
				"External resources are not allowed (" + href + ") from (" + base + ")");
	}

}
