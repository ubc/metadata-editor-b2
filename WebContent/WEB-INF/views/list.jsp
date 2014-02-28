<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="com.spvsoftwareproducts.blackboard.utils.B2Context,
				ca.ubc.ctlt.metadataeditor.*,
				java.util.*"
				errorPage="error.jsp"%>
<%@ taglib prefix="bbNG" uri="/bbNG"%>
<%@ taglib prefix="metadatabbNG" uri="/WEB-INF/metadatabbNG.tld"%>
<%@ taglib uri="/bbUI" prefix="bbUI"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>

<%
B2Context b2Context = new B2Context(request);
pageContext.setAttribute("version", BuildingBlockHelper.getVersion());
%>
<spring:message var="page_title" code="title.editing" arguments="${formWrapper.title}"/>
<spring:message var="verify_file" code="label.verify_file" />
<spring:message var="list_selected_files" code="message.list_selected_files" />
<spring:message var="apply_to_files" code="label.apply_to_files" arguments="${formWrapper.title}"/>
<spring:message var="total_on_page" code="label.total_on_page"/>
<spring:message var="too_many_files_print" code="message.too_many_files_print"/>

<bbNG:learningSystemPage title="${page_title}" ctxId="ctx">

	<bbNG:pageHeader instructions="${formWrapper.instructions}">
		<bbNG:breadcrumbBar>
			<bbNG:breadcrumb title="${formWrapper.title}"/>
		</bbNG:breadcrumbBar>
		<bbNG:pageTitleBar showIcon="true" showTitleBar="true" title="${page_title}"/>
	</bbNG:pageHeader>
	
	<style type="text/css">
	#tinyfootnote
	{
		margin-top: 5em;
		font-size: 0.7em;
		text-align: center;
		color: #aaa;
	}
	#tinyfootnote a:link, #tinyfootnote a:visited
	{
		color: #888;
	}
	#print_view_button
	{
		float: right;
		margin-top: 0.5em;
	}
	#filter_apply_button
	{
		margin-top: 0.5em;
	}
	#metadataFilter, #metadataPrintView
	{
		 display: none;
	}
	</style>
	
	<bbNG:jsBlock>
	<script type="text/javascript">
	document.observe("dom:loaded", function() {
		var row = $$('#copyrightCountDiv tbody tr');
		var title = '<c:out value="${list_selected_files}" ></c:out>';
		var tab = $$('#listContainer_datatable[title="' + title + '"] tbody');
		$(row.first().childElements()).each(function(node){node.setStyle({
				'border-top': '2px solid #ccc'
			});
		});
		tab.first().insert(row.first());
	});
		
	function ubc_m_setFilter(filterId, origin)
	{
		$(filterId).checked = origin.checked;
	}

	function ubc_m_printView() {
		if ("${fn:length(files)}" > 200) {
			var msg = '<c:out value="${too_many_files_print}" ></c:out>';
			if(!confirm(msg)) {
				return false;
			}
		}
		
		$("print_view_button").addClassName("disabled");
		$('metadataPrintView').submit();
		return false;
	}
	
	/* I don't know why, but variables set by RedirectAttributes in the controller
	doesn't get resolved properly in the jsp unless you specifically tell it to
	look in the right scope (e.g.: param.var). An exception is that variables that
	start with "file" seems to work fine, no idea why that is. Session luckily
	doesn't seem to be affected. */
	<c:if test="${not empty alertsB2Url and not empty filesJson}">
		// for updating the copyright alerts building block when previously untagged files
		// gets tagged.
		function ubc_m_notifyAlerts()
		{
			var alertsB2Url = "${alertsB2Url}";
			var filesJson = '${filesJson}';
	
			var options = 
				{
					method: 'post',
					parameters: filesJson,
					contentType: 'application/json',
					asynchronous: false // intentional to make sure that the alerts building block is updated
				};
			new Ajax.Request(alertsB2Url, options);
		}
		ubc_m_notifyAlerts();
		// make sure to clean up session afterwards
		<c:remove var="alertsB2Url" scope="session" />
		<c:remove var="filesJson" scope="session" />
	</c:if>
	</script>
	</bbNG:jsBlock>

	<!-- These forms live outside of the main form because the Copyright Metadata Template has javascript that prevents form submissions
	unless one of the copyright attributes is selected. Unfortunately, that javascript applies to secondary forms like these if they're
	inside the scope of the main form. Moving them outside was a quick solution.  -->
	<!-- metadataFilter is a hacky way of doing data filtering. We resubmit the page in order to apply the selected filter against
	the list of files. Note the submit is done in it's counterpart in the main form. -->
	<bbNG:form action="list" method="post" id="metadataFilter">
		<bbNG:checkboxElement name="limitTagged" value="true" isSelected="${limitTagged}"></bbNG:checkboxElement>
		<bbNG:checkboxElement name="limitUploaded" value="true" isSelected="${limitUploaded}"></bbNG:checkboxElement>
		<bbNG:checkboxElement name="limitAccess" value="true" isSelected="${limitAccess}"></bbNG:checkboxElement>
		<bbNG:checkboxElement name="limitLinked" value="true" isSelected="${limitLinked}"></bbNG:checkboxElement>

		<input type="hidden" name="referer" value="${referer}" />
		<c:if test="${not empty path}">
			<input type="hidden" name="path" value="${path}" />
		</c:if>
		<c:forEach var="file" items="${fileSet}" varStatus="rowCounter">
			<input type="hidden" name="file${rowCounter.count}" value="${file}" />
		</c:forEach>
		<input type="submit" value="submit" />
	</bbNG:form>

	<!-- Print View Button, another form to submit to bring the user to the print view -->
	<bbNG:form action="printView" method="post" id="metadataPrintView" target="_top">
		<c:if test="${not empty path}">
			<input type="hidden" name="path" value="${path}" />
		</c:if>
		<c:forEach var="file" items="${fileSet}" varStatus="rowCounter">
			<input type="hidden" name="files${rowCounter.count}" value="${file}" />
		</c:forEach>
		<input type="submit" value="Print View" />
	</bbNG:form>
	
	<!-- Main File List Form -->
	<bbNG:form action="save" method="post">
		<bbNG:dataCollection>
			<bbNG:step title="${verify_file}">

				<!-- The actual user interactable filter selection, it sets the metadataForm with user given config. -->
				<bbNG:dataElement>
					<h3>File Filters</h3>
					<bbNG:checkboxElement optionLabel="Limit to files that do not have copyright status attribution tags." name="limitTagged" 
						id="limitTagged2" value="true" 
						onclick="ubc_m_setFilter('limitTagged', this);" isSelected="${limitTagged}"></bbNG:checkboxElement>
					<br />
					<bbNG:checkboxElement optionLabel="Limit to files uploaded by me." name="limitUploaded" id="limitUploaded2" value="true" 
						onclick="ubc_m_setFilter('limitUploaded', this);" isSelected="${limitUploaded}"></bbNG:checkboxElement>
					<br />
					<bbNG:checkboxElement optionLabel="Limit to files where I have manage or write access." name="limitAccess" 
						id="limitAccess2" value="true" 
						onclick="ubc_m_setFilter('limitAccess', this);" isSelected="${limitAccess}"></bbNG:checkboxElement>
					<br />
					<bbNG:checkboxElement optionLabel="Limit to files that are linked to a course or organization." name="limitLinked" 
						id="limitLinked2" value="true" 
						onclick="ubc_m_setFilter('limitLinked', this);" isSelected="${limitLinked}"></bbNG:checkboxElement>
					<br />
					<bbNG:button label="Apply" onClick="$('metadataFilter').submit();" id="filter_apply_button" /> 
					<!-- Print view button, placed here for convenient styling -->
					<bbNG:button onClick="ubc_m_printView();" label="Print View" id="print_view_button" />
				</bbNG:dataElement>
				
				<metadatabbNG:metadataInventoryList collection="${files}" objectVar="file" 
					className="FileWrapper" description="${list_selected_files}" enableSelectEntireList="${canSelectAll}"
					initialSortCol="file" includePageParameters="true" session="<%=ctx.getSession()%>">
					<bbNG:listCheckboxElement name="fileSelected" value="${file.filePath}" />
					<bbNG:listElement label="File" name="file" isRowHeader="true" comparator="${file.cmPath}">
						<a href="/bbcswebdav${file.filePath}" target="_blank">${file.filePath}</a>
					</bbNG:listElement>
					<c:forEach items="${attributes}" varStatus="status" var="attribute">
						<c:if test="${attribute.type == 'Boolean'}">
							<jsp:useBean id="attribute" type="ca.ubc.ctlt.metadataeditor.MetadataAttribute" />
							<bbNG:listElement label="${attribute.label}" name="${attribute.id}" isRowHeader="false">
								<%=Boolean.TRUE.equals(file.getMetaValue(b2Context.getSetting(MetadataUtil.FORM_ID)).get(attribute.getId()))?"Y":""%>
							</bbNG:listElement>
						</c:if>
					</c:forEach>
					<bbNG:listElement label="Created" name="created" comparator="${file.cmCreationTimestamp}">
						${file.creationTimestamp}
					</bbNG:listElement>
				</metadatabbNG:metadataInventoryList>
				
				<div id="copyrightCountDiv" style="display: none;">
					<bbNG:inventoryList collection="${copyrightCount}" objectVar="count"  
							className="HashMap" includePageParameters="false">
							<bbNG:listElement name="checkbox" isRowHeader="false">
							</bbNG:listElement>
							<bbNG:listElement label="File" name="file" isRowHeader="true">
								<c:out value="${total_on_page}"></c:out>
							</bbNG:listElement>
							<c:forEach items="${attributes}" varStatus="status" var="attribute">
								<c:if test="${attribute.type == 'Boolean'}">
									<bbNG:listElement label="${attribute.label}" name="${attribute.id}" isRowHeader="false">
										<c:choose>
		  										<c:when test="${empty count[attribute.id]}">0</c:when>
		  										<c:otherwise><c:out value="${count[attribute.id]}"></c:out></c:otherwise>
		  									</c:choose>
									</bbNG:listElement>
								</c:if>
							</c:forEach>
							<bbNG:listElement name="created" isRowHeader="false">
							</bbNG:listElement>
					</bbNG:inventoryList>
				</div>
				
			</bbNG:step>
			<bbNG:step title="${apply_to_files}">
				<bbNG:collapsibleList id="md" isDynamic="false">
			        <bbNG:collapsibleListItem id="1" title="${formWrapper.pageHeader}" expandOnPageLoad="true">
					<ul>
						<bbUI:formBody formBody="${formWrapper.formBody}" name="metadata" hideStepLabel="true"/>
					</ul>
     				</bbNG:collapsibleListItem>

        		</bbNG:collapsibleList>
			</bbNG:step>
			<bbNG:stepSubmit cancelUrl="${referer}"/>
			<input type="hidden" name="referer" value="${referer}" />
			<c:if test="${not empty path}">
				<input type="hidden" name="path" value="${path}" />
			</c:if>
			<c:forEach var="file" items="${fileSet}">
				<input type="hidden" name="files" value="${file}" />
			</c:forEach>

		</bbNG:dataCollection>
	</bbNG:form>   
	
	<bbNG:okButton url="${referer}" />
	
<%-- 	<bbNG:jsBlock>
	<script type="text/javascript">
		// first select the individual user checkboxes 
		var checkboxes = document.forms[0].getInputs('checkbox', 'fileSelected');
		checkboxes.each(
			function (box) { box.checked = true; }
		);
		// then select the select all users checkbox
		$('listContainer_selectAll').checked = true;
	</script>
	</bbNG:jsBlock> --%>
	
	<p id="tinyfootnote">Version: ${version}<br />Open source project, available on <a href="https://github.com/ubc/metadata-editor-b2" target="_blank">Github</a></p>
	
</bbNG:learningSystemPage>