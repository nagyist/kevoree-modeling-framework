package org.kevoree.modeling.databases.websocket.test;

import org.kevoree.modeling.*;
import org.kevoree.modeling.memory.KContentKey;
import org.kevoree.modeling.memory.KContentDeliveryDriver;
import org.kevoree.modeling.memory.cdn.KContentPutRequest;
import org.kevoree.modeling.memory.KDataManager;
import org.kevoree.modeling.msg.KMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by duke on 10/03/15.
 */
public class KContentDeliveryDriverMock implements KContentDeliveryDriver {

    public HashMap<String, String> alreadyPut = new HashMap<String, String>();

    @Override
    public void atomicGetIncrement(KContentKey key, ThrowableCallback<Short> callback) {
        callback.on(Short.parseShort("0"), null);
    }

    @Override
    public void get(KContentKey[] keys, ThrowableCallback<String[]> callback) {
        String[] values = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            values[i] = keys[i].toString();
        }
        callback.on(values, null);
    }

    @Override
    public void put(KContentPutRequest request, Callback<Throwable> error) {
        for (int i = 0; i < request.size(); i++) {
            alreadyPut.put(request.getKey(i).toString(), request.getContent(i));
        }
        error.on(null);
    }

    @Override
    public void remove(String[] keys, Callback<Throwable> error) {

    }

    @Override
    public void connect(Callback<Throwable> callback) {
        callback.on(null);
    }

    @Override
    public void close(Callback<Throwable> callback) {

    }

    @Override
    public void registerListener(long groupId, KObject origin, KEventListener listener) {

    }

    @Override
    public void registerMultiListener(long groupId, KUniverse origin, long[] objects, KEventMultiListener listener) {

    }

    @Override
    public void unregisterGroup(long groupId) {

    }

    public ArrayList<KMessage> recMessages = new ArrayList<KMessage>();

    public CountDownLatch msgCounter = new CountDownLatch(1);

    @Override
    public void send(KMessage msgs) {
        recMessages.add(msgs);
        msgCounter.countDown();
    }

    @Override
    public void setManager(KDataManager manager) {

    }
}
