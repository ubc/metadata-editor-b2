<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
	errorPage="error.jsp"
	import="blackboard.persist.*,
	blackboard.platform.*,blackboard.cms.xythos.*,
	blackboard.cms.filesystem.*,blackboard.cms.metadata.*,
	blackboard.cms.metadata.MetadataManagerFactory,
	blackboard.cms.xythos.impl.BlackboardFileMetaData,
	blackboard.platform.contentsystem.manager.*,
	blackboard.platform.contentsystem.service.*,
	java.io.*,java.util.*,
	ca.ubc.ctlt.metadataeditor.*,
	blackboard.platform.forms.Form,
	com.spvsoftwareproducts.blackboard.utils.B2Context
	"%>
<%@ taglib prefix="bbNG" uri="/bbNG"%>
<%@ taglib uri="/bbUI" prefix="bbUI"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
B2Context b2Context = new B2Context(request);
%>

<bbUI:inlineReceipt />

<bbNG:learningSystemPage title="Editing ${formWrapper.title}" ctxId="ctx">

	<bbNG:pageHeader instructions="${formWrapper.instructions}">
		<bbNG:breadcrumbBar>
			<bbNG:breadcrumb>${formWrapper.title}</bbNG:breadcrumb>
		</bbNG:breadcrumbBar>
		<bbNG:pageTitleBar>Editing ${formWrapper.title}</bbNG:pageTitleBar>

	</bbNG:pageHeader>

	<bbNG:form action="save" method="post">
		<bbNG:dataCollection>
			<bbNG:step title="Verify selected files">
				<bbNG:inventoryList collection="${files}" objectVar="file" 
					className="FileWrapper" description="List of selected files." enableSelectEntireList="${canSelectAll}" >
					<bbNG:listCheckboxElement name="fileSelected" value="${file.filePath}" />
					<bbNG:listElement label="File" name="file" isRowHeader="true"><a href="/bbcswebdav${file.filePath}" target="_blank">${file.filePath}</a></bbNG:listElement>
					<c:forEach items="${attributes}" varStatus="status" var="attribute">
						<c:if test="${attribute.type == 'Boolean'}">
							<jsp:useBean id="attribute" type="ca.ubc.ctlt.metadataeditor.MetadataAttribute" />
							<bbNG:listElement label="${attribute.label}" name="${attribute.id}" isRowHeader="false">
								<%="true".equals(file.getMetaValue(b2Context.getSetting(MetadataUtil.FORM_ID)).get(attribute.getId()))?"Y":""%>
							</bbNG:listElement>
						</c:if>
					</c:forEach>
					<bbNG:listElement label="Last Modified By" name="lastModifiedBy" isRowHeader="false">${file.lastModifedUser}</bbNG:listElement>
					<bbNG:listElement label="Last Modified On" name="lastModifiedOn" isRowHeader="false">${file.lastModifed}</bbNG:listElement>
				</bbNG:inventoryList>
			</bbNG:step>
			<bbNG:step title="${formWrapper.title} to apply to the selected files">
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
	
	<bbNG:jsBlock>
	<script type="text/javascript">
		// first select the individual user checkboxes 
		var checkboxes = document.forms[0].getInputs('checkbox', 'fileSelected');
		checkboxes.each(
			function (box) { box.checked = true; }
		);
		// then select the select all users checkbox
		$('listContainer_selectAll').checked = true;
	</script>
	</bbNG:jsBlock>
</bbNG:learningSystemPage>
