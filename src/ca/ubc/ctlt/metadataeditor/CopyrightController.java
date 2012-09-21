package ca.ubc.ctlt.metadataeditor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import blackboard.cms.filesystem.CSContext;
import blackboard.cms.filesystem.CSEntry;
import blackboard.cms.filesystem.CSEntryMetadata;
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
@RequestMapping("/copyright")
public class CopyrightController {

    public static class MetaDataChangeSelectQuery extends SelectQuery
    {
    	private B2Context b2context;
    	private List<FileWrapper> files;
    	
    	public MetaDataChangeSelectQuery(B2Context b2context, List<FileWrapper> files) {
    		this.b2context = b2context;
    		this.files = files;
    	}
    	
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
        	List<CopyrightAttribute> attributes = MetadataUtil.getCopyrightAtttributes(b2context.getSetting(MetadataUtil.FORM_ID));
        	StringBuilder nameClause = new StringBuilder();
        	delim = "";
			for (CopyrightAttribute attribute : attributes) {
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
            System.out.println(sql);
            
            // fill in the variables
            PreparedStatement stmt = con.prepareStatement(sql.toString());
            int index = 1;
        	for (CopyrightAttribute attribute : attributes) {
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
	public String list(HttpServletRequest webRequest, @RequestHeader(value = "referer", required = false) String referer, ModelMap model) {
		List<FileWrapper> files = new ArrayList<FileWrapper>();
		List<String> fileSet = new ArrayList<String>();
		B2Context b2context = new B2Context(webRequest);
		CSContext ctxCS = null;
		String canSelectAll = "true";
		ReceiptOptions ro = new ReceiptOptions();
		int startIndex = (webRequest.getParameter("startIndex") == null) ? 0 : Integer.parseInt((webRequest.getParameter("startIndex")));
		int numResults = (webRequest.getParameter("numResults") == null) ? 25 : Integer.parseInt((webRequest.getParameter("numResults")));
		
		// TODO need to figure out a way to set the default rows in the list
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
			if (files.size() > 1000) {
				canSelectAll = "false";
				ro.addWarningMessage("You have selected more than 1000 files. The system can only process at most 1000 files at a time. Please select fewer files in the list.");
			}
		} catch (Exception e) {
			LogServiceFactory.getInstance().logError("Exception occured while reading files ", e);
			ro.addErrorMessage("Something went wrong when reading path. "+e.getMessage(), e);
			ctxCS.rollback();
		} finally {
			if (ctxCS != null) {
				try {
					ctxCS.commit();
				} catch (XythosException e) {
					LogServiceFactory.getInstance().logError("Exception occured while commiting csContext.", e);
					ro.addErrorMessage("Something went wrong. Please contact administrator. "+e.getMessage(), e);
				}
			}
		}		


		// load the metadata changes (e.g. last modified) from database
		if (0 != files.size()) {
			try {
				MetaDataChangeSelectQuery query = new MetaDataChangeSelectQuery(b2context, files);
				PersistenceServiceFactory.getInstance().getDbPersistenceManager().runDbQuery(query);
			} catch (PersistenceException e) {
				LogServiceFactory.getInstance().logError("Exception occured while reading metadata.", e);
				ro.addErrorMessage("Something went wrong. Please contact administrator. "+e.getMessage(), e);
			}
		}
		
		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);
		
		List<CopyrightAttribute> attributes = MetadataUtil.getCopyrightAtttributes(b2context.getSetting(MetadataUtil.FORM_ID));
		model.addAttribute("attributes", attributes);
		model.addAttribute("files", files);
		model.addAttribute("fileSet", fileSet);
		model.addAttribute("canSelectAll", canSelectAll);
		try {
			model.addAttribute("form", MetadataUtil.getFormBodyByFormId(b2context.getSetting(MetadataUtil.FORM_ID)));
		} catch (PersistenceException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading the form: Please contact administrator to setup correct form.", e);
			ro.addErrorMessage("Invalid form. Please contact administrator to setup correct form. "+e.getMessage(), e);
		} catch (Exception e) {
			LogServiceFactory.getInstance().logError("Exception occured while generating form body.", e);
			ro.addErrorMessage("Invalid form body. Please contact administrator to setup correct form. "+e.getMessage(), e);
		}
		return "list";
	}
	
	
	@RequestMapping(value="/save", method = RequestMethod.POST)
	public String save(HttpServletRequest webRequest, RedirectAttributes redirectAttributes) {
		String[] fileSelected = webRequest.getParameterValues("fileSelected");
		String[] files = webRequest.getParameterValues("files");
		B2Context b2context = new B2Context(webRequest);
		ReceiptOptions ro = new ReceiptOptions();

		// find out the form integration key. e.g. bb_xxxxxxxxx
		String key = "";
		try {
			Id formId = Id.generateId(Form.DATA_TYPE, b2context.getSetting(MetadataUtil.FORM_ID));
			Form form = CSFormManagerFactory.getInstance().loadFormById(formId);
			key = "bb_" + form.getIntegrationKey().replace( "-", "" ) + "_";
		} catch (PersistenceException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading the form: Please contact administrator to setup correct form.", e);
			ro.addErrorMessage("Invalid form. Please contact administrator to setup correct form. "+e.getMessage(), e);
		}

		// load the values submitted
		List<CopyrightAttribute> attributes = MetadataUtil.getCopyrightAtttributes(b2context.getSetting(MetadataUtil.FORM_ID));
		for (CopyrightAttribute attribute : attributes) { 
			String value = webRequest.getParameter(key + attribute.getId());
			if ("Boolean".equals(attribute.getType())) {
				attribute.setValue(value==null?"N":"Y");
			} else {
				attribute.setValue(value);
			}
		}

		// try to save them
		CSContext ctxCS = null;
		try {
			ctxCS = CSContext.getContext();
			
			for (String file : fileSelected) {
				CSEntry entry = ctxCS.findEntry(file);

				CSEntryMetadata metadata = entry.getCSEntryMetadata();

				for (CopyrightAttribute attribute : attributes) { 
					metadata.setStandardProperty(attribute.getId(), attribute.getValue());
//				String xythosIdStr = entry.getFileSystemEntry().getEntryID();
//				System.out.println("xythosIdStr: "+xythosIdStr);
				}
			}
			ro.addSuccessMessage("Copyright status are saved!");
		} catch (Exception e) {
			ctxCS.rollback();
			LogServiceFactory.getInstance().logError("Exception occured while saving the metadata.", e);
			ro.addErrorMessage("Failed to save metadata. Please contact administrator. "+e.getMessage(), e);
		} finally {
			if (ctxCS != null) {
				try {
					ctxCS.commit();
				} catch (XythosException e) {
					LogServiceFactory.getInstance().logError("Exception occured while commiting csContext.", e);
					ro.addErrorMessage("Something went wrong. Please contact administrator. "+e.getMessage(), e);
				}
			}
		}

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
