package ca.ubc.ctlt.metadataeditor.inventoryList;

import java.util.HashMap;
import java.util.Iterator;

import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.session.BbSession;
import blackboard.servlet.helper.PagedListHelper;
import blackboard.servlet.tags.ngui.list.InventoryListTag;

public class MetadataInventoryListTag extends InventoryListTag {

	private static final long serialVersionUID = -4116969609228049505L;
	private static HashMap<BbSession, Integer> _startIndex = new HashMap<BbSession, Integer>();
	private BbSession _session;
	
	@Override
	public int doAfterBody() {
		PagedListHelper ph = getPagedListHelper();
		_startIndex.put(_session, ph.getStartIndex());
		return super.doAfterBody();
	}
	
	public static int getStaticStartIndex(BbSession session) {
		return _startIndex.get(session) != null ? _startIndex.get(session) : 0;
	}

	public void setSession(BbSession session) {
		//remove any old (unused) session the user might still have
		cleanSessions(ContextManagerFactory.getInstance().getContext().getUser().getUserName());
		this._session = session;
	}
	
	private void cleanSessions(String userName) {
		Iterator<BbSession> iter = _startIndex.keySet().iterator();
		while(iter.hasNext()) {
			BbSession sess = iter.next();
			if (sess.getUserName().equals(userName)) {
				iter.remove();
			}
		}
	}
}
