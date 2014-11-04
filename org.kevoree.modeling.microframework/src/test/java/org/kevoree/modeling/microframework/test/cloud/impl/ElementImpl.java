package org.kevoree.modeling.microframework.test.cloud.impl;

import org.kevoree.modeling.api.KDimension;
import org.kevoree.modeling.api.abs.AbstractKObject;
import org.kevoree.modeling.api.meta.MetaAttribute;
import org.kevoree.modeling.api.meta.MetaClass;
import org.kevoree.modeling.api.meta.MetaOperation;
import org.kevoree.modeling.api.meta.MetaReference;
import org.kevoree.modeling.api.time.TimeTree;
import org.kevoree.modeling.microframework.test.cloud.CloudView;
import org.kevoree.modeling.microframework.test.cloud.Element;

/**
 * Created by duke on 10/13/14.
 */
public class ElementImpl extends AbstractKObject<Element, CloudView> implements Element {

    public ElementImpl(CloudView factory, MetaClass metaClass, long kid, Long now, KDimension dimension, TimeTree timeTree) {
        super(factory, metaClass, kid, now, dimension, timeTree);
    }

    @Override
    public MetaAttribute[] metaAttributes() {
        return Element.METAATTRIBUTES.values();
    }

    private final MetaReference[] _mataReferences = new MetaReference[0];

    @Override
    public MetaReference[] metaReferences() {
        return _mataReferences;
    }

    @Override
    public MetaOperation[] metaOperations() {
        return new MetaOperation[0];
    }

    @Override
    public String getName() {
        return (String) get(METAATTRIBUTES.NAME);
    }

    @Override
    public Element setName(String name) {
        set(METAATTRIBUTES.NAME, name);
        return this;
    }

    @Override
    public Long getValue() {
        return (Long) get(METAATTRIBUTES.VALUE);
    }

    @Override
    public Element setValue(Long name) {
        set(METAATTRIBUTES.VALUE, name);
        return this;
    }


}
