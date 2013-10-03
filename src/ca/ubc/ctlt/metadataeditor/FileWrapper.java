package ca.ubc.ctlt.metadataeditor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Map;

import blackboard.base.GenericFieldComparator;
import blackboard.cms.filesystem.CSEntryMetadata;
import blackboard.cms.filesystem.CSFile;

public class FileWrapper implements Comparable<FileWrapper> {
	private int mLastModifedUserId;
	private String mLastModifedUser;
	private String mLastModifed;
	private CSFile mFileEntry;
	private boolean mVisible;
	private CSEntryMetadata mMetadata;
	
	/** Comparator to enable sorting by path for bbNG:inventoryList */
	private static GenericFieldComparator<FileWrapper> cmPath = 
			new GenericFieldComparator<FileWrapper>("getFilePath", FileWrapper.class);
	/** Comparator to enable sorting by creation time for bbNG:inventoryList */
	private static Comparator<FileWrapper> cmCreationTimestamp = 
			new Comparator<FileWrapper>() {
				public int compare(FileWrapper file1, FileWrapper file2) {
					return file1.getFileEntry().getCreationTimestamp().compareTo(file2.getFileEntry().getCreationTimestamp());
				}
			};

	public FileWrapper(CSFile csFile) {
		this(csFile, true);
	}
	
	public FileWrapper(CSFile csFile, boolean visible) {
		mFileEntry = csFile;
		mVisible = visible;
		mMetadata = csFile.getCSEntryMetadata();
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
	
	public String getMetadataAttribute(String attributeId) {
		return mMetadata.getStandardProperty(attributeId);
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
	
	public String getCreationTimestamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
		String ret = dateFormat.format(mFileEntry.getCreationTimestamp());
		return ret;
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
	 * @return the cmCreationTimestamp
	 */
	public Comparator<FileWrapper> getCmCreationTimestamp()
	{
		return cmCreationTimestamp;
	}

	/**
	 * @return the mFileEntry
	 */
	public CSFile getFileEntry()
	{
		return mFileEntry;
	}

	/**
	 * Note that this comparison MUST match the same sort used for bbNG:inventoryList
	 */
	@Override
	public int compareTo(FileWrapper arg0)
	{
		FileWrapper other = (FileWrapper) arg0;
		return getFilePath().compareToIgnoreCase(other.getFilePath());
	}
}
