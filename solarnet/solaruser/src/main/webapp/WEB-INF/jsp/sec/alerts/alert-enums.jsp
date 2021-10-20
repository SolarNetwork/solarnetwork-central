<%--
	Input parameters:
	
		nodeDataAlertTypes 	- collection of UserAlertType that represent node data alerts
		alertStatuses       - collection of UesrAlertStatus
 --%>
<script type="text/javascript">
SolarReg.userAlertTypes = {
<c:forEach items="${nodeDataAlertTypes}" var="alertType" varStatus="itr">
	${alertType} : "<fmt:message key='alert.type.${alertType}.label'/>"${itr.last ? '' : ', '}
</c:forEach>
};
SolarReg.userAlertStatuses = {
<c:forEach items="${alertStatuses}" var="alertStatus" varStatus="itr">
	${alertStatus} : "<fmt:message key='alert.status.${alertStatus}.label'/>"${itr.last ? '' : ', '}
</c:forEach>
};
</script>
