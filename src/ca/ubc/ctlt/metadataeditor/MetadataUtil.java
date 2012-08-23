package ca.ubc.ctlt.metadataeditor;

import com.spvsoftwareproducts.blackboard.utils.B2Context;
import com.xythos.common.api.XythosException;

import blackboard.cms.filesystem.CSContext;
import blackboard.cms.filesystem.CSEntry;
import blackboard.cms.filesystem.CSEntryMetadata;

public class MetadataUtil {
	public static final String TEMPLATE_ID = "template_id";
	
	public static String getCopyright(B2Context b2context, String filepath) {
		CSContext ctxCS = null;
		String ret = null;
		try {
			ctxCS = CSContext.getContext();
			CSEntry entry = ctxCS.findEntry(filepath);
			CSEntryMetadata metadata = entry.getCSEntryMetadata();
			ret = metadata.getCustomProperty(b2context.getSetting(TEMPLATE_ID));
		} catch (Exception e) {
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

}
