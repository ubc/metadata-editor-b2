package ca.ubc.ctlt.metadataeditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import blackboard.platform.plugin.PlugIn;
import blackboard.platform.plugin.PlugInException;
import blackboard.platform.plugin.PlugInManager;
import blackboard.platform.plugin.PlugInManagerFactory;
import blackboard.platform.plugin.PlugInUtil;

/**
 * This is a helper class for your building block. Feel free to modify it to meet the needs
 * of this building block. 
 *
 */
public class BuildingBlockHelper {
	
	/** 
	 * The vendor ID for the Building Block. This must match the vendor ID in the
	 * bb-manifest.xml file.
	 */
	public static final String VENDOR_ID = "ubc";
	/** 
	 * The vendor ID for the Building Block. This must match the building block "handle"
	 * in the bb-manifest.xml file.
	 */
	public static final String HANDLE = "metadata";
	/** The name of the building block settings file in the config directory */
	public static final String SETTINGS_FILE_NAME = "settings.properties";
	
	/** 
	 * Returns the config directory for this Building Block 
	 *  
	 * @return
	 * @throws PlugInException
	 */
	public static File getConfigDirectory() throws PlugInException {
		File configDir = PlugInUtil.getConfigDirectory(VENDOR_ID, HANDLE);
		if (!configDir.exists()) {
			configDir.mkdir();
		}
		return configDir;
	}

	/** 
	 * Loads the settings file for the Building block. 
	 * 
	 * @return
	 * @throws PlugInException
	 * @throws IOException
	 */
	public static Properties loadBuildingBlockSettings() throws PlugInException, IOException {
		File settingsFile = new File(getConfigDirectory(), SETTINGS_FILE_NAME);
		if (!settingsFile.exists()) {
			settingsFile.createNewFile();
		}
		Properties settings = new Properties();
		settings.load(new FileInputStream(settingsFile));
		return settings;
	}
	
	/** 
	 * Saves the building block settings.
	 * @param props
	 * @throws PlugInException
	 * @throws IOException
	 */
	public static void saveBuildingBlockSettings(Properties props) throws PlugInException, IOException {
		File settingsFile = new File(getConfigDirectory(), SETTINGS_FILE_NAME);
		if (!settingsFile.exists()) {
			settingsFile.createNewFile();
		}
		props.store(new FileOutputStream(settingsFile), "Building Block Properties File");;
	}
	
	
	public static String displayErrorForWeb(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		String stackTrace = sw.toString();
		return "<pre>"+stackTrace+"</pre>";
	}
	
	public static String dumpCookies(HttpServletRequest request) {
		StringBuilder cookieStr = new StringBuilder("<pre>");
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				cookieStr.append(cookies[i].getName());
				cookieStr.append("=["+ cookies[i].getValue() + "]\n");
			}
		}
		cookieStr.append("</pre>");
		return cookieStr.toString();
	}
	
	/** 
	 * Get the building block version, defined in bb-manifest.xml
	 */
	public static String getVersion() {
		PlugInManager manager = PlugInManagerFactory.getInstance();
		PlugIn plugin = manager.getPlugIn(VENDOR_ID, HANDLE);
		return plugin.getVersion().toString();
	}
}
