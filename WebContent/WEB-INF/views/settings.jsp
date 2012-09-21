<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="com.spvsoftwareproducts.blackboard.utils.B2Context,
	ca.ubc.ctlt.metadataeditor.MetadataUtil"
   errorPage="../error.jsp"%>
<%@taglib prefix="bbNG" uri="/bbNG"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<bbNG:genericPage title="Copyright Settings" entitlement="system.admin.VIEW">
  <bbNG:pageHeader instructions="Instruction">
    <bbNG:breadcrumbBar environment="SYS_ADMIN_PANEL" navItem="admin_plugin_manage">
      <bbNG:breadcrumb title="Copyright Settings" />
    </bbNG:breadcrumbBar>
    <bbNG:pageTitleBar showTitleBar="true" title="Copyright Settings"/>
  </bbNG:pageHeader>
  <bbNG:form action="" id="id_simpleForm" name="simpleForm" method="post" onsubmit="return validateForm();">
  <bbNG:dataCollection markUnsavedChanges="true" showSubmitButtons="true">
    <bbNG:step hideNumber="false" id="stepOne" title="Select a template" instructions="">
      <bbNG:dataElement isRequired="true" label="Copyright Metadata Template">
      	<bbNG:selectElement name="<%=MetadataUtil.FORM_ID%>" isRequired="true" title="Copyright Template">
      		<c:forEach var="form" items="${forms}">
      			<bbNG:selectOptionElement value="${form.id.externalString}" optionLabel="${form.title}" isSelected="${template_id eq form.id.externalString}" />
      		</c:forEach>
      	</bbNG:selectElement>
      </bbNG:dataElement>
    </bbNG:step>
    <bbNG:stepSubmit hideNumber="false" showCancelButton="true" />
  </bbNG:dataCollection>
  </bbNG:form>
</bbNG:genericPage>