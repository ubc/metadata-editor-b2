package ca.ubc.ctlt.metadataeditor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import blackboard.platform.log.LogServiceFactory;

public class CustomSimpleMappingExceptionResolver extends
		SimpleMappingExceptionResolver {
	@Override
	public ModelAndView resolveException(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) {
		final Logger log = LoggerFactory.getLogger(CustomSimpleMappingExceptionResolver.class);
		log.error("A " + ex.getClass().getSimpleName() + " has occured in the application", ex);
		LogServiceFactory.getInstance().logError(ex.getMessage(), ex);
		return super.resolveException(request, response, handler, ex);
	}
}
