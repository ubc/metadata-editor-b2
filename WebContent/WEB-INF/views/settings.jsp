<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="com.spvsoftwareproducts.blackboard.utils.B2Context,
	ca.ubc.ctlt.metadataeditor.MetadataUtil"
   errorPage="../error.jsp"%>
<%@taglib uri="/bbNG" prefix="bbNG" %>
<bbNG:genericPage title="Copyright Settings" entitlement="system.admin.VIEW">

<%
  B2Context b2Context = new B2Context(request);

  if (request.getMethod().equalsIgnoreCase("POST")) {
    String template_id = b2Context.getRequestParameter(MetadataUtil.TEMPLATE_ID, "").trim();
    b2Context.setSetting(MetadataUtil.TEMPLATE_ID, template_id);

    b2Context.persistSettings();
  }

  pageContext.setAttribute("bundle", b2Context.getResourceStrings());
%>
  <bbNG:pageHeader instructions="Instruction">
    <bbNG:breadcrumbBar environment="SYS_ADMIN_PANEL" navItem="admin_plugin_manage">
      <bbNG:breadcrumb title="Copyright Settings" />
    </bbNG:breadcrumbBar>
    <bbNG:pageTitleBar showTitleBar="true" title="Copyright Settings"/>
  </bbNG:pageHeader>
  <bbNG:form action="" id="id_simpleForm" name="simpleForm" method="post" onsubmit="return validateForm();">
  <bbNG:dataCollection markUnsavedChanges="true" showSubmitButtons="true">
    <bbNG:step hideNumber="false" id="stepOne" title="Template Field Id" instructions="template field id">
      <bbNG:dataElement isRequired="true" label="Copyright Metadata Template Field ID">
        <bbNG:textElement name="<%=MetadataUtil.TEMPLATE_ID%>" value="<%=b2Context.getSetting(MetadataUtil.TEMPLATE_ID)%>" helpText="template id" size="30" minLength="1" />
      </bbNG:dataElement>
    </bbNG:step>
    <bbNG:stepSubmit hideNumber="false" showCancelButton="true" />
  </bbNG:dataCollection>
  </bbNG:form>
</bbNG:genericPage>