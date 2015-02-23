package org.kevoree.modeling.api;

import org.kevoree.modeling.api.data.cdn.KContentDeliveryDriver;
import org.kevoree.modeling.api.data.manager.KDataManager;
import org.kevoree.modeling.api.meta.MetaModel;
import org.kevoree.modeling.api.meta.MetaOperation;

/**
 * Created by duke on 9/30/14.
 */

public interface KModel<A extends KUniverse> {

    public A newUniverse();

    public A universe(long key);

    public KDataManager manager();

    public void disable(KEventListener listener);

    public void listen(KEventListener listener);

    public KModel<A> setContentDeliveryDriver(KContentDeliveryDriver dataBase);

    public KModel<A> setScheduler(KScheduler scheduler);

    public void setOperation(MetaOperation metaOperation, KOperation operation);

    public void setInstanceOperation(MetaOperation metaOperation, KObject target, KOperation operation);

    public MetaModel metaModel();

    public KTask task();

    public KTask<Throwable> save();

    public KTask<Throwable> discard();

    public KTask<Throwable> connect();

    public KTask<Throwable> close();

}