package ca.ubc.ctlt.metadataeditor;

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
import blackboard.cms.filesystem.CSDirectory;
import blackboard.cms.filesystem.CSEntry;
import blackboard.cms.filesystem.CSEntryMetadata;
import blackboard.cms.filesystem.CSFile;
import blackboard.data.ReceiptOptions;
import blackboard.platform.servlet.InlineReceiptUtil;

import com.spvsoftwareproducts.blackboard.utils.B2Context;
import com.xythos.common.api.XythosException;

@Controller
@RequestMapping("/copyright")
public class CopyrightController {

	@RequestMapping(value="/list")
	public String list(HttpServletRequest webRequest, @RequestHeader(value = "referer", required = false) String referer, ModelMap model) {
		List<FileWrapper> files = new ArrayList<FileWrapper>();
		B2Context b2context = new B2Context(webRequest);
		CSContext ctxCS = null;
		
		String goBackUrl = webRequest.getParameter("referer");
		if (goBackUrl == null && referer != null) {
			if (referer.indexOf("webui") != -1) {
				goBackUrl = referer;
			}
		}
		model.addAttribute("referer", goBackUrl);
		
		// path mode
		String path = webRequest.getParameter("path");
		if (null != path && !path.isEmpty() ) {
			model.addAttribute("path", path);
			try {
				ctxCS = CSContext.getContext();
				CSEntry entry = ctxCS.findEntry(path);
				files.addAll(getFilesInPath(b2context, entry));
			} catch (Exception e) {
				ctxCS.rollback();
			} finally {
				if (ctxCS != null) {
					try {
						ctxCS.commit();
					} catch (XythosException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}		

		} else {
			// file list mode
			Map<String, String[]> parameters = webRequest.getParameterMap();
			for (String parameter : parameters.keySet()) {
				if (parameter.toLowerCase().startsWith("file")) {
					String[] values = parameters.get(parameter);
					if (1 >= values.length) {
						try {
							ctxCS = CSContext.getContext();
							CSEntry entry = ctxCS.findEntry(values[0]);
							files.addAll(getFilesInPath(b2context, entry));
						} catch (Exception e) {
							ctxCS.rollback();
						} finally {
							if (ctxCS != null) {
								try {
									ctxCS.commit();
								} catch (XythosException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}	
					}
				}
			}
		}
		
		model.addAttribute("files", files);
		
		return "list";
	}
	
	
	@RequestMapping(value="/save", method = RequestMethod.POST)
	public String save(HttpServletRequest webRequest, RedirectAttributes redirectAttributes) {
		String[] fileSelected = webRequest.getParameterValues("fileSelected");
		String[] files = webRequest.getParameterValues("files");
		String option = webRequest.getParameter("copyright");
		B2Context b2context = new B2Context(webRequest);
		
		CSContext ctxCS = null;
		try {
			ctxCS = CSContext.getContext();

			for (String file : fileSelected) {
				CSEntry entry = ctxCS.findEntry(file);

				CSEntryMetadata metadata = entry.getCSEntryMetadata();

				metadata.setCustomProperty(
						b2context.getSetting(MetadataUtil.TEMPLATE_ID), option);
			}
		} catch (Exception e) {
			ctxCS.rollback();
		} finally {
			if (ctxCS != null) {
				try {
					ctxCS.commit();
				} catch (XythosException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		ReceiptOptions inlineReceiptMsgs = new ReceiptOptions();
		inlineReceiptMsgs.addSuccessMessage("Copyright status are saved!");
		InlineReceiptUtil.addReceiptToRequest(webRequest, inlineReceiptMsgs);
		//redirectAttributes.addAttribute("path", webRequest.getParameter("path"));
		redirectAttributes.addAttribute("referer", webRequest.getParameter("referer"));

		int i = 0;
		for (String file : files) {
			redirectAttributes.addAttribute("file"+i, file);
			i++;
		}
		
		return "redirect:list";
	}
	
	private List<FileWrapper> getFilesInPath(B2Context b2context, CSEntry entry) {
		List<FileWrapper> files = new ArrayList<FileWrapper>();

		if (entry instanceof CSFile) {
			String metaValue = MetadataUtil.getCopyright(b2context,
					entry.getFullPath());
			files.add(new FileWrapper(entry.getFullPath(), metaValue));
		} else if (entry instanceof CSDirectory) {
			CSDirectory dir = (CSDirectory) entry;
			List<CSEntry> contents = dir.getDirectoryContents();
			for (CSEntry e : contents) {
				files.addAll(getFilesInPath(b2context, e));
			}
		}

		return files;
	}
}
