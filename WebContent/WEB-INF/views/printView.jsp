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
<spring:message var="list_selected_files" code="message.list_selected_files" />
<spring:message var="total" code="label.total"/>

			<bbNG:genericPage>
			<bbNG:form action="" method="post">
				<bbNG:inventoryList collection="${files}" objectVar="file" 
					className="FileWrapper" description="${list_selected_files}" enableSelectEntireList="${canSelectAll}"
					includePageParameters="false" displayPagingControls="false" showAll="true">
					<bbNG:listElement label="File" name="file" isRowHeader="true">
						${file.filePath}
					</bbNG:listElement>
					<c:forEach items="${attributes}" varStatus="status" var="attribute">
						<c:if test="${attribute.type == 'Boolean'}">
							<jsp:useBean id="attribute" type="ca.ubc.ctlt.metadataeditor.MetadataAttribute" />
							<bbNG:listElement label="${attribute.label}" name="${attribute.id}" isRowHeader="false">
								<%=Boolean.TRUE.equals(file.getMetaValue(b2Context.getSetting(MetadataUtil.FORM_ID)).get(attribute.getId()))?"Y":""%>
							</bbNG:listElement>
						</c:if>
					</c:forEach>
					<bbNG:listElement label="Created" name="created">
						${file.creationTimestamp}
					</bbNG:listElement>
				</bbNG:inventoryList>
				<div id="copyrightCountDiv" style="display: none;">
					<bbNG:inventoryList collection="${copyrightCount}" objectVar="count"  
							className="HashMap" includePageParameters="false">
							<bbNG:listElement label="File" name="file" isRowHeader="true">
								<c:out value="${total}"></c:out>
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
				</bbNG:form>
			</bbNG:genericPage>

	
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
	</script>
