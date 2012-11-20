<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/response-1.0" prefix="res" %>
<res:setStatus status="SC_METHOD_NOT_ALLOWED"/>
<res:setHeader name="Allow">POST</res:setHeader>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
	<head>
		<title>HTTP GET not supported</title>
	</head>
	<body>
		<p>The HTTP <code>GET</code> method is not allowed for this action. Please 
		use the HTTP <code>POST</code> method.</p>
	</body>
</html>
