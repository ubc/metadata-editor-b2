package ca.ubc.ctlt.metadataeditor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequestMapping("/settings")
public class SettingController {
	@RequestMapping(value="/index")
	public String index(WebRequest webRequest, ModelMap model) {
		
		return "settings";
	}
	
}
