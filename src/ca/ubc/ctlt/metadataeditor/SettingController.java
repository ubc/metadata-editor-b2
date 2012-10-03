package ca.ubc.ctlt.metadataeditor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import blackboard.cms.metadata.CSFormManagerFactory;
import blackboard.cms.metadata.XythosMetadata;
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.platform.forms.Form;
import blackboard.platform.log.LogServiceFactory;
import blackboard.platform.plugin.PlugInUtil;
import blackboard.platform.servlet.InlineReceiptUtil;

import com.spvsoftwareproducts.blackboard.utils.B2Context;

@Controller
@RequestMapping("/settings")
public class SettingController {
	@RequestMapping(value = "/index", method = RequestMethod.GET)
	public String index(HttpServletRequest webRequest, ModelMap model) {
		try {
			List<Form> forms = CSFormManagerFactory.getInstance().loadAllForms(
					XythosMetadata.DATA_TYPE);
			model.addAttribute("forms", forms);
		} catch (KeyNotFoundException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading the form. Could not find the form.", e);
			throw new RuntimeException("Could not find the form. Please contact administrator.", e);
		} catch (PersistenceException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading the form: Please contact administrator.", e);
			throw new RuntimeException("Could not load the form. Please contact administrator.", e);
		}

		B2Context b2Context = new B2Context(webRequest);

		model.addAttribute("bundle", b2Context.getResourceStrings());
		model.addAttribute("template_id", b2Context.getSetting(MetadataUtil.FORM_ID));

		return "settings";
	}
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String save(HttpServletRequest webRequest, HttpServletResponse response, ModelMap model) {
		B2Context b2Context = new B2Context(webRequest);
	
		String template_id = b2Context.getRequestParameter(
				MetadataUtil.FORM_ID, "").trim();
		b2Context.setSetting(MetadataUtil.FORM_ID, template_id);

		b2Context.persistSettings();

		return "redirect:"+InlineReceiptUtil.addSuccessReceiptToUrl(
				b2Context.getServerUrl()+PlugInUtil.getPlugInManagerURL(), 
				"The settings for Metadata Editor has been saved successfully!");
	}
	
}
