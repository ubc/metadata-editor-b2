package ca.ubc.ctlt.metadataeditor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import blackboard.cms.metadata.CSFormManagerFactory;
import blackboard.cms.metadata.XythosMetadata;
import blackboard.data.ReceiptOptions;
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.platform.forms.Form;
import blackboard.platform.log.LogServiceFactory;
import blackboard.platform.servlet.InlineReceiptUtil;

import com.spvsoftwareproducts.blackboard.utils.B2Context;

@Controller
@RequestMapping("/settings")
public class SettingController {
	@RequestMapping(value = "/index")
	public String index(HttpServletRequest webRequest, ModelMap model) {
		ReceiptOptions ro = new ReceiptOptions();
		
		try {
			List<Form> forms = CSFormManagerFactory.getInstance().loadAllForms(
					XythosMetadata.DATA_TYPE);
			model.addAttribute("forms", forms);
		} catch (KeyNotFoundException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading the form. Could not find the form.", e);
			ro.addErrorMessage("Could not find the form. Please contact administrator.", e);
		} catch (PersistenceException e) {
			LogServiceFactory.getInstance().logError("Exception occured while loading the form: Please contact administrator.", e);
			ro.addErrorMessage("Could not load the form. Please contact administrator.", e);
		}

		B2Context b2Context = new B2Context(webRequest);

		if (webRequest.getMethod().equalsIgnoreCase("POST")) {
			String template_id = b2Context.getRequestParameter(
					MetadataUtil.FORM_ID, "").trim();
			b2Context.setSetting(MetadataUtil.FORM_ID, template_id);

			b2Context.persistSettings();
		}

		InlineReceiptUtil.addReceiptToRequest(webRequest, ro);
		
		model.addAttribute("bundle", b2Context.getResourceStrings());
		model.addAttribute("template_id", b2Context.getSetting(MetadataUtil.FORM_ID));

		return "settings";
	}
	
}
