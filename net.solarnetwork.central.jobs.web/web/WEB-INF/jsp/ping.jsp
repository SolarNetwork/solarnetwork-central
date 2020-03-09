<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8" />
	<title>SolarUser Ping Test</title>
	<style type="text/css">
		body {
			font-family: helvetica,sans-serif;
			font-size: 13px;
		}
		
		.caption {
			font-size: 11px;
			color: #ccc;
			margin: 4px 0;
			font-weight: normal;
		}
		
		.fail {
			color: #c00;
			background-color: inherit;
		}
				
		table {
		    border-collapse: collapse;
		    border-spacing: 0;
		}
		td, th {
		    padding: 0;
		}
		th {
		    text-align: left;
		}
		.table {
		    margin-bottom: 20px;
		    max-width: 100%;
		    width: 100%;
		}
		.table > thead > tr > th, .table > tbody > tr > th, .table > tfoot > tr > th, .table > thead > tr > td, .table > tbody > tr > td, .table > tfoot > tr > td {
		    border-top: 1px solid #ddd;
		    line-height: 1.42857;
		    padding: 8px;
		    vertical-align: top;
		}
		.table > thead > tr > th {
		    border-bottom: 2px solid #ddd;
		    vertical-align: bottom;
		}
		.table > caption + thead > tr:first-child > th, .table > colgroup + thead > tr:first-child > th, .table > thead:first-child > tr:first-child > th, .table > caption + thead > tr:first-child > td, .table > colgroup + thead > tr:first-child > td, .table > thead:first-child > tr:first-child > td {
		    border-top: 0 none;
		}
		.table > tbody + tbody {
		    border-top: 2px solid #ddd;
		}	
	</style>
</head>
<body>
	<c:choose>
		<c:when test="${results != null}">
			<p>
				<strong>Overall: </strong>
				<span${results.allGood ? '' : ' class="fail"'}>
					<c:choose>
						<c:when test="${results.allGood}">ALL_GOOD</c:when>
						<c:otherwise>One or more tests failed.</c:otherwise>
					</c:choose>
				</span>
			</p>
			<p>
				<strong>Date: </strong>
				<fmt:formatDate value="${results.date}" pattern="yyyy-MM-dd HH:mm:ss.SSS'Z'" timeZone="UTC"/>
			</p>
			<table class="table">
				<thead>
					<tr>
						<th>Test</th>
						<th>Status</th>
						<th>Execution time (ms)</th>
						<th>Message</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${results.results}" var="result">
						<tr>
							<th>
								${result.value.pingTestName}<br />
								<div class="caption">${result.key}</div>
							</th>
							<td${result.value.success ? '' : ' class="fail"'}>
								<c:choose>
									<c:when test="${result.value.success}">
										PASS
									</c:when>
									<c:otherwise>
										FAIL
									</c:otherwise>
								</c:choose>
							</td>
							<td>
								${result.value.end.time - result.value.start.time}
							</td>
							<td>
								${result.value.message}
							</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		</c:when>
		<c:otherwise>
			<p class="fail">FAIL: no ping results available</p>
		</c:otherwise>
	</c:choose>
</body>
</html>
