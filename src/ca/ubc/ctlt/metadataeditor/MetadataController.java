package ca.ubc.ctlt.metadataeditor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import blackboard.cms.filesystem.CSContext;
import blackboard.cms.filesystem.CSEntry;
import blackboard.cms.filesystem.CSEntryMetadata;
import blackboard.cms.filesystem.CSFileSystemException;
import blackboard.cms.filesystem.security.CSPrincipal;
import blackboard.cms.filesystem.security.CSPrincipalManager;
import blackboard.cms.filesystem.security.UserPrincipal;
import blackboard.cms.metadata.CSFormManagerFactory;
import blackboard.cms.metadata.XythosMetadataDef;
import blackboard.cms.xythos.impl.BlackboardFileMetaData;
import blackboard.data.ReceiptOptions;
import blackboard.data.user.User;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.persist.impl.SelectQuery;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.forms.Form;
import blackboard.platform.log.LogServiceFactory;
import blackboard.platform.persistence.PersistenceServiceFactory;
import blackboard.platform.servlet.InlineReceiptUtil;

import com.spvsoftwareproducts.blackboard.utils.B2Context;
import com.xythos.common.api.XythosException;
import com.xythos.storageServer.api.FileSystemEntry;

import java.util.Collections;

@Controller
@RequestMapping("/metadata")
public class MetadataController {
	
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
	public String list(HttpServletRequest webRequest, ModelMap model, Locale locale) throws Exception {
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
		boolean showAll = (webRequest.getParameter("showAll") != null);
		String sortDir = webRequest.getParameter("sortDir");
		String limitTagged = webRequest.getParameter("limitTagged") == null ? "false" : "true";
		String limitUploaded = webRequest.getParameter("limitUploaded") == null ? "false" : "true";
		String limitAccess = webRequest.getParameter("limitAccess") == null ? "false" : "true";
		
		//TODO: need to figure out a way to set the default # of rows showing in the list
//		HttpSession session = webRequest.getSession();
//		//session.setAttribute("fileFileWrapperlistContainernumResults", "100");
//		Enumeration keys = session.getAttributeNames();
//		while (keys.hasMoreElements())
//		{
//		  String key = (String)keys.nextElement();
//		  System.out.println(key + ": " + session.getValue(key) + "<br>");
//		}

		// referer parameter is set by backUrlFilter from header if it is not exists
		// otherwise, this parameter will be passed around so that we can go back 
		// when clicked on cancel or OK
		model.addAttribute("referer", webRequest.getParameter("referer"));
		
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
			boolean noPermission = false;
		
			for (String file : fileSet) {
				CSEntry entry = ctxCS.findEntry(file);
				
				MetadataUtil.getFilesInPathWithMetadata(files, entry, startIndex, numResults);
				if (!ctxCS.canWrite(entry)) {
					noPermission = true;
				}
			}
			if (files.size() > 100) {
				//canSelectAll = "false";
				ro.addWarningMessage(messageSource.getMessage("message.too_many_files", null, locale));
			}
			if (noPermission) {
				ro.addWarningMessage(messageSource.getMessage("message.permission_warning", null, locale));
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
		// need to sort first because we need to know which files are visible to the user
		Collections.sort(files); // sort files by file path (ascending not case-sensitive)
		if (sortDir != null && sortDir.equals("DESCENDING")) { // user wants it reversed
			Collections.reverse(files);
		}
		// apply file filters
		files = applyFilters(files, b2Context, Boolean.valueOf(limitTagged), Boolean.valueOf(limitUploaded), Boolean.valueOf(limitAccess));
		// only get metadata for max 1000 items, it threw an error when I went over 1000
		int endIndex = startIndex + numResults; 
		if (endIndex > 1000 || showAll) {
			endIndex = 1000;
		}
		// mark the files that we should load metadata for
		for (int i = startIndex; i < endIndex; i++) {
			if (i >= files.size()) {
				break;
			}
			files.get(i).setVisible(true);
		}
		// load metadata for visible files
		/*if (0 != files.size()) {
			try {
				MetaDataChangeSelectQuery query = new MetaDataChangeSelectQuery(b2Context, files);
				PersistenceServiceFactory.getInstance().getDbPersistenceManager().runDbQuery(query);
			} catch (PersistenceException e) {
				LogServiceFactory.getInstance().logError("Exception occured while reading metadata.", e);
				throw new RuntimeException(messageSource.getMessage("message.contact_admin", null, locale), e);
			}
		}*/
		
		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);
		
		// load attributes
		List<MetadataAttribute> attributes = MetadataUtil.getMetadataAtttributes(b2Context.getSetting(MetadataUtil.FORM_ID));
		model.addAttribute("attributes", attributes);
		model.addAttribute("files", files);
		model.addAttribute("fileSet", fileSet);
		model.addAttribute("canSelectAll", canSelectAll);
		model.addAttribute("limitTagged", limitTagged);
		model.addAttribute("limitAccess", limitAccess);
		model.addAttribute("limitUploaded", limitUploaded);
		model.addAttribute("formWrapper", new FormWrapper(b2Context.getSetting(MetadataUtil.FORM_ID)));

		return "list";
	}
	
	
	@RequestMapping(value="/save", method = RequestMethod.POST)
	public String save(HttpServletRequest webRequest, RedirectAttributes redirectAttributes, Locale locale) {
		String[] fileSelected = webRequest.getParameterValues("fileSelected");
		String selectedAll = webRequest.getParameter("selectAllFromList");
		String[] files = webRequest.getParameterValues("files");
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
				allFiles = new ArrayList<String>(Arrays.asList(fileSelected));
			}
			
			// do not process files that we don't have permission to change
			Iterator<String> i = allFiles.iterator();
			while(i.hasNext()) {
				String file = i.next();
				CSEntry entry = ctxCS.findEntry(file);
				if (!ctxCS.canWrite(entry)) {
					i.remove();
					String[] arg = {file};
					ro.addWarningMessage(messageSource.getMessage("message.permission_error", arg, locale));
				}
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

		// Save the new metadata values in a single transaction. 
		try {
			ctxCS = CSContext.getContext();
			for (String file : allFiles) {
				for (MetadataAttribute attribute : attributes) {
					CSEntry entry = ctxCS.findEntry(file);
					CSEntryMetadata metadata = entry.getCSEntryMetadata();
					metadata.setStandardProperty(attribute.getId(), (attribute.getValue() == null ? "" : attribute.getValue()));
				}
			}
		} catch (Exception e) {
			ctxCS.rollback();
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
		
		if (fileSelected == null) {
			ro.addWarningMessage(messageSource.getMessage("message.save_change_empty", null, locale));
		}
		else if (allFiles.isEmpty()) {
			ro.addWarningMessage(messageSource.getMessage("message.save_change_noop", null, locale));
		}
		else {
			ro.addSuccessMessage(messageSource.getMessage("message.save_change_success", null, locale));
		}

		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);

		// forwarding the parameters for the list page
		int i = 0;
		for (String file : files) {
			redirectAttributes.addAttribute("file" + i, file);
			i++;
		}
		// forward single value parameters that aren't files, this is needed to persist file filters
		Map<String, String[]> parameters = webRequest.getParameterMap();
		for (String parameter : parameters.keySet()) {
			if (!parameter.toLowerCase().startsWith("file")) {
				String[] values = parameters.get(parameter);
				if (values.length == 1)
				{
					redirectAttributes.addAttribute(parameter, values[0]);
				}
			}
		}

		return "redirect:list";
	}
	
	/**
	 * Helper function to remove files that match enabled filters from the user's view.
	 * @param files
	 * @return
	 */
	private List<FileWrapper> applyFilters(List<FileWrapper> files, B2Context b2Context, 
			boolean limitTagged, boolean limitUploaded, boolean limitAccess)
	{
		//System.out.println("Limit P: " + limitTagged + " Up " + limitUploaded + " Ac " + limitAccess);
		if (!limitTagged && !limitUploaded && !limitAccess)
		{ // no filters active
			return files;
		}

		// TODO
		/*
		 * BUGFIX: Selecting folders work file for limiting files, selecting individual files doesn't
		 * can be fixed by back button?
		 */
		// TODO
		
		User user = ContextManagerFactory.getInstance().getContext().getUser();
		CSContext ctx = CSContext.getContext();
		List<MetadataAttribute> attributes = MetadataUtil.getMetadataAtttributes(b2Context.getSetting(MetadataUtil.FORM_ID));
		for (Iterator<FileWrapper> it = files.iterator(); it.hasNext();)
		{
			FileWrapper file = it.next();
			if (limitTagged)
			{ // remove files that have already been copyright tagged
				boolean gotoNext = false;
				for (MetadataAttribute attribute : attributes)
				{
					//System.out.println("Checking tag: " + file.getFilePath());
					Boolean ret = (Boolean) file.getMetaValue(b2Context.getSetting(MetadataUtil.FORM_ID)).get(attribute.getId());
					if (ret != null && ret != false)
					{ // there is a valid tag, so remove the file from listing
						it.remove();
						gotoNext = true;
						break;
					}
				}
				if (gotoNext) continue;
			}

			CSEntry entry = ctx.findEntry(file.getFilePath());
			if (entry == null) return files; // just in case

			if (limitUploaded)
			{ // remove files that the user did not upload
				FileSystemEntry fse = entry.getFileSystemEntry(); // note: undocumented api
				try
				{ // basically, if the file's principal id for the creator does not match the current user, we remove the file
					if (!UserPrincipal.calculatePrincipalID(user).equals(fse.getCreatedByPrincipalID()))
					{
						it.remove();
						continue;
					}
				} catch (XythosException e)
				{
					e.printStackTrace();
				}
			}

			if (limitAccess)
			{
				if (!ctx.canManage(entry) && !ctx.canWrite(entry))
				{ // user doesn't have read or write acces to file, so remove it
					it.remove();
					continue;
				}
			}
		}
		
		return files;
	}

}
