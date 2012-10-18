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
<spring:message var="last_modified_by" code="label.last_modified_by" />
<spring:message var="last_modified_at" code="label.last_modified_at" />
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
	</style>
	
	<bbNG:form action="save" method="post">
		<bbNG:dataCollection>
			<bbNG:step title="${verify_file}">
				<bbNG:inventoryList collection="${files}" objectVar="file" 
					className="FileWrapper" description="${list_selected_files}" enableSelectEntireList="${canSelectAll}"
					initialSortCol="file" >
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
					<bbNG:listElement label="${last_modified_by}" name="lastModifiedBy" isRowHeader="false">${file.lastModifedUser}</bbNG:listElement>
					<bbNG:listElement label="${last_modified_at}" name="lastModifiedOn" isRowHeader="false">${file.lastModifed}</bbNG:listElement>
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
			<bbNG:stepSubmit />
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