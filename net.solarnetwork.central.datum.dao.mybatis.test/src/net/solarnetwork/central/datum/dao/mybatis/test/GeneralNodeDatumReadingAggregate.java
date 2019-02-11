/* ==================================================================
 * GeneralNodeDatumReadingAggregate.java - 11/02/2019 4:31:58 pm
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import java.util.Map;
import org.joda.time.DateTime;

/**
 * Reading aggregate for testing.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeDatumReadingAggregate {

	private DateTime date;
	private Long nodeId;
	private String sourceId;
	private Map<String, Number> a;
	private Map<String, Number> as;
	private Map<String, Number> af;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GeneralNodeDatumReadingAggregate{date=");
		builder.append(date);
		builder.append(",nodeId=");
		builder.append(nodeId);
		builder.append(",sourceId=");
		builder.append(sourceId);
		builder.append(",a=");
		builder.append(a);
		builder.append(",as=");
		builder.append(as);
		builder.append(",af=");
		builder.append(af);
		builder.append("}");
		return builder.toString();
	}

	public DateTime getDate() {
		return date;
	}

	public void setDate(DateTime date) {
		this.date = date;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public Map<String, Number> getA() {
		return a;
	}

	public void setA(Map<String, Number> a) {
		this.a = a;
	}

	public Map<String, Number> getAs() {
		return as;
	}

	public void setAs(Map<String, Number> as) {
		this.as = as;
	}

	public Map<String, Number> getAf() {
		return af;
	}

	public void setAf(Map<String, Number> af) {
		this.af = af;
	}

}
