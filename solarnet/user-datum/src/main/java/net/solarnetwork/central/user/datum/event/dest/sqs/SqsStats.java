
package net.solarnetwork.central.user.datum.event.dest.sqs;

/** Basic counted fields. */
public enum SqsStats {

	NodeEventsReceived(0, "node events received"),

	NodeEventsPublished(1, "node events published"),

	NodeEventsPublishFailed(2, "node publish failures");

	private final int index;
	private final String description;

	SqsStats(int index, String description) {
		this.index = index;
		this.description = description;
	}

	public int getIndex() {
		return index;
	}

	public String getDescription() {
		return description;
	}

}
