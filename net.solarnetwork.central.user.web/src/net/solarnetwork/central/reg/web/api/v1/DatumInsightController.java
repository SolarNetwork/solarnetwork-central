/* ==================================================================
 * DatumInsightController.java - 13/07/2018 11:07:43 AM
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.web.domain.Response.response;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.reg.web.domain.DatumInsightOverallStatistics;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.SimpleSortDescriptor;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.web.domain.Response;

/**
 * Web service API for datum insight.
 * 
 * @author matt
 * @version 1.0
 * @since 1.30
 */
@RestController("v1DatumInsightController")
@RequestMapping(value = { "/sec/data-insight", "/v1/sec/user/data-insight" })
public class DatumInsightController {

	private final AuditDatumBiz auditDatumBiz;
	private final UserBiz userBiz;

	/**
	 * Constructor.
	 * 
	 * @param auditDatumBiz
	 *        the audit datum biz to use
	 * @param userBiz
	 *        the user biz to ues
	 */
	@Autowired
	public DatumInsightController(AuditDatumBiz auditDatumBiz, UserBiz userBiz) {
		super();
		this.auditDatumBiz = auditDatumBiz;
		this.userBiz = userBiz;
	}

	@ResponseBody
	@RequestMapping(value = "/overall", method = RequestMethod.GET)
	public Response<DatumInsightOverallStatistics> overallStatistics() {
		Long userId = SecurityUtils.getCurrentActorUserId();
		User user = userBiz.getUser(userId);
		TimeZone userTimeZone = user.getTimeZone() != null ? user.getTimeZone()
				: TimeZone.getTimeZone("UTC");

		// get last 30 days of audit data
		DateTime today = new DateTime(DateTimeZone.forTimeZone(userTimeZone)).hourOfDay()
				.roundFloorCopy();
		DateTime tomorrow = today.plusDays(1);
		DateTime thirtyDaysAgo = tomorrow.minusDays(30);
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(userId);
		filter.setStartDate(thirtyDaysAgo);
		filter.setEndDate(tomorrow);

		List<SortDescriptor> sorts = Arrays.asList(
				(SortDescriptor) new SimpleSortDescriptor("created", true),
				(SortDescriptor) new SimpleSortDescriptor("node"),
				(SortDescriptor) new SimpleSortDescriptor("source"));

		FilterResults<AuditDatumRecordCounts> last30days = auditDatumBiz
				.findFilteredAuditRecordCounts(filter, sorts, null, null);

		// accumulative most recent
		filter.setMostRecent(true);
		filter.setStartDate(null);
		filter.setEndDate(null);
		FilterResults<AuditDatumRecordCounts> accumulative = auditDatumBiz
				.findFilteredAccumulativeAuditRecordCounts(filter, null, null, null);

		DatumInsightOverallStatistics result = new DatumInsightOverallStatistics(last30days,
				accumulative);

		return response(result);
	}
}
