<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="com.spvsoftwareproducts.blackboard.utils.B2Context,
				ca.ubc.ctlt.metadataeditor.*,
				java.util.*"
				errorPage="error.jsp"%>
<%@ taglib prefix="bbNG" uri="/bbNG"%>
<%@ taglib uri="/bbUI" prefix="bbUI"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>

<%
B2Context b2Context = new B2Context(request);
pageContext.setAttribute("version", BuildingBlockHelper.getVersion());
%>
<spring:message var="page_title" code="title.editing" arguments="${formWrapper.title}"/>
<spring:message var="verify_file" code="label.verify_file" />
<spring:message var="list_selected_files" code="message.list_selected_files" />
<spring:message var="apply_to_files" code="label.apply_to_files" arguments="${formWrapper.title}"/>

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
	#metadataFilter
	{
		 display: none;
	}
	</style>
	
	<bbNG:jsBlock>
	<script type="text/javascript">
	function ubc_m_setFilter(filterId, origin)
	{
		$(filterId).checked = origin.checked;
		$('metadataFilter').submit();
	}
	</script>
	</bbNG:jsBlock>

	<!-- metadataFilter is a hacky way of doing data filtering. We resubmit the page in order to get a list of files
	that doesn't have files that have already been copyright tagged. -->
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
	
	<bbNG:form action="save" method="post">
		<bbNG:dataCollection>
			<bbNG:step title="${verify_file}">
<!-- 
2. Ability to filter the list for the following conditions:
	Where user is uploader
	Where user has manage or write access
	Where files are linked into the course
	 - no easy way to determine if files are linked by a course
	 - would have to search through all course content (very very slow)
-->
				<bbNG:dataElement>
					<h3>File Filters</h3>
					<bbNG:checkboxElement optionLabel="Limit to files that have not been tagged." name="limitTagged" 
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
					<bbNG:checkboxElement optionLabel="Limit to files that has been linked." name="limitLinked" 
						id="limitLinked2" value="true" 
						onclick="ubc_m_setFilter('limitLinked', this);" isSelected="${limitLinked}"></bbNG:checkboxElement>
					<br />
				</bbNG:dataElement>

				<bbNG:inventoryList collection="${files}" objectVar="file" 
					className="FileWrapper" description="${list_selected_files}" enableSelectEntireList="${canSelectAll}"
					initialSortCol="file" includePageParameters="true">
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
				</bbNG:inventoryList>
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