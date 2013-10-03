package ca.ubc.ctlt.metadataeditor.CopyrightAlertsInterface;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import blackboard.platform.context.Context;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.plugin.PlugIn;
import blackboard.platform.plugin.PlugInManager;
import blackboard.platform.plugin.PlugInManagerFactory;
import blackboard.platform.plugin.PlugInUtil;

/**
 * This was initially going to be used to call up the Copyright Alerts building block API.
 * But considering just porting some code over and modify the database directly now.
 * @author john
 *
 */
public class IndexUpdater
{
	/**
	 * Send a request to the copyright-alerts building block to rescan files that's been modified.
	 * 
	 * @param files
	 * @return True if successful or if no copyright-alerts building block found, false otherwise.
	 */
	public boolean update(List<String> files)
	{
		// first, get the rest url to the copyright alerts building block
		Context ctx = ContextManagerFactory.getInstance().getContext();
		
		// get ubc-copyright-alerts plugin for use in generating the rest url
		PlugInManager pman = PlugInManagerFactory.getInstance();
		PlugIn p = pman.getPlugIn("ubc", "copyright-alerts");
		if (p == null)
		{ // copyright alerts building block not installed, do nothing
			return true;
		}
		URL url;
		try
		{
			// use the request url to get the protocol and domain information, create a relative url based on that
			url = new URL(new URL(ctx.getRequestUrl()), PlugInUtil.getUri(p, "ondemandindexer/processfiles"));
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
			return false;
		}

		// generate request header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		// generate request body
		Gson gson = new Gson();
		// create hacky representation of ProcessFiles in copyright alerts
		HashMap<String, List<String>> processFiles = new HashMap<String, List<String>>();
		processFiles.put("files", files);
		HttpEntity<String> request = new HttpEntity<String>(gson.toJson(processFiles), headers);

		// spring's rest client
		RestTemplate rt = new RestTemplate();
		try
		{
			rt.postForEntity(
					new URI(url.toString()),
					request, String.class);
		} catch (RestClientException e)
		{
			e.printStackTrace();
			return false;
		} catch (URISyntaxException e)
		{
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
}
