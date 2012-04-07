<div class="ui-widget-header ui-corner-all">
Edit User
</div>

<form id="editUserForm" method="POST">
	<input type="hidden" name="user.id"/>
	
	<table>
		<tr><td>Created: </td><td class="userCreatedDate"></td></tr>
		<tr><td>Username: </td><td><input type="text" name="user.username" class="required" id="username" /></td></tr>
		<tr><td>Name: </td><td><input type="text" name="user.displayName" class="required" id="displayName"  autocomplete="off"/></td></tr>
		
		<%@ include file="userDetails.jspf" %>
		
		<tr><td></td><td><input id="saveUser" type="submit" value="Save"></td></tr>
	</table>
	
</form>
