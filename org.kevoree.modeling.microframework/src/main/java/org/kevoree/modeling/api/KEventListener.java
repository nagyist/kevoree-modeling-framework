package org.kevoree.modeling.api;

import org.kevoree.modeling.api.meta.Meta;

public interface KEventListener {
    public void on(KObject src, Meta[] modifications);
}
