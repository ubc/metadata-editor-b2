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
import org.springframework.http.ResponseEntity;
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
	public void update(List<String> files)
	{
		// first, get the rest url to the copyright alerts building block
		Context ctx = ContextManagerFactory.getInstance().getContext();
		
		// get ubc-copyright-alerts plugin for use in generating the rest url
		PlugInManager pman = PlugInManagerFactory.getInstance();
		PlugIn p = pman.getPlugIn("ubc", "copyright-alerts");
		if (p == null)
		{ // copyright alerts building block not installed, do nothing
			return;
		}
		URL url;
		try
		{
			// use the request url to get the protocol and domain information, create a relative url based on that
			url = new URL(new URL(ctx.getRequestUrl()), PlugInUtil.getUri(p, "ondemandindexer/processfiles"));
		} catch (MalformedURLException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
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
		// response
		ResponseEntity<String> resp;

		// spring's rest client
		RestTemplate rt = new RestTemplate();
		try
		{
			resp = rt.postForEntity(
					new URI(url.toString()),
					request, String.class);
		} catch (RestClientException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
