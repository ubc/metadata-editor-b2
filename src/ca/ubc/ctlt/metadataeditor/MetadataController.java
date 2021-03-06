package ca.ubc.ctlt.metadataeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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
import blackboard.cms.filesystem.security.UserPrincipal;
import blackboard.cms.metadata.CSFormManagerFactory;
import blackboard.data.ReceiptOptions;
import blackboard.data.user.User;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.forms.Form;
import blackboard.platform.log.LogServiceFactory;
import blackboard.platform.plugin.PlugIn;
import blackboard.platform.plugin.PlugInManager;
import blackboard.platform.plugin.PlugInManagerFactory;
import blackboard.platform.plugin.PlugInUtil;
import blackboard.platform.servlet.InlineReceiptUtil;
import ca.ubc.ctlt.metadataeditor.inventoryList.MetadataInventoryListTag;

import com.google.gson.Gson;
import com.spvsoftwareproducts.blackboard.utils.B2Context;
import com.xythos.common.api.XythosException;
import com.xythos.storageServer.api.FileSystemEntry;

@Controller
@RequestMapping("/metadata")
public class MetadataController {
	
	/**
	 * When in the content collection, the user selects files for metadata editing,
	 * which is then submitted to this building block. We need to store this
	 * selection in session in order for paging to work. This constant defines the
	 * key we use to access this stored selection from the session.
	 */
	private static final String FILES_SELECTED_FROM_CC = "ubc_metadata_files_selected_from_cc";
	
	@Autowired
    private MessageSource messageSource;
    
	public List<HashMap<String, Integer>> getCopyrightCount(List<MetadataAttribute> attributes, List<FileWrapper> files, String form_id, int startIndex, int numResults) {
		HashMap<String, Integer> count = new HashMap<String, Integer>();
		List<HashMap<String, Integer>> countList = new ArrayList<HashMap<String, Integer>>();
		int start = 0;
		int stop = numResults;
		for (FileWrapper file: files) {
			if (stop <= 0) break;
			if (start < startIndex) {
				start++;
				continue;
			}
			for (MetadataAttribute attribute: attributes) {
				if (Boolean.TRUE.equals(file.getMetaValue(form_id).get(attribute.getId()))) {
					Integer i = 1;
					if (count.get(attribute.getId()) != null) {
						i = count.get(attribute.getId()) + 1;
					}
					count.put(attribute.getId(), i);
				}
			}
			stop--;
		}
		countList.add(count);
		return countList;
	}
	
	@RequestMapping(value="/printView")
	public String printView(HttpServletRequest webRequest, ModelMap model, Locale locale) throws Exception {
		createList(webRequest, model, locale, true);
		return "printView";
	}
	
	@RequestMapping(value="/list")
	public String list(HttpServletRequest webRequest, ModelMap model, Locale locale) throws Exception {
		createList(webRequest, model, locale, false);
		return "list";
	}
	
	
	@RequestMapping(value="/save", method = RequestMethod.POST)
	public String save(HttpServletRequest webRequest, RedirectAttributes redirectAttributes, Locale locale) {
		String[] fileSelected = webRequest.getParameterValues("fileSelected");
		String selectedAll = webRequest.getParameter("selectAllFromList");
		B2Context b2context = new B2Context(webRequest);
		ReceiptOptions ro = new ReceiptOptions();
		HttpSession session = webRequest.getSession();

		CSContext ctxCS = null;
		List<String> allFiles = new ArrayList<String>();
		try {
			ctxCS = CSContext.getContext();
			// if in selected all mode
			if ("true".equals(selectedAll)) {
				List<String> filesSelectedFromCC = (List<String>) session.getAttribute(FILES_SELECTED_FROM_CC);
				for (String file : filesSelectedFromCC) {
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
			if (value == null)
			{
				value = webRequest.getParameter(key + attribute.getId() + "text");
			}
			if ("Boolean".equals(attribute.getType())) {
				attribute.setValue(value==null?"N":"Y");
			} else {
				attribute.setValue(value);
			}
		}

		// stores a list of files that hasn't been tagged before
		Set<String> newlyTaggedFiles = new HashSet<String>(allFiles);
		// Save the new metadata values in a single transaction. 
		try {
			ctxCS = CSContext.getContext();
			for (String file : allFiles) {
				boolean isNewlyTagged = true;
				for (MetadataAttribute attribute : attributes) {
					CSEntry entry = ctxCS.findEntry(file);
					CSEntryMetadata metadata = entry.getCSEntryMetadata();
					String prevVal = metadata.getStandardProperty(attribute.getId());
					if (!prevVal.isEmpty()) {
						isNewlyTagged = false;
					}
					metadata.setStandardProperty(attribute.getId(), (attribute.getValue() == null ? "" : attribute.getValue()));
				}
				if (!isNewlyTagged) {
					newlyTaggedFiles.remove(file);
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
		
		// fileSelected is empty when the user selects all, so must account for that when checking if user has made no selections
		if (fileSelected == null && !"true".equals(selectedAll)) {
			ro.addWarningMessage(messageSource.getMessage("message.save_change_empty", null, locale));
		}
		else if (allFiles.isEmpty()) {
			ro.addWarningMessage(messageSource.getMessage("message.save_change_noop", null, locale));
		}
		else {
			ro.addSuccessMessage(messageSource.getMessage("message.save_change_success", null, locale));
		}

		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);

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

		// Set parameters required for notifying the copyright alerts building block
		// about files that's been modified. We don't want the Blackboard server to
		// issue this request for performance reasons, so have to get the user's
		// browser to issue it instead. This part is mainly concerned about passing
		// the required parameters to the Javascript that'll do the request to copyright
		// alerts.
		// First, see if the ubc-copyright-alerts building block is installed
		PlugInManager pman = PlugInManagerFactory.getInstance();
		PlugIn p = pman.getPlugIn("ubc", "copyright-alerts"); // real copyright alerts
		if (p != null && !newlyTaggedFiles.isEmpty())
		{ // copyright alerts building block is installed and there are newly tagged files
			// We're using session to pass these data instead of redirect attributes
			// because bbNG:inventoryList's paging is broken with redirect attributes.
			// The "next" button url includes all param, such as the potentially
			// extremely long filesJson string, which will error out Tomcat.
			session.setAttribute("alertsB2Url", PlugInUtil.getUri(p, "ondemandindexer/processfiles"));
			HashMap<String, Set<String>> processFiles = new HashMap<String, Set<String>>();
			processFiles.put("files", newlyTaggedFiles);
			Gson gson = new Gson();
			session.setAttribute("alertsFilesJson", gson.toJson(processFiles));
		}

		return "redirect:list";
	}
	
	public void createList(HttpServletRequest webRequest, ModelMap model, Locale locale, boolean forPrint) { 
		List<FileWrapper> files = new ArrayList<FileWrapper>();
		List<String> fileSet = new ArrayList<String>();
		B2Context b2Context = new B2Context(webRequest);
		CSContext ctxCS = null;
		String canSelectAll = "true";
		// RecepitOptions is Blackboard's mechanism for displaying Success, Warning, Error messages to the user.
		// Don't create a new object unless it's empty. Otherwise, it will override the message passed from another action
		ReceiptOptions ro = InlineReceiptUtil.getReceiptFromRequest(webRequest);
		if (null == ro) {
			ro = new ReceiptOptions();
		}
		int startIndex = (forPrint || webRequest.getParameter("startIndex") == null) ? 0 : Integer.parseInt((webRequest.getParameter("startIndex")));
		if (!forPrint && webRequest.getParameter("startIndex") == null && MetadataInventoryListTag.getStaticStartIndex(ContextManagerFactory.getInstance().getContext().getSession()) != startIndex) {
			startIndex = MetadataInventoryListTag.getStaticStartIndex(ContextManagerFactory.getInstance().getContext().getSession());
		}
		int numResults = (webRequest.getParameter("numResults") == null) ? 25 : Integer.parseInt((webRequest.getParameter("numResults")));
		boolean showAll = (webRequest.getParameter("showAll") != null);
		String sortDir = webRequest.getParameter("sortDir");
		String limitTagged = webRequest.getParameter("limitTagged") == null ? "false" : "true";
		String limitUploaded = webRequest.getParameter("limitUploaded") == null ? "false" : "true";
		String limitAccess = webRequest.getParameter("limitAccess") == null ? "false" : "true";
		String limitLinked = webRequest.getParameter("limitLinked") == null ? "false" : "true";
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
		
		// Get the list of files the user selected from the content collection.

		// On the initial load, the files selected is coming from the content
		// collection in the request parameters.
		boolean loadFilesFromSession = true; // check for files in the session only if we can't find it in params
		Map<String, String[]> parameters = webRequest.getParameterMap();
		Pattern filePathKeyPattern = Pattern.compile("^file\\d+$");
		Matcher filePathMatcher = filePathKeyPattern.matcher("");
		for (String parameter : parameters.keySet()) {
			if (filePathMatcher.reset(parameter).matches()) {
				String[] values = parameters.get(parameter);
				if (1 >= values.length) {
					String path = values[0];
					fileSet.add(path);
					model.addAttribute(parameter, path);
					loadFilesFromSession = false;
				}
			}
		}
		// User might also be coming from selecting "Edit Copyright Status" in a file's drop down menu, this uses a different 
		// parameter than from the same button in the tool bar.
		String path = webRequest.getParameter("path");
		if (path != null)
		{
			fileSet.add(path);
			loadFilesFromSession = false;
		}
		HttpSession session = webRequest.getSession();
		if (!loadFilesFromSession && !fileSet.isEmpty()) {
			// store the files selected in session
			session.setAttribute(FILES_SELECTED_FROM_CC, fileSet);
		}
		// On subsequent loads (e.g.: after save and after paging), the files selected 
		// should be loaded from session
		if (loadFilesFromSession && session.getAttribute(FILES_SELECTED_FROM_CC) != null) {
			fileSet = (List<String>) session.getAttribute(FILES_SELECTED_FROM_CC);
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
			if (!forPrint && files.size() > 200) {
				//canSelectAll = "false";
				ro.addWarningMessage(messageSource.getMessage("message.too_many_files", null, locale));
			}
			if (!forPrint && noPermission) {
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

		// load the metadata changes
		// need to sort first because we need to know which files are visible to the user
		Collections.sort(files); // sort files by file path (ascending not case-sensitive)
		if (sortDir != null && sortDir.equals("DESCENDING")) { // user wants it reversed
			Collections.reverse(files);
		}
		// apply file filters
		files = applyFilters(files, b2Context, Boolean.valueOf(limitTagged), Boolean.valueOf(limitUploaded),
				Boolean.valueOf(limitAccess), Boolean.valueOf(limitLinked));
		// only get metadata for max 1000 items, it threw an error when I went over 1000
		int endIndex = startIndex + numResults; 
		if (endIndex > 1000 || showAll) {
			endIndex = 1000;
		}
		
		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);
		
		// load attributes
		List<MetadataAttribute> attributes = MetadataUtil.getMetadataAtttributes(b2Context.getSetting(MetadataUtil.FORM_ID));
		model.addAttribute("attributes", attributes);
		model.addAttribute("files", files);
		model.addAttribute("fileSet", fileSet);
		model.addAttribute("canSelectAll", canSelectAll);
		model.addAttribute("limitTagged", limitTagged);
		model.addAttribute("limitAccess", limitAccess);
		model.addAttribute("limitLinked", limitLinked);
		model.addAttribute("limitUploaded", limitUploaded);
		model.addAttribute("formWrapper", new FormWrapper(b2Context.getSetting(MetadataUtil.FORM_ID)));

		model.addAttribute("copyrightCount", getCopyrightCount(attributes, files, b2Context.getSetting(MetadataUtil.FORM_ID), startIndex, forPrint ? files.size() : numResults));
	}
	/**
	 * Helper function to remove files that match enabled filters from the user's view.
	 * @param files
	 * @return
	 */
	private List<FileWrapper> applyFilters(List<FileWrapper> files, B2Context b2Context, 
			boolean limitTagged, boolean limitUploaded, boolean limitAccess, boolean limitLinked)
	{
		if (!limitTagged && !limitUploaded && !limitAccess && !limitLinked)
		{ // no filters active
			return files;
		}

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
					String ret = file.getMetadataAttribute(attribute.getId());
					if (ret != null && !ret.isEmpty())
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
			
			if (limitLinked)
			{
				String ret = file.getMetadataAttribute("linked");
				if (ret == null || ret.isEmpty() || ret.equals("false"))
				{ // remove files that don't have the linked property or has it set to false
					it.remove();
					continue;
				}
			}
		}
		
		return files;
	}

}
