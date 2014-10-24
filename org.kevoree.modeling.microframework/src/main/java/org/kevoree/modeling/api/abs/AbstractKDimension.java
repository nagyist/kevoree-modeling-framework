package org.kevoree.modeling.api.abs;

import org.kevoree.modeling.api.Callback;
import org.kevoree.modeling.api.KDimension;
import org.kevoree.modeling.api.KUniverse;
import org.kevoree.modeling.api.KView;
import org.kevoree.modeling.api.time.TimeTree;
import org.kevoree.modeling.api.time.impl.DefaultTimeTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by duke on 10/10/14.
 */
public abstract class AbstractKDimension<A extends KView, B extends KDimension, C extends KUniverse> implements KDimension<A, B, C> {

    private KUniverse universe;

    private long key;

    protected AbstractKDimension(KUniverse universe, long key) {
        this.universe = universe;
        this.key = key;
    }

    @Override
    public long key() {
        return key;
    }

    @Override
    public C universe() {
        return (C) universe;
    }

    @Override
    public void save(Callback<Throwable> callback) {
        universe().storage().save(this, callback);
    }

    @Override
    public void saveUnload(Callback<Throwable> callback) {
        universe().storage().saveUnload(this, callback);
    }

    @Override
    public void delete(Callback<Throwable> callback) {
        universe().storage().delete(this, callback);
    }

    @Override
    public void discard(Callback<Throwable> callback) {
        universe().storage().discard(this, callback);
    }

    public void timeTrees(long[] keys, Callback<TimeTree[]> callback) {
        universe().storage().timeTrees(this, keys, callback);
    }

    public void timeTree(long key, Callback<TimeTree> callback) {
        universe().storage().timeTree(this, key, callback);
    }


    @Override
    public void parent(Callback<B> callback) {

    }

    @Override
    public void children(Callback<Set<B>> callback) {

    }

    @Override
    public void fork(Callback<B> callback) {

    }

    private Map<Long, A> timesCache = new HashMap<Long, A>();

    @Override
    public synchronized A time(Long timePoint) {
        if (timesCache.containsKey(timePoint)) {
            return timesCache.get(timePoint);
        } else {
            A newCreatedTime = internal_create(timePoint);
            timesCache.put(timePoint, newCreatedTime);
            return newCreatedTime;
        }
    }

    protected abstract A internal_create(Long timePoint);

    private final TimeTree rootTimeTree = new DefaultTimeTree();

    public TimeTree getRootTimeTree() {
        return rootTimeTree;
    }

}
