package org.kevoree.modeling.api;

import org.kevoree.modeling.api.data.KStore;
import org.kevoree.modeling.api.events.ModelElementListener;

/**
 * Created by duke on 9/30/14.
 */

public interface KUniverse<A extends KDimension> {

    public A create();

    public A get(long key);

    public void saveAll(Callback<Boolean> callback);

    public void deleteAll(Callback<Boolean> callback);

    public void unloadAll(Callback<Boolean> callback);

    public void listen(ModelElementListener listener, Long from, Long to, String path);

    public void disable(ModelElementListener listener);

    public void stream(String query, Callback<KObject> callback);

    public KStore storage();

}