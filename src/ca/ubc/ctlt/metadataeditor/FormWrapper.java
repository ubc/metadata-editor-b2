package ca.ubc.ctlt.metadataeditor;

import java.util.Properties;

import blackboard.cms.metadata.CSFormManagerFactory;
import blackboard.cms.metadata.MetadataManagerFactory;
import blackboard.cms.metadata.XythosMetadata;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.forms.Form;
import blackboard.platform.log.LogServiceFactory;
import blackboard.servlet.form.FormBody;
import blackboard.servlet.form.struts.DynamicFormFactory;

public class FormWrapper {
	private Form form = null;
	private FormBody formBody = null;
	private String formIdString = null;
	private XythosMetadata metaObj = null;

	public FormWrapper(String formIdString) {
		this.formIdString = formIdString;
	}

	public String getFormIdString() {
		return formIdString;
	}

	public void setFormIdString(String formIdString) {
		this.formIdString = formIdString;
	}

	public Form getForm() {
		if (form == null) {
			if (null == this.getFormIdString()) {
				throw new RuntimeException("Empty form ID!");
			}
			// loading the metadata form
			Id formId;
			try {
				formId = Id.generateId(Form.DATA_TYPE, this.getFormIdString());
				metaObj = MetadataManagerFactory.getInstance().convertFromProperties( new Properties() );
	
				ContextManagerFactory.getInstance().getContext().setAttribute( blackboard.servlet.form.FormBodyTag.NO_DEFAULT, "Y" );
				form = CSFormManagerFactory.getInstance().loadFormById(formId);
				metaObj.setAssociatedFormId( form.getId() );
			} catch (PersistenceException e) {
				LogServiceFactory.getInstance().logError("Failed to load form. Check form ID.", e);
				throw new RuntimeException("Failed to load form. Check form ID.", e);
			}
		}
		
		return form;
	}
	
	public void setForm(Form form) {
		this.form = form;
	}
	
	public FormBody getFormBody() {
		if (null == formBody) {
			// find the form body to be populated in view
			String key;
			try {
				key = "bb_" + this.getForm().getIntegrationKey().replace( "-", "" ) + "_";
				formBody = DynamicFormFactory.generateForm( this.getForm(), metaObj, FormBody.FormType.Edit, key );
			} catch (Exception e) {
				LogServiceFactory.getInstance().logError("Failed to load form body. Check the metadata template form.", e);
				throw new RuntimeException("Failed to load form body. Check the metadata template form.", e);
			}
		}

		return formBody;
	}
	
	public void setFormBody(FormBody formBody) {
		this.formBody = formBody;
	}
	
	public String getTitle() {
		return this.getForm().getTitle();
	}
	
	public String getPageHeader() {
		return this.getForm().getPageHeader();
	}
	
	public String getInstructions() {
		return this.getForm().getInstructions().getFormattedText();
	}
}