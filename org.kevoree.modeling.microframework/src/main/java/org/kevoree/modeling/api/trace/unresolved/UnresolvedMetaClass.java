package org.kevoree.modeling.api.trace.unresolved;

import org.kevoree.modeling.api.meta.MetaClass;

/**
 * Created by duke on 10/16/14.
 */
public class UnresolvedMetaClass implements MetaClass {

    private String metaName;

    public UnresolvedMetaClass(String metaName) {
        this.metaName = metaName;
    }

    @Override
    public String metaName() {
        return metaName;
    }

    @Override
    public int index() {
        return -1;
    }
}
