<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
	import="blackboard.persist.*,
	blackboard.platform.*,blackboard.cms.xythos.*,
	blackboard.cms.filesystem.*,blackboard.cms.metadata.*,
	blackboard.cms.metadata.MetadataManagerFactory,
	blackboard.cms.xythos.impl.BlackboardFileMetaData,
	blackboard.platform.contentsystem.manager.*,
	blackboard.platform.contentsystem.service.*,java.io.*,java.util.*,
	ca.ubc.ctlt.metadataeditor.*,
	blackboard.platform.forms.Form
	"%>
<%@ taglib prefix="bbNG" uri="/bbNG"%>
<%@ taglib uri="/bbUI" prefix="bbUI"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<bbUI:inlineReceipt />

<bbNG:learningSystemPage title="Editing Copyright Status" ctxId="ctx">

	<bbNG:pageHeader instructions="Use of Connect must comply with the Canadian Copyright Act. Permission from the copyright holder is required to upload and post content from copyrighted works into Connect. Please see <a href='http://copyright.ubc.ca' target='_blank'>http://copyright.ubc.ca</a> for Copyright Guidelines for UBC Faculty, Staff and Students. <br /><br />Please select the appropriate copyright status for each file below.">
		<bbNG:breadcrumbBar>
			<bbNG:breadcrumb>Copyright</bbNG:breadcrumb>
		</bbNG:breadcrumbBar>
		<bbNG:pageTitleBar>Editing Copyright Status</bbNG:pageTitleBar>
	</bbNG:pageHeader>

	<bbNG:form action="save" method="post">
		<bbNG:dataCollection>
			<bbNG:step title="Verify selected files">
				<bbNG:inventoryList collection="${files}" objectVar="file"
					className="FileWrapper" description="List of selected files."
					showAll="true">
					<bbNG:listCheckboxElement name="fileSelected"
						value="${file.filePath}" />
					<bbNG:listElement label="File" name="file" isRowHeader="true"><a href="/bbcswebdav${file.filePath}" target="_blank">${file.filePath}</a></bbNG:listElement>
					<bbNG:listElement label="Copyright Status" name="copyright"
						isRowHeader="false">${file.metaValue}</bbNG:listElement>
				</bbNG:inventoryList>
			</bbNG:step>
			<bbNG:step
				title="Copyright permission to apply to the selected files">
				<bbNG:dataElement label="Copyright Status" isRequired="true"
					labelFor="copyright_select">
					<bbNG:selectElement name="copyright" isRequired="true"
						id="copyright_select">
						<bbNG:selectOptionElement value="Self Created"
							optionLabel="Self Created" />
						<bbNG:selectOptionElement value="UBC Licensed Material"
							optionLabel="UBC Licensed Material" />
						<bbNG:selectOptionElement value="Permission Granted"
							optionLabel="Permission Granted" />
						<bbNG:selectOptionElement value="Openly Licensed Material "
							optionLabel="Openly Licensed Material " />
						<bbNG:selectOptionElement value="Fair-Dealing Exception"
							optionLabel="Fair-Dealing Exception" />
					</bbNG:selectElement>
				</bbNG:dataElement>
				<ul>
					<li><b>Self Created</b> = I am the sole author and own the
						copyright in this file.</li>
					<li><b>UBC Licensed Material</b> = The terms of an existing <a
						href="http://licenses.library.ubc.ca/" target="_blank"
						title="UBC electronic resources licensing information">UBC
							license</a> allows me to upload and use this file in Connect.</li>
					<li><b>Permission Granted</b> = I have written permission from
						the copyright holder to upload and use this file.</li>
					<li><b>Openly Licensed Material</b> = The copyright holders of
						this file grants permission for its use through a Creative Commons
						or other open license, and the uploading and use of this file in
						Connect is compliant with such license terms.</li>
					<li><b>Fair-Dealing Exception</b> = I am uploading and using
						the file in Connect for the purposes of criticism, review or news
						reporting, have properly attributed the source and author of the
						file and am otherwise in compliance with the <a
						href="http://copyright.ubc.ca/fair-dealing-guidelines-for-members-of-the-ubc-community/"
						target="_blank" title="Fair Dealing guidelines for UBC">Fair
							Dealing Guidelines for Members of the UBC Community</a>.</li>
				</ul>

For more information and assistance, please visit the <a
					href="http://copyright.ubc.ca/connect" target="_blank"
					title="UBC Copyright site">UBC Copyright site</a>.
			</bbNG:step>
			<bbNG:stepSubmit />
			<input type="hidden" name="referer" value="${referer}" />
			<input type="hidden" name="path" value="${path}" />
			<c:forEach var="file" items="${files}">
				<input type="hidden" name="files" value="${file.filePath}" />
			</c:forEach>

		</bbNG:dataCollection>
	</bbNG:form>
	<bbNG:okButton url="${referer}" />
</bbNG:learningSystemPage>
