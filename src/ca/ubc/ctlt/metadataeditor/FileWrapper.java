package ca.ubc.ctlt.metadataeditor;

import java.util.Map;

import blackboard.cms.filesystem.CSFile;

public class FileWrapper {
	private int mLastModifedUserId;
	private String mLastModifedUser;
	private String mLastModifed;
	private CSFile mFileEntry;
	private boolean mVisible;

	public FileWrapper(CSFile csFile) {
		this(csFile, true);
	}
	
	public FileWrapper(CSFile csFile, boolean visible) {
		mFileEntry = csFile;
		mVisible = visible;
	}
	
	public String getFilePath() {
		return mFileEntry.getFullPath();
	}
	
	public String getEntryId() {
		return mFileEntry.getFileSystemEntry().getEntryID();
	}

	public Map<String, String> getMetaValue(String formIdString) {
		return MetadataUtil.getMetadata(formIdString, mFileEntry.getFullPath());
	}

	public int getLastModifedUserId() {
		return mLastModifedUserId;
	}

	public void setLastModifedUserId(int mLastModifedUserId) {
		this.mLastModifedUserId = mLastModifedUserId;
	}

	public String getLastModifed() {
		return mLastModifed;
	}

	public void setLastModifed(String mLastModifed) {
		this.mLastModifed = mLastModifed;
	}

	public String getLastModifedUser() {
		return mLastModifedUser;
	}

	public void setLastModifedUser(String mLastModifedUser) {
		this.mLastModifedUser = mLastModifedUser;
	}

	public boolean isVisible() {
		return mVisible;
	}

	public void setVisible(boolean visible) {
		this.mVisible = visible;
	}
}
