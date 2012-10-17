package ca.ubc.ctlt.metadataeditor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import blackboard.cms.filesystem.CSContext;
import blackboard.cms.filesystem.CSEntry;
import blackboard.cms.filesystem.CSEntryMetadata;
import blackboard.cms.filesystem.CSFileSystemException;
import blackboard.cms.metadata.CSFormManagerFactory;
import blackboard.data.ReceiptOptions;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.persist.impl.SelectQuery;
import blackboard.platform.forms.Form;
import blackboard.platform.log.LogServiceFactory;
import blackboard.platform.persistence.PersistenceServiceFactory;
import blackboard.platform.servlet.InlineReceiptUtil;

import com.spvsoftwareproducts.blackboard.utils.B2Context;
import com.xythos.common.api.XythosException;

@Controller
@RequestMapping("/metadata")
public class MedadataController {
	
	@Autowired
    private MessageSource messageSource;
	
    public static class MetaDataChangeSelectQuery extends SelectQuery
    {
    	private B2Context b2context;
    	private List<FileWrapper> files;
    	
    	public MetaDataChangeSelectQuery(B2Context b2context, List<FileWrapper> files) {
    		this.b2context = b2context;
    		this.files = files;
    	}
    	  
    	@Override
		protected void processRow(ResultSet rst) throws SQLException {
			String location = rst.getString(3);
			for (FileWrapper file : files) {
				if (file.getFilePath().equals(location)) {
					file.setLastModifedUserId(rst.getInt(1));
					file.setLastModifed(rst.getString(4));
					file.setLastModifedUser(rst.getString(5));
				}
			}
		}

    	@Override
        protected Statement prepareStatement(Connection con)
            throws SQLException
        {
			// count files for select statement
			StringBuilder fileClause = new StringBuilder();
			String delim = "";
			for (FileWrapper file : files) {
				// don't need to query the files that not visible on the screen
				if (!file.isVisible()) {
					continue;
				}
				fileClause.append(delim).append('?');
				delim = ",";
			}
        	
        	// load attributes and count them for select statement
        	List<MetadataAttribute> attributes = MetadataUtil.getMetadataAtttributes(b2context.getSetting(MetadataUtil.FORM_ID));
        	StringBuilder nameClause = new StringBuilder();
        	delim = "";
			for (MetadataAttribute attribute : attributes) {
				nameClause.append(delim).append('?');
				delim = ",";
			}
        	
        	// prepare the sql statement
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT users_pk1, name, location, timestamp, u.firstname || ' ' || u.lastname, value ");
            sql.append("FROM CMS_METADATA_CHANGES c, USERS u ");
            sql.append("WHERE c.pk1 IN ( ");
            	sql.append("SELECT max(pk1) FROM ( ");
            		sql.append("SELECT pk1, users_pk1, location, name, value, timestamp, max(TIMESTAMP) OVER (PARTITION BY location) AS MAX_TIMESTAMP ");
            		sql.append("FROM CMS_METADATA_CHANGES ");
            		sql.append("WHERE name IN (" + nameClause.toString() + ")  AND xythos_id IN (" + fileClause.toString() + ")");
            	sql.append(") GROUP BY location, MAX_TIMESTAMP");
            sql.append(") AND c.users_pk1 = u.pk1");
            //System.out.println(sql);
            
            // fill in the variables
            PreparedStatement stmt = con.prepareStatement(sql.toString());
            int index = 1;
        	for (MetadataAttribute attribute : attributes) {
        		stmt.setString(index, attribute.getId());
        		index++;
        	}
        	
            for (FileWrapper file : files) {
				if (!file.isVisible()) {
					continue;
				}
            	stmt.setString(index, "xid-"+file.getEntryId());
            	index++;
            }

            return stmt;
        }
    }
    
	@RequestMapping(value="/list")
	public String list(HttpServletRequest webRequest, @RequestHeader(value = "referer", required = false) String referer, ModelMap model, Locale locale) throws Exception {
		List<FileWrapper> files = new ArrayList<FileWrapper>();
		List<String> fileSet = new ArrayList<String>();
		B2Context b2Context = new B2Context(webRequest);
		CSContext ctxCS = null;
		String canSelectAll = "true";
		// Don't create a new object unless it's empty. Otherwise, it will override the message passed from another action
		ReceiptOptions ro = InlineReceiptUtil.getReceiptFromRequest(webRequest);
		if (null == ro) {
			ro = new ReceiptOptions();
		}
		int startIndex = (webRequest.getParameter("startIndex") == null) ? 0 : Integer.parseInt((webRequest.getParameter("startIndex")));
		int numResults = (webRequest.getParameter("numResults") == null) ? 25 : Integer.parseInt((webRequest.getParameter("numResults")));
		
		//TODO: need to figure out a way to set the default # of rows showing in the list
//		HttpSession session = webRequest.getSession();
//		//session.setAttribute("fileFileWrapperlistContainernumResults", "100");
//		Enumeration keys = session.getAttributeNames();
//		while (keys.hasMoreElements())
//		{
//		  String key = (String)keys.nextElement();
//		  System.out.println(key + ": " + session.getValue(key) + "<br>");
//		}

		//PagingInfo pagingInfo = new 

		// figure out the where to go back when cancel button is clicked
		String goBackUrl = webRequest.getParameter("referer");
		if (goBackUrl == null && referer != null) {
			if (referer.indexOf("webui") != -1) {
				goBackUrl = referer;
			}
		}
		model.addAttribute("referer", goBackUrl);
		
		// loading the selected files
		String path = webRequest.getParameter("path");
		if (null != path && !path.isEmpty() ) {
			fileSet.add(path);
		} else {
			Map<String, String[]> parameters = webRequest.getParameterMap();
			for (String parameter : parameters.keySet()) {
				if (parameter.toLowerCase().startsWith("file")) {
					String[] values = parameters.get(parameter);
					if (1 >= values.length) {
						fileSet.add(values[0]);
					}
				}
			}
		}
		
		try {
			ctxCS = CSContext.getContext();
			for (String file : fileSet) {
				CSEntry entry = ctxCS.findEntry(file);
				
				MetadataUtil.getFilesInPathWithMetadata(files, entry, startIndex, numResults);
			}
			if (files.size() > 100) {
				//canSelectAll = "false";
				ro.addWarningMessage(messageSource.getMessage("message.too_many_files", null, locale));
			}
		} catch (CSFileSystemException e) {
			ctxCS.rollback();
			LogServiceFactory.getInstance().logError("Exception occured while reading files ", e);
			throw new RuntimeException(messageSource.getMessage("message.error_reading_files", null, locale), e);
		} finally {
			if (ctxCS != null) {
				try {
					ctxCS.commit();
				} catch (XythosException e) {
					LogServiceFactory.getInstance().logError("Exception occured while commiting csContext.", e);
					throw new RuntimeException(messageSource.getMessage("message.contact_admin", null, locale), e);
				}
			}
		}		


		// load the metadata changes (e.g. last modified) from database
		if (0 != files.size()) {
			try {
				MetaDataChangeSelectQuery query = new MetaDataChangeSelectQuery(b2Context, files);
				PersistenceServiceFactory.getInstance().getDbPersistenceManager().runDbQuery(query);
			} catch (PersistenceException e) {
				LogServiceFactory.getInstance().logError("Exception occured while reading metadata.", e);
				throw new RuntimeException(messageSource.getMessage("message.contact_admin", null, locale), e);
			}
		}
		
		
		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);
		
		List<MetadataAttribute> attributes = MetadataUtil.getMetadataAtttributes(b2Context.getSetting(MetadataUtil.FORM_ID));
		model.addAttribute("attributes", attributes);
		model.addAttribute("files", files);
		model.addAttribute("fileSet", fileSet);
		model.addAttribute("canSelectAll", canSelectAll);
		model.addAttribute("formWrapper", new FormWrapper(b2Context.getSetting(MetadataUtil.FORM_ID)));

		return "list";
	}
	
	
	@RequestMapping(value="/save", method = RequestMethod.POST)
	public String save(HttpServletRequest webRequest, RedirectAttributes redirectAttributes, Locale locale) {
		String[] fileSelected = webRequest.getParameterValues("fileSelected");
		String selectedAll = webRequest.getParameter("selectAllFromList");
		String[] files = webRequest.getParameterValues("files");
		List<CSEntryMetadata> metadatas = new ArrayList<CSEntryMetadata>();
		B2Context b2context = new B2Context(webRequest);
		ReceiptOptions ro = new ReceiptOptions();

		CSContext ctxCS = null;
		List<String> allFiles = new ArrayList<String>();
		try {
			ctxCS = CSContext.getContext();
			// if in selected all mode
			if ("true".equals(selectedAll)) {
				for (String file : files) {
					List<String> f = new ArrayList<String>();
					MetadataUtil.getFilesInPath(f, file);
					allFiles.addAll(f);
				}
			} else if (fileSelected != null) {
				allFiles = Arrays.asList(fileSelected);
			}
			
			for (String file : allFiles) {
				CSEntry entry = ctxCS.findEntry(file);

				CSEntryMetadata metadata = entry.getCSEntryMetadata();
				metadatas.add(metadata);
			}
		} catch (CSFileSystemException e) {
			ctxCS.rollback();
			LogServiceFactory.getInstance().logError("Exception occured while saving the metadata.", e);
			throw new RuntimeException(messageSource.getMessage("message.failed_save_metadata", null, locale), e);
		} finally {
			if (ctxCS != null) {
				try {
					ctxCS.commit();
				} catch (XythosException e) {
					LogServiceFactory.getInstance().logError("Exception occured while commiting csContext.", e);
					throw new RuntimeException(messageSource.getMessage("message.contact_admin", null, locale), e);
				}
			}
		}
		
		// find out the form integration key. e.g. bb_xxxxxxxxx
		String key = "";
		try {
			Id formId = Id.generateId(Form.DATA_TYPE, b2context.getSetting(MetadataUtil.FORM_ID));
			Form form = CSFormManagerFactory.getInstance().loadFormById(formId);
			key = "bb_" + form.getIntegrationKey().replace( "-", "" ) + "_";
		} catch (PersistenceException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading the form: Please contact administrator to setup correct form.", e);
			throw new RuntimeException(messageSource.getMessage("message.invalid_form", null, locale), e);
		}

		// load the values submitted
		List<MetadataAttribute> attributes = MetadataUtil.getMetadataAtttributes(b2context.getSetting(MetadataUtil.FORM_ID));
		for (MetadataAttribute attribute : attributes) { 
			String value = webRequest.getParameter(key + attribute.getId());
			if ("Boolean".equals(attribute.getType())) {
				attribute.setValue(value==null?"N":"Y");
			} else {
				attribute.setValue(value);
			}
		}

		/* OK, the code below doesn't work when the files are imported from archive. 
		   A CSFileSystemException was thrown out with "Transaction Ended" message. 
		   It seems system is trying to create xythos property with createXythosProperty 
		   and somehow the transaction ends, maybe closed by createXythosProperty.  So 
		   the second setStandardProperty call fails. The workaround is to get the file 
		   system entry every time before making setStandardProperty calls. This make 
		   setting the properties slow.
		*/
//		for (CSEntryMetadata metadata : metadatas) {
//			for (MetadataAttribute attribute : attributes) { 
//				System.out.println(metadata.getStandardProperty(attribute.getId()));
//				System.out.println(attribute.getId()+"="+(attribute.getValue() == null ? "" : attribute.getValue()));
//				metadata.setStandardProperty(attribute.getId(), (attribute.getValue() == null ? "" : attribute.getValue()));
////				String xythosIdStr = entry.getFileSystemEntry().getEntryID();
////				System.out.println("xythosIdStr: "+xythosIdStr);
//			}
//		}
		for (String file : allFiles) {
			for (MetadataAttribute attribute : attributes) {
				CSEntry entry = ctxCS.findEntry(file);
				CSEntryMetadata metadata = entry.getCSEntryMetadata();
				metadata.setStandardProperty(attribute.getId(), (attribute.getValue() == null ? "" : attribute.getValue()));
			}
		}
		
		ro.addSuccessMessage(messageSource.getMessage("message.save_change_success", null, locale));

		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);

		// forwarding the parameters for the list page
		int i = 0;
		for (String file : files) {
			redirectAttributes.addAttribute("file" + i, file);
			i++;
		}
		redirectAttributes.addAttribute("referer", webRequest.getParameter("referer"));

		return "redirect:list";
	}
}
