package org.kevoree.modeling.api;

import org.kevoree.modeling.api.meta.MetaClass;
import org.kevoree.modeling.api.time.TimeTree;
import org.kevoree.modeling.api.trace.TraceSequence;

import java.util.List;

/**
 * Created by thomas on 10/2/14.
 */
public interface KView {

    public KObject createFQN(String metaClassName);

    public KObject create(MetaClass clazz);

    public void setRoot(KObject elem);
    
    public void select(String query, Callback<KObject[]> callback);

    public void lookup(Long key, Callback<KObject> callback);

    public void lookupAll(Long[] keys, Callback<KObject[]> callback);

    public void stream(String query, Callback<KObject> callback);

    public MetaClass[] metaClasses();

    public MetaClass metaClass(String fqName);

    public KDimension dimension();

    public long now();

    public KObject createProxy(MetaClass clazz, TimeTree timeTree, long key);

    public void listen(ModelListener listener);

    public void diff(KObject origin, KObject target, Callback<TraceSequence> callback);

    public void merge(KObject origin, KObject target, Callback<TraceSequence> callback);

    public void intersection(KObject origin, KObject target, Callback<TraceSequence> callback);

    public void slice(List<KObject> elems, Callback<TraceSequence> callback);

    public <A extends KObject> void clone(A o, Callback<A> callback);

    public ModelFormat json();

    public ModelFormat xmi();

}
