/* ==================================================================
 * RandomUuidFunction.java - 5/04/2024 4:59:34 pm
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

package net.solarnetwork.central.support.xslt;

import java.util.UUID;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

/**
 * Generate a UUIDv4 (random) string.
 * 
 * @author matt
 * @version 1.0
 */
public class RandomUuidFunction extends ExtensionFunctionDefinition implements SolarNetworkXslt {

	/** A default instance. */
	public static final RandomUuidFunction INSTANCE = new RandomUuidFunction();

	/**
	 * Constructor.
	 */
	public RandomUuidFunction() {
		super();
	}

	@Override
	public StructuredQName getFunctionQName() {
		return new StructuredQName(SN_XSLT_NAMESPACE_PREFIX, SN_XSLT_NAMESPACE_URI, "random-uuid");
	}

	@Override
	public SequenceType[] getArgumentTypes() {
		return new SequenceType[0];
	}

	@Override
	public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
		return SequenceType.one(BuiltInAtomicType.STRING);
	}

	@Override
	public ExtensionFunctionCall makeCallExpression() {
		return new ExtensionFunctionCall() {

			@Override
			public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
				String result = UUID.randomUUID().toString();
				return new StringValue(result);
			}
		};
	}

}
