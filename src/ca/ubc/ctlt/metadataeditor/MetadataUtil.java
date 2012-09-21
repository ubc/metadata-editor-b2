package ca.ubc.ctlt.metadataeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import blackboard.cms.filesystem.CSContext;
import blackboard.cms.filesystem.CSDirectory;
import blackboard.cms.filesystem.CSEntry;
import blackboard.cms.filesystem.CSFile;
import blackboard.cms.metadata.CSFormManagerFactory;
import blackboard.cms.metadata.MetadataManagerFactory;
import blackboard.cms.metadata.XythosMetadata;
import blackboard.data.BbAttributes;
import blackboard.persist.Id;
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.persist.metadata.AttributeDefinition;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.forms.Form;
import blackboard.servlet.form.FormBody;
import blackboard.servlet.form.struts.DynamicFormFactory;

import com.xythos.common.api.XythosException;

public class MetadataUtil {
	public static final String FORM_ID = "form_id";
	public static List<CopyrightAttribute> attributes = null;
	
	public static List<CopyrightAttribute> getCopyrightAtttributes(String formIdString) {
		List<CopyrightAttribute> attributes = new ArrayList<CopyrightAttribute>();
		
		try {
			// find form by form ID
			Id formId = Id.generateId(Form.DATA_TYPE, formIdString);
			Form form = CSFormManagerFactory.getInstance().loadFormById(formId);

			// get all attributes in the form
			Set<AttributeDefinition> adSet = form.getAttributeDefinitions();
			for (AttributeDefinition ad : adSet) {
				CopyrightAttribute attr = new CopyrightAttribute();
				attr.setId(ad.getName());
				attr.setLabel(ad.getLabel());
				attr.setType(ad.getValueTypeLabel());
				attributes.add(attr);
			}
		} catch (KeyNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (PersistenceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return attributes;
	}
	
	public static Map<String, String> getCopyright(String formIdString, String filepath) {
		
		// load all attribute IDs
		if (attributes == null || attributes.isEmpty()) {
			attributes = MetadataUtil.getCopyrightAtttributes(formIdString);
		}
		
		CSContext ctxCS = null;
		Map<String, String> ret = new HashMap<String, String>();
		try {
			ctxCS = CSContext.getContext();
			CSEntry entry = ctxCS.findEntry(filepath);
			//CSEntryMetadata metadata = entry.getCSEntryMetadata();
			XythosMetadata mdObj = MetadataManagerFactory.getInstance().load(entry.getFileSystemEntry());
			BbAttributes bbAttributes = mdObj.getBbAttributes();
			for (CopyrightAttribute attribute : attributes) {
				//System.out.println(attribute.getType());
				if ("Boolean".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getBoolean(attribute.getId()).toString());
				} else if ("Short String".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getString(attribute.getId()));
				}
			}
		} catch (Exception e) {
			System.out.println("Exception: "+e);
			ctxCS.rollback();
		} finally {
			if (ctxCS != null) {
				try {
					ctxCS.commit();
				} catch (XythosException e) {
					e.printStackTrace();
				}
			}
		}
		
		return ret;
	}
	
	public static FormBody getFormBodyByFormId(String formIdString) throws Exception {
		FormBody formBody = null;

		// loading the copyright form
		Id formId = Id.generateId(Form.DATA_TYPE, formIdString);
		XythosMetadata metaObj = MetadataManagerFactory.getInstance().convertFromProperties( new Properties() );

		ContextManagerFactory.getInstance().getContext().setAttribute( blackboard.servlet.form.FormBodyTag.NO_DEFAULT, "Y" );
		Form form = CSFormManagerFactory.getInstance().loadFormById(formId);
		metaObj.setAssociatedFormId( form.getId() );
		
		// find the form body to be populated in view
		String key = "bb_" + form.getIntegrationKey().replace( "-", "" ) + "_";
		formBody = DynamicFormFactory.generateForm( form, metaObj, FormBody.FormType.Edit, key );
	
		return formBody;
	}
	
	public static void getFilesInPathWithMetadata(List<FileWrapper> files, CSEntry entry, int startIndex, int numResults) {
		if (entry instanceof CSFile) {
			// mark the files that are not showing on the page 
			boolean visible = false;
			if (files.size() >= startIndex && files.size() < startIndex + numResults) {
				visible = true;
			}
			files.add(new FileWrapper((CSFile)entry, visible));
		} else if (entry instanceof CSDirectory) {
			CSDirectory dir = (CSDirectory) entry;
			List<CSEntry> contents = dir.getDirectoryContents();
			for (CSEntry e : contents) {
				MetadataUtil.getFilesInPathWithMetadata(files, e, startIndex, numResults);
			}
		}
	}

}
