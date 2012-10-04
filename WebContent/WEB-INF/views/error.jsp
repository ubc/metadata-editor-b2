<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page isErrorPage="true" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="ca.ubc.ctlt.metadataeditor.BuildingBlockHelper" %>

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
	try {
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

	} catch (Exception ex) {
		out.println(BuildingBlockHelper.displayErrorForWeb(ex));
	}
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