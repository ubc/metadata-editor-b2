package ca.ubc.ctlt.metadataeditor;

import blackboard.base.BbList;
import blackboard.persist.PersistenceException;
import blackboard.persist.impl.NewBaseDbLoader;
import blackboard.persist.impl.SimpleSelectQuery;
import blackboard.persist.role.impl.PortalRoleDbMap;

public class MetadataChangeDbLoaderImpl extends NewBaseDbLoader {
	public BbList loadAll() throws PersistenceException {
		BbList value = null;
        SimpleSelectQuery query = new SimpleSelectQuery(PortalRoleDbMap.MAP);
        query.addWhere("RowStatus", Integer.valueOf(0));
        value = super.loadList(query, null);
        return value;
	}

}
