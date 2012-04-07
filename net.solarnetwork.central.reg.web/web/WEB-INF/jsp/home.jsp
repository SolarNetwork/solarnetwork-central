<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<p>You're logged in as <sec:authentication property="principal.username" />.</p>
