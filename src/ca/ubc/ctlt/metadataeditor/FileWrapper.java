package ca.ubc.ctlt.metadataeditor;

import java.util.Map;

import blackboard.base.GenericFieldComparator;
import blackboard.cms.filesystem.CSFile;

public class FileWrapper implements Comparable {
	private int mLastModifedUserId;
	private String mLastModifedUser;
	private String mLastModifed;
	private CSFile mFileEntry;
	private boolean mVisible;
	
	/** Comparator to enable sorting by path for bbNG:inventoryList */
	private static GenericFieldComparator<FileWrapper> cmPath = 
			new GenericFieldComparator<FileWrapper>("getFilePath", FileWrapper.class);

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

	public Map<String, Object> getMetaValue(String formIdString) {
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

	/**
	 * Comparator to enable sorting by path for bbNG:inventoryList
	 * @return the cmPath
	 */
	public GenericFieldComparator<FileWrapper> getCmPath()
	{
		return cmPath;
	}

	/**
	 * Note that this comparison MUST match the same sort used for bbNG:inventoryList
	 */
	@Override
	public int compareTo(Object arg0)
	{
		FileWrapper other = (FileWrapper) arg0;
		return getFilePath().compareToIgnoreCase(other.getFilePath());
	}
}
