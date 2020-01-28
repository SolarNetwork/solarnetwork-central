
package net.solarnetwork.central.query.web.domain;

import org.joda.time.LocalDate;

/**
 * ReportableIntervalCommand object.
 * 
 * @author matt
 * @version 2.0
 */
public final class ReportableIntervalCommand {

	private Long nodeId;
	private LocalDate start;
	private LocalDate end;

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public LocalDate getStart() {
		return start;
	}

	public void setStart(LocalDate start) {
		this.start = start;
	}

	public LocalDate getEnd() {
		return end;
	}

	public void setEnd(LocalDate end) {
		this.end = end;
	}

}
