<%-- This expects the id of the form to be passed in as a param. --%>

<form id="${param.formId}" method="POST">
	<%-- Find location will store the resulting ID here, also used in edit mode to store the location id. --%>
	<input name="id" type="hidden">
	
	<div class="leftPanel">
		<table class="dialogTable">
			<tr><td class="dialogTableLabel">Name: </td><td><input name="name" type="text" class=" required"></td></tr>
			<tr><td class="dialogTableLabel">Latitude: </td><td><input name="latitude" type="text" class=" required"></td></tr>
			<tr><td class="dialogTableLabel">Longitude: </td><td><input name="longitude" type="text" class=" required"></td></tr>
			<tr><td class="dialogTableLabel">GXP: </td><td><input name="gxp" type="text"></td></tr>
			<tr><td class="dialogTableLabel">ICP: </td><td><input name="icp" type="text"></td></tr>
		</table>
	</div>
	
	<div class="rightPanel">
		<table class="dialogTable">
			<tr><td class="dialogTableLabel">Locality: </td><td><input name="locality" type="text"></td></tr>
			<tr><td class="dialogTableLabel">Region: </td><td><input name="region" type="text"></td></tr>
			<tr><td class="dialogTableLabel">State/Province: </td><td><input name="stateOrProvince" type="text"></td></tr>
			<tr><td class="dialogTableLabel">Post Code: </td><td><input name="postalCode" type="text" class=" required"></td></tr>
			<tr><td class="dialogTableLabel">Country: </td><td><input name="country" type="text"></td></tr>
		</table>
	</div>
	
	<div class="clear"></div>
</form>
