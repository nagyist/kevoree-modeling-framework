package org.kevoree.modeling.api.data.manager;

import org.kevoree.modeling.api.KConfig;
import org.kevoree.modeling.api.map.LongLongHashMap;
import org.kevoree.modeling.api.map.LongLongHashMapCallBack;
import org.kevoree.modeling.api.rbtree.LongRBTree;
import org.kevoree.modeling.api.rbtree.LongTreeNode;

/**
 * Created by duke on 03/03/15.
 */
public class ResolutionHelper {

    public static long resolve_universe(LongLongHashMap globalTree, LongLongHashMap objUniverseTree, long timeToResolve, long universeId) {
        if (globalTree == null) {
            return universeId;
        }
        long currentUniverseValue = globalTree.get(universeId);
        long currentUniverseKey = universeId;
        if (currentUniverseValue == KConfig.NULL_LONG) {
            return universeId;
        }
        long resolvedKey = universeId;
        long resolvedValue = objUniverseTree.get(resolvedKey);
        while (resolvedValue == KConfig.NULL_LONG && currentUniverseKey != currentUniverseValue) {
            currentUniverseKey = currentUniverseValue;
            currentUniverseValue = globalTree.get(currentUniverseKey);
            resolvedKey = currentUniverseKey;
            resolvedValue = objUniverseTree.get(resolvedKey);
        }
        if (resolvedValue == KConfig.NULL_LONG) {
            return universeId;
        }
        while (resolvedValue != KConfig.NULL_LONG && resolvedValue > timeToResolve && resolvedKey != resolvedValue) {
            long resolvedCurrentKey = resolvedKey;
            long resolvedCurrentValue = globalTree.get(resolvedCurrentKey);
            resolvedKey = resolvedCurrentValue;
            resolvedValue = objUniverseTree.get(resolvedKey);
            while (resolvedValue == KConfig.NULL_LONG && resolvedCurrentKey != resolvedCurrentValue) {
                resolvedKey = resolvedCurrentValue;
                resolvedValue = objUniverseTree.get(resolvedCurrentValue);
                resolvedCurrentKey = resolvedCurrentValue;
                resolvedCurrentValue = globalTree.get(resolvedCurrentValue);
            }
            if (resolvedCurrentKey == resolvedCurrentValue) {
                break;
            }
        }
        if (resolvedValue != KConfig.NULL_LONG) {
            return resolvedKey;
        } else {
            return universeId;
        }
    }

    public static long[] universeSelectByRange(LongLongHashMap globalTree, LongLongHashMap objUniverseTree, long rangeMin, long rangeMax, long originUniverseId) {
        LongLongHashMap collected = new LongLongHashMap(KConfig.CACHE_INIT_SIZE, KConfig.CACHE_LOAD_FACTOR);
        long currentUniverse = originUniverseId;
        long previousUniverse = KConfig.NULL_LONG;
        long divergenceTime = objUniverseTree.get(currentUniverse);
        while (currentUniverse != previousUniverse) {
            //check range
            if (divergenceTime != KConfig.NULL_LONG) {
                if (divergenceTime <= rangeMin) {
                    collected.put(collected.size(), currentUniverse);
                    break;
                } else if (divergenceTime <= rangeMax) {
                    collected.put(collected.size(), currentUniverse);
                }
            }
            //next round
            previousUniverse = currentUniverse;
            currentUniverse = globalTree.get(currentUniverse);
            divergenceTime = objUniverseTree.get(currentUniverse);
        }
        long[] trimmed = new long[collected.size()];
        for(long i=0;i<collected.size();i++){
            trimmed[(int)i] = collected.get(i);
        }
        return trimmed;
    }

}
