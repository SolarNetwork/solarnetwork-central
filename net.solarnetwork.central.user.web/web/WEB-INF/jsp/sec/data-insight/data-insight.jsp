<a id="top"></a>

<div class="row">
	<div class="col-md-9 col-md-offset-1">
		<p class="intro">
			<fmt:message key='dataInsight.intro'/>
		</p>

		<section id="data-insight-overview">
			<h2><fmt:message key='dataInsight.overall.nodes.header'/></h2>
			<p><fmt:message key='dataInsight.overall.nodes.intro'/></p>
			<div class="row">
				<div class="col-md-7">
					<table class="datum-counts tally table">
						<tbody>
							<tr>
								<th><fmt:message key='dataInsight.nodeCount.label'/></th><td data-tprop="nodeCountDisplay"></td>
							</tr>
							<tr>
								<th><fmt:message key='dataInsight.sourceCount.label'/></th><td data-tprop="sourceCountDisplay"></td>
							</tr>
							<tr>
								<th><fmt:message key='dataInsight.recentNodeCount.label'/></th><td data-tprop="activeNodeCountDisplay"></td>
							</tr>
							<tr>
								<th><fmt:message key='dataInsight.recentSourceCount.label'/></th><td data-tprop="activeSourceCountDisplay"></td>
							</tr>
						</tbody>
					</table>
				</div>
			</div>
		</section>
			
		<section id="data-insight-overview-datum">
			<h2><fmt:message key='dataInsight.overall.datum.header'/></h2>
			<p><fmt:message key='dataInsight.overall.datum.intro'/></p>
			<div class="row">
				<div class="col-md-6">
					<table class="datum-counts tally table">
						<tbody>
							<tr>
								<th><fmt:message key='dataInsight.datumMonthlyCount.label'/></th><td data-tprop="accumulativeTotalDatumMonthlyCountDisplay"></td>
							</tr>
							<tr>
								<th><fmt:message key='dataInsight.datumDailyCount.label'/></th><td data-tprop="accumulativeTotalDatumDailyCountDisplay"></td>
							</tr>
							<tr>
								<th><fmt:message key='dataInsight.datumHourlyCount.label'/></th><td data-tprop="accumulativeTotalDatumHourlyCountDisplay"></td>
							</tr>
							<tr>
								<th><fmt:message key='dataInsight.datumCount.label'/></th><td data-tprop="accumulativeTotalDatumCountDisplay"></td>
							</tr>
						</tbody>
						<tfoot>
							<tr>
								<th><fmt:message key='dataInsight.datumTotalCount.label'/></th><th data-tprop="accumulativeTotalDatumTotalCountDisplay"></th>
							</tr>
						</tfoot>
					</table>
				</div>
			</div>
		</section>

		<section id="data-insight-recent">
			<h2><fmt:message key='dataInsight.counts.header'/></h2>
			<p><fmt:message key='dataInsight.counts.intro'/></p>
			<table class="datum-counts tally table">
				<thead>
					<tr>
						<th><fmt:message key="dataInsight.counts.date.label"/></th>
						<th><fmt:message key="dataInsight.counts.node.label"/></th>
						<th><fmt:message key="dataInsight.counts.source.label"/></th>
						<th style="text-align: right;"><fmt:message key="dataInsight.counts.datumQueryCount.label"/></th>
						<th><fmt:message key="dataInsight.counts.propertyPostedCount.label"/></th>
					</tr>
					<tr class="template">
						<td data-tprop="dateDisplay"></td>
						<td data-tprop="nodeId"></td>
						<td data-tprop="sourceId"></td>
						<td style="text-align: right;" data-tprop="datumQueryCountDisplay"></td>
						<td data-tprop="datumPropertyPostedCountDisplay"></td>
					</tr>
				</thead>
				<tbody class="list-container">
				</tbody>
			</table>
		
		</section>
	</div>
</div>
