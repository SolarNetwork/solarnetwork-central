<div>
<h1 class="title"><fmt:message key="users.title"/></h1><button id="newUserButton" type="button" class="titleButton"><fmt:message key="user.new"/></button>
<div class="clear"></div>
</div>

<div class="leftPanel">
	<div class="panel ui-corner-all">
		<table id="userTable" class="display">
			<thead><tr><th>Username</th><th>Name</th><th>Roles</th></tr></thead>
			<tbody></tbody>
		</table>
	</div>
</div>

<div class="rightPanel">
	<div class="panel ui-corner-all">
		<div id="newUserPanel">
			<jsp:include page="/WEB-INF/jsp/users/newUser.jsp"></jsp:include>
		</div>
		<div id="editUserPanel" style="display:none">
			<jsp:include page="/WEB-INF/jsp/users/editUser.jsp"></jsp:include>
		</div>
	</div>
	
</div>

<div class="clear"></div>

<script type="text/javascript">
var loaded = 0;
var handleLoaded = function() {
	loaded++;
	if ( loaded > 1 ) {
		userHelper.setupUsersPage('#userTable');
	}
};
var i18n = new SolarNetwork.DRAS.Messages({
	context : SolarNetwork.DRAS.Config.contextMessage,
	contextUi : "<c:url value='/u/msg'/>",
	callback : function() {
		handleLoaded();
	}
});
$(document).ready(function() {
	handleLoaded();
});
</script>