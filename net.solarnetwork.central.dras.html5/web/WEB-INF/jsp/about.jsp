<h1><fmt:message key="about.title"/></h1>


<table id="about-table" class="display left">
	<thead>
		<tr>
			<td colspan="2">
				<h2><fmt:message key="about.ui.title"/></h2>
			</td>
		</tr>
	</thead>
	<tbody>
		<tr>
			<th>App</th><td>@APP_NAME@</td>
		</tr>
		<tr>
			<th>Build</th><td>@BUILD_VERSION@ </td>
		</tr>
		<tr>
			<th>Date</th><td>@BUILD_DATE@</td>
		</tr>
		<tr>
			<th>Host</th><td><%= java.net.InetAddress.getLocalHost().getHostName() %></td>
		</tr>
	</tbody>
	<thead>
		<tr>
			<td colspan="2">
				<h2><fmt:message key="about.system.title"/></h2>
			</td>
		</tr>
	</thead>
	<tbody>
	</tbody>
</table>

<script>
$(document).ready(function() {
	var about = new SolarNetwork.DRAS.About({
			elementId : 'about-table',
			context : SolarNetwork.DRAS.Config.contextAbout
	});
	about.update();
});
</script>