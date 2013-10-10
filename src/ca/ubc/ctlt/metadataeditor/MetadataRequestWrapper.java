package ca.ubc.ctlt.metadataeditor;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class MetadataRequestWrapper extends HttpServletRequestWrapper {

	public MetadataRequestWrapper(HttpServletRequest request) {
		super(request);
	}
	
	@Override
	public String getParameter(String paramName) {
		// this workaround is to push the "referer" parameter into
		// the request, so that the bb inventory list thinks "referer"
		// is from request and will add it to its parameter list for
		// sorting, paging, or select all URL
		if ("referer".equals(paramName)) {
			String paramValue = super.getParameter(paramName);
			if (null == paramValue) {
				return this.getHeader("referer");
			} else {
				return paramValue;
			}
		} else {
			return super.getParameter(paramName);
		}
	}

	@Override
	public Map<String, String> getParameterMap() {
		Map<String, String> map = new HashMap<String, String>(super.getParameterMap());
		map.put("referer", this.getParameter("referer"));
		return map;
	}
}
