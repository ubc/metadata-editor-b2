package ca.ubc.ctlt.metadataeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import blackboard.platform.forms.Form;
import blackboard.platform.log.LogServiceFactory;

import com.xythos.common.api.XythosException;

public class MetadataUtil {
	public static final String FORM_ID = "form_id";
	public static List<MetadataAttribute> attributes = null;

	public static List<MetadataAttribute> getMetadataAtttributes(String formIdString) {
		List<MetadataAttribute> attributes = new ArrayList<MetadataAttribute>();

		try {
			// find form by form ID
			Id formId = Id.generateId(Form.DATA_TYPE, formIdString);
			Form form = CSFormManagerFactory.getInstance().loadFormById(formId);

			// get all attributes in the form
			Set<AttributeDefinition> adSet = form.getAttributeDefinitions();
			for (AttributeDefinition ad : adSet) {
				MetadataAttribute attr = new MetadataAttribute();
				attr.setId(ad.getName());
				attr.setLabel(ad.getLabel());
				attr.setType(ad.getValueTypeLabel());
				attributes.add(attr);
			}
		} catch (KeyNotFoundException e) {
			LogServiceFactory.getInstance().logError("Could not find the form. Check the form ID.", e);
			throw new RuntimeException("Could not find the form. Check the form ID.", e);
		} catch (PersistenceException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading form or attribute.", e);
			throw new RuntimeException("Loading form or attribute definitions failed.", e);
		}

		return attributes;
	}

	public static Map<String, Object> getMetadata(String formIdString, String filepath) {

		// load all attribute IDs
		if (attributes == null || attributes.isEmpty()) {
			attributes = MetadataUtil.getMetadataAtttributes(formIdString);
		}

		CSContext ctxCS = null;
		Map<String, Object> ret = new HashMap<String, Object>();
		try {
			ctxCS = CSContext.getContext();
			CSEntry entry = ctxCS.findEntry(filepath);
			//CSEntryMetadata metadata = entry.getCSEntryMetadata();
			XythosMetadata mdObj = MetadataManagerFactory.getInstance().load(entry.getFileSystemEntry());
			BbAttributes bbAttributes = mdObj.getBbAttributes();
			for (MetadataAttribute attribute : attributes) {
				// BbAttributes.getXXX() may return null.
				if ("Boolean".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getBoolean(attribute.getId()));
				} else if ("Short String".equals(attribute.getType())||"String".equals(attribute.getType())||"Long String".equals(attribute.getType()) || "Unlimited String".equals(attribute.getType()) || "Medium String".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getString(attribute.getId()));
				} else if ("Integer".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getInteger(attribute.getId()));
				} else if ("Long".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getLong(attribute.getId()));
				} else if ("Double".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getDouble(attribute.getId()));
				} else if ("Float".equals(attribute.getType())) {
					ret.put(attribute.getId(), bbAttributes.getFloat(attribute.getId()));
				} else {
					throw new RuntimeException("Unsupported attribute type: " + attribute.getType());
				}
			}
		} catch (Exception e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading attribute.", e);
			ctxCS.rollback();
			throw new RuntimeException("Error loading attribute!", e);
		} finally {
			if (ctxCS != null) {
				try {
					ctxCS.commit();
				} catch (XythosException e) {
					throw new RuntimeException("CSContext committing failed!", e);
				}
			}
		}

		return ret;
	}

	public static void getFilesInPathWithMetadata(List<FileWrapper> files, CSEntry entry, int startIndex, int numResults) {
		if (entry instanceof CSFile) {
			// mark the files that are not showing on the page
			files.add(new FileWrapper((CSFile)entry));
		} else if (entry instanceof CSDirectory) {
			CSDirectory dir = (CSDirectory) entry;
			List<CSEntry> contents = dir.getDirectoryContents();
			for (CSEntry e : contents) {
				MetadataUtil.getFilesInPathWithMetadata(files, e, startIndex, numResults);
			}
		}
	}


	public static void getFilesInPath(List<String> files, String entryString) {
		CSContext ctxCS = CSContext.getContext();
		CSEntry entry = ctxCS.findEntry(entryString);
		if (entry instanceof CSFile) {
			files.add(entryString);
		} else if (entry instanceof CSDirectory) {
			CSDirectory dir = (CSDirectory) entry;
			List<CSEntry> contents = dir.getDirectoryContents();
			for (CSEntry e : contents) {
				MetadataUtil.getFilesInPath(files, e.getFullPath());
			}
		}
	}
}
