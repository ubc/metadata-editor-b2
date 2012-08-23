package ca.ubc.ctlt.metadataeditor;

public class FileWrapper {
	private String mFilePath;
	private String mMetaValue;
	
	public FileWrapper(String filePath, String metaValue) {
		mFilePath = filePath;
		mMetaValue = metaValue;
	}
	
	public String getFilePath() {
		return mFilePath;
	}
	public void setFilePath(String mFilePath) {
		this.mFilePath = mFilePath;
	}
	public String getMetaValue() {
		return mMetaValue;
	}
	public void setMetaValue(String mMetaValue) {
		this.mMetaValue = mMetaValue;
	}

}
