package org.kevoree.modeling.api.data.cdn;

import org.kevoree.modeling.api.*;
import org.kevoree.modeling.api.data.cache.*;
import org.kevoree.modeling.api.data.manager.KDataManager;
import org.kevoree.modeling.api.event.LocalEventListeners;
import org.kevoree.modeling.api.msg.KMessage;

import java.util.HashMap;

public class MemoryKContentDeliveryDriver implements KContentDeliveryDriver {

    private final HashMap<String, String> backend = new HashMap<String, String>();
    private LocalEventListeners _localEventListeners = new LocalEventListeners();

    public static boolean DEBUG = false;

    @Override
    public void atomicGetMutate(KContentKey key, AtomicOperation operation, ThrowableCallback<String> callback) {
        String result = backend.get(key.toString());
        String mutated = operation.mutate(result);
        if (DEBUG) {
            System.out.println("ATOMIC GET " + key + "->" + result);
            System.out.println("ATOMIC PUT " + key + "->" + mutated);
        }
        backend.put(key.toString(), mutated);
        callback.on(result, null);
    }

    @Override
    public void get(KContentKey[] keys, ThrowableCallback<String[]> callback) {
        String[] values = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                values[i] = backend.get(keys[i].toString());
            }
            if (DEBUG) {
                System.out.println("GET " + keys[i] + "->" + values[i]);
            }
        }
        if (callback != null) {
            callback.on(values, null);
        }
    }

    @Override
    public synchronized void put(KContentPutRequest p_request, Callback<Throwable> p_callback) {
        for (int i = 0; i < p_request.size(); i++) {
            backend.put(p_request.getKey(i).toString(), p_request.getContent(i));
            if (DEBUG) {
                System.out.println("PUT " + p_request.getKey(i).toString() + "->" + p_request.getContent(i));
            }
        }
        if (p_callback != null) {
            p_callback.on(null);
        }
    }

    @Override
    public void remove(String[] keys, Callback<Throwable> callback) {
        for (int i = 0; i < keys.length; i++) {
            backend.remove(keys[i]);
        }
        if (callback != null) {
            callback.on(null);
        }
    }

    @Override
    public void connect(Callback<Throwable> callback) {
        if (callback != null) {
            callback.on(null);
        }
    }

    @Override
    public void close(Callback<Throwable> callback) {
        _localEventListeners.clear();
        backend.clear();
    }


    @Override
    public void registerListener(long groupId, KObject p_origin, KEventListener p_listener) {
        _localEventListeners.registerListener(groupId, p_origin, p_listener);
    }

    @Override
    public void unregisterGroup(long groupId) {
        _localEventListeners.unregister(groupId);
    }

    @Override
    public void registerMultiListener(long groupId, KUniverse origin, long[] objects, KEventMultiListener listener) {
        _localEventListeners.registerListenerAll(groupId, origin, objects, listener);
    }

    @Override
    public void send(KMessage msgs) {
        //NO REMOTE MANAGEMENT
        _localEventListeners.dispatch(msgs);
    }

    @Override
    public void setManager(KDataManager manager) {
        _localEventListeners.setManager(manager);
    }


}