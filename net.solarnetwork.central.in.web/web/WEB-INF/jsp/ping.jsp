<%@page contentType="text/html" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8" />
	<title>Ping Test</title>
	<style type="text/css">
		body {
			font-family: helvetica,sans-serif;
			font-size: 13px;
		}
		
		td, th {
			padding: 8px;
		}
		
		th {
			text-align: left;
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
		
			<table>
				<thead>
					<tr>
						<th>Test</th>
						<th>Status</th>
						<th>Message</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${results.results}" var="result">
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
							${result.value.message}
						</td>
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
