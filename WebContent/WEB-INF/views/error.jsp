<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java"
    isErrorPage="true"
    import="org.slf4j.Logger, 
    org.slf4j.LoggerFactory,
    ca.ubc.ctlt.metadataeditor.BuildingBlockHelper,
    blackboard.platform.plugin.PlugInUtil
    " %>

<%@ taglib prefix="bbNG" uri="/bbNG"%>
<%@ taglib uri="/bbUI" prefix="bbUI"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	final Logger log = LoggerFactory.getLogger("ca.ubc.ctlt.metadataeditor.internal-error");
	log.error("Internal Server Error", exception);
%>

<bbNG:learningSystemPage title="Internal Error">

<h2>Internal error</h2>

<p><b>${exception.message}</b></p>

<br />

<div>
<h6><a href="#" onclick="toggleDetails(); return false;">Show/Hide Details</a></h6>
<ul id="details">
<%
// The Servlet spec guarantees this attribute will be available
//Throwable exception = (Throwable) request.getAttribute("javax.servlet.error.exception"); 

if (exception != null) {
	if (exception instanceof ServletException) {
		// It's a ServletException: we should extract the root cause
		ServletException sex = (ServletException) exception;
		Throwable rootCause = sex.getRootCause();
		if (rootCause == null)
			rootCause = sex;
		out.println("** Root cause is: " + rootCause.getMessage());
		out.println(BuildingBlockHelper.displayErrorForWeb(rootCause));
	} else {
		// It's not a ServletException, so we'll just show it
		out.println(BuildingBlockHelper.displayErrorForWeb(exception));
	}
} else {
	out.println("No error information available");
}

// Display cookies
out.println("<br />Cookies:<br />");
out.println(BuildingBlockHelper.dumpCookies(request));
out.println("<br />Version: " + BuildingBlockHelper.getVersion() + "<br />");
%>
</ul>
</div>

<bbNG:jsBlock>
<script type="text/javascript">
toggleDetails();
function toggleDetails() 
{
	$('details').toggle();
}
</script>
</bbNG:jsBlock>  

</bbNG:learningSystemPage>