<div class="ui-widget-header ui-corner-all">
Create User
</div>

<form id="newUserForm" method="POST">
	
	<table>
		<tr><td>Username: </td><td><input type="text" name="user.username" class="required"/></td></tr>
		<tr><td>Name: </td><td><input type="text" name="user.displayName" class="required" autocomplete="off"/></td></tr>
		<tr><td>Password: </td><td><input type="password" name="user.password" class="required" value="" autocomplete="off"/></td></tr>
		
		<%@ include file="userDetails.jspf" %>
		
		<tr><td></td><td><input type="submit" id="createUserButton" value="Create User" /></td></tr>
	</table>

</form>
