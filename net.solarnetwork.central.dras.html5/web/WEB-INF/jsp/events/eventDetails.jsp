<form id="eventForm">
<%--programId=-9&eventDate=2010-12-31T19:00&name=max1&eventPeriod=PT20M --%>
	<input type="hidden" name="programId" value="${param.programId}"/>
Event Name: <input type="text" name="name"/>
</form>

<script>
	$(function() {
		$( "#datepicker" ).datepicker();
	});
	</script>