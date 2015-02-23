package org.kevoree.modeling.api.abs;

import org.kevoree.modeling.api.Callback;
import org.kevoree.modeling.api.KActionType;
import org.kevoree.modeling.api.KInfer;
import org.kevoree.modeling.api.KObject;
import org.kevoree.modeling.api.KTask;
import org.kevoree.modeling.api.KUniverse;
import org.kevoree.modeling.api.KView;
import org.kevoree.modeling.api.ModelAttributeVisitor;
import org.kevoree.modeling.api.KEventListener;
import org.kevoree.modeling.api.ModelVisitor;
import org.kevoree.modeling.api.TraceRequest;
import org.kevoree.modeling.api.VisitRequest;
import org.kevoree.modeling.api.VisitResult;
import org.kevoree.modeling.api.data.cache.KCacheEntry;
import org.kevoree.modeling.api.data.manager.AccessMode;
import org.kevoree.modeling.api.data.manager.Index;
import org.kevoree.modeling.api.data.manager.JsonRaw;
import org.kevoree.modeling.api.meta.MetaAttribute;
import org.kevoree.modeling.api.meta.MetaClass;
import org.kevoree.modeling.api.meta.MetaOperation;
import org.kevoree.modeling.api.meta.MetaReference;
import org.kevoree.modeling.api.operation.DefaultModelCompare;
import org.kevoree.modeling.api.time.rbtree.LongRBTree;
import org.kevoree.modeling.api.traversal.DefaultKTraversal;
import org.kevoree.modeling.api.traversal.KTraversal;
import org.kevoree.modeling.api.traversal.actions.KParentsAction;
import org.kevoree.modeling.api.traversal.actions.KReverseQueryAction;
import org.kevoree.modeling.api.traversal.actions.KTraverseAction;
import org.kevoree.modeling.api.traversal.actions.KTraverseQueryAction;
import org.kevoree.modeling.api.traversal.selector.KSelector;
import org.kevoree.modeling.api.trace.ModelAddTrace;
import org.kevoree.modeling.api.trace.ModelSetTrace;
import org.kevoree.modeling.api.trace.ModelTrace;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.kevoree.modeling.api.util.Checker;
import org.kevoree.modeling.api.util.PathHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by duke on 10/9/14.
 */
public abstract class AbstractKObject implements KObject {

    private KView _view;
    private long _uuid;
    private LongRBTree _universeTree;
    private MetaClass _metaClass;

    public AbstractKObject(KView p_view, long p_uuid, LongRBTree p_universeTree, MetaClass p_metaClass) {
        this._view = p_view;
        this._uuid = p_uuid;
        this._metaClass = p_metaClass;
        this._universeTree = p_universeTree;
    }

    @Override
    public KView view() {
        return _view;
    }

    @Override
    public long uuid() {
        return _uuid;
    }

    @Override
    public MetaClass metaClass() {
        return _metaClass;
    }

    @Override
    public long now() {
        return _view.now();
    }

    @Override
    public LongRBTree universeTree() {
        return _universeTree;
    }

    @Override
    public KUniverse universe() {
        return _view.universe();
    }

    @Override
    public void path(final Callback<String> callback) {
        if (!Checker.isDefined(callback)) {
            return;
        }
        parent(new Callback<KObject>() {
            @Override
            public void on(KObject parent) {
                if (parent == null) {
                    callback.on("/");
                } else {
                    parent.path(new Callback<String>() {
                        @Override
                        public void on(String parentPath) {
                            callback.on(PathHelper.path(parentPath, referenceInParent(), AbstractKObject.this));
                        }
                    });
                }
            }
        });
    }

    @Override
    public Long parentUuid() {
        KCacheEntry raw = _view.universe().model().manager().entry(this, AccessMode.READ);
        if (raw == null) {
            return null;
        } else {
            return (Long) raw.get(Index.PARENT_INDEX);
        }
    }

    @Override
    public void parent(Callback<KObject> callback) {
        if (!Checker.isDefined(callback)) {
            return;
        }
        Long parentKID = parentUuid();
        if (parentKID == null) {
            callback.on(null);
        } else {
            _view.lookup(parentKID, callback);
        }
    }

    @Override
    public MetaReference referenceInParent() {
        KCacheEntry raw = _view.universe().model().manager().entry(this, AccessMode.READ);
        if (raw == null) {
            return null;
        } else {
            return (MetaReference) raw.get(Index.REF_IN_PARENT_INDEX);
        }
    }

    @Override
    public void delete(Callback<Throwable> callback) {
        KObject toRemove = this;
        KCacheEntry rawPayload = _view.universe().model().manager().entry(this, AccessMode.DELETE);
        if (rawPayload == null) {
            callback.on(new Exception("Out of cache Error"));
        } else {
            Object payload = rawPayload.get(Index.INBOUNDS_INDEX);
            if (payload != null) {
                Set<Long> inboundsKeys = (Set<Long>) payload;
                Long[] refArr = inboundsKeys.toArray(new Long[inboundsKeys.size()]);
                view().lookupAll(refArr, new Callback<KObject[]>() {
                    @Override
                    public void on(KObject[] resolved) {
                        for (int i = 0; i < resolved.length; i++) {
                            if (resolved[i] != null) {
                                MetaReference[] linkedReferences = resolved[i].referencesWith(toRemove);
                                for (int j = 0; j < linkedReferences.length; j++) {
                                    ((AbstractKObject) resolved[i]).internal_mutate(KActionType.REMOVE, linkedReferences[j], toRemove, false, true);
                                }
                            }
                        }
                        if (callback != null) {
                            callback.on(null);
                        }
                    }
                });
            } else {
                callback.on(new Exception("Out of cache error"));
            }
        }
    }

    @Override
    public void select(String query, Callback<KObject[]> callback) {
        if (!Checker.isDefined(callback)) {
            return;
        }
        if (!Checker.isDefined(query)) {
            callback.on(new KObject[0]);
            return;
        }
        String cleanedQuery = query;
        if (cleanedQuery.startsWith("/")) {
            cleanedQuery = cleanedQuery.substring(1);
        }
        if (query.startsWith("/")) {
            final String finalCleanedQuery = cleanedQuery;
            universe().model().manager().getRoot(this.view(), new Callback<KObject>() {
                @Override
                public void on(KObject rootObj) {
                    if (rootObj == null) {
                        callback.on(new KObject[0]);
                    } else {
                        KSelector.select(rootObj, finalCleanedQuery, callback);
                    }
                }
            });
        } else {
            KSelector.select(this, query, callback);
        }
    }

    @Override
    public void listen(KEventListener listener) {
        universe().model().manager().cdn().registerListener(this, listener, null);
    }

    @Override
    public String domainKey() {
        StringBuilder builder = new StringBuilder();
        MetaAttribute[] atts = metaClass().metaAttributes();
        for (int i = 0; i < atts.length; i++) {
            MetaAttribute att = atts[i];
            if (att.key()) {
                if (builder.length() != 0) {
                    builder.append(",");
                }
                builder.append(att.metaName());
                builder.append("=");
                Object payload = get(att);
                if (payload != null) {
                    //TODO, forbid multiple cardinality as domainKey
                    builder.append(payload.toString());
                }
            }
        }
        String result = builder.toString();
        if (result.equals("")) {
            return uuid() + "";
        } else {
            return result;
        }
    }

    @Override
    public Object get(MetaAttribute p_attribute) {
        MetaAttribute transposed = internal_transpose_att(p_attribute);
        if (transposed == null) {
            throw new RuntimeException("Bad KMF usage, the attribute named " + p_attribute.metaName() + " is not part of " + metaClass().metaName());
        } else {
            return transposed.strategy().extrapolate(this, transposed);
        }
    }

    @Override
    public void set(MetaAttribute p_attribute, Object payload) {
        MetaAttribute transposed = internal_transpose_att(p_attribute);
        if (transposed == null) {
            throw new RuntimeException("Bad KMF usage, the attribute named " + p_attribute.metaName() + " is not part of " + metaClass().metaName());
        } else {
            transposed.strategy().mutate(this, transposed, payload);
        }
    }

    private void removeFromContainer(final KObject param) {
        if (param != null && param.parentUuid() != null && param.parentUuid() != _uuid) {
            view().lookup(param.parentUuid(), new Callback<KObject>() {
                @Override
                public void on(KObject parent) {
                    ((AbstractKObject) parent).internal_mutate(KActionType.REMOVE, param.referenceInParent(), param, true, false);
                }
            });
        }
    }

    @Override
    public void mutate(KActionType actionType, final MetaReference metaReference, KObject param) {
        internal_mutate(actionType, metaReference, param, true, false);
    }

    public void internal_mutate(KActionType actionType, final MetaReference metaReferenceP, KObject param, final boolean setOpposite, final boolean inDelete) {
        MetaReference metaReference = internal_transpose_ref(metaReferenceP);
        if (metaReference == null) {
            if (metaReferenceP == null) {
                throw new RuntimeException("Bad KMF usage, the reference " + " is null in metaClass named " + metaClass().metaName());
            } else {
                throw new RuntimeException("Bad KMF usage, the reference named " + metaReferenceP.metaName() + " is not part of " + metaClass().metaName());
            }
        }
        if (actionType.equals(KActionType.ADD)) {
            if (metaReference.single()) {
                internal_mutate(KActionType.SET, metaReference, param, setOpposite, inDelete);
            } else {
                KCacheEntry raw = view().universe().model().manager().entry(this, AccessMode.WRITE);
                Set<Long> previousList;
                if (raw.get(metaReference.index()) != null && raw.get(metaReference.index()) instanceof Set) {
                    previousList = (Set<Long>) raw.get(metaReference.index());
                } else {
                    if (raw.get(metaReference.index()) != null) {
                        try {
                            throw new Exception("Bad cache values in KMF, " + raw.get(metaReference.index()) + " is not an instance of Set for the reference " + metaReference.metaName() + ", index:" + metaReference.index());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    previousList = new HashSet<Long>();
                    raw.set(metaReference.index(), previousList);
                }
                //Actual add
                previousList.add(param.uuid());
                //Opposite
                if (metaReference.opposite() != null && setOpposite) {
                    ((AbstractKObject) param).internal_mutate(KActionType.ADD, metaReference.opposite(), this, false, inDelete);
                }
                //Container
                if (metaReference.contained()) {
                    removeFromContainer(param);
                    ((AbstractKObject) param).set_parent(_uuid, metaReference);
                }
                //Inbound
                KCacheEntry rawParam = view().universe().model().manager().entry(param, AccessMode.WRITE);
                Set<Long> previousInbounds;
                if (rawParam.get(Index.INBOUNDS_INDEX) != null && rawParam.get(Index.INBOUNDS_INDEX) instanceof Set) {
                    previousInbounds = (Set<Long>) rawParam.get(Index.INBOUNDS_INDEX);
                } else {
                    if (rawParam.get(Index.INBOUNDS_INDEX) != null) {
                        try {
                            throw new Exception("Bad cache values in KMF, " + rawParam.get(Index.INBOUNDS_INDEX) + " is not an instance of Set for the INBOUNDS manager");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    previousInbounds = new HashSet<Long>();
                    rawParam.set(Index.INBOUNDS_INDEX, previousInbounds);
                }
                previousInbounds.add(uuid());
            }
        } else if (actionType.equals(KActionType.SET)) {
            if (!metaReference.single()) {
                internal_mutate(KActionType.ADD, metaReference, param, setOpposite, inDelete);
            } else {
                if (param == null) {
                    internal_mutate(KActionType.REMOVE, metaReference, null, setOpposite, inDelete);
                } else {
                    //Actual add
                    KCacheEntry payload = view().universe().model().manager().entry(this, AccessMode.WRITE);
                    Object previous = payload.get(metaReference.index());
                    if (previous != null) {
                        internal_mutate(KActionType.REMOVE, metaReference, null, setOpposite, inDelete);
                    }
                    payload.set(metaReference.index(), param.uuid());
                    //Container
                    if (metaReference.contained()) {
                        removeFromContainer(param);
                        ((AbstractKObject) param).set_parent(_uuid, metaReference);
                    }
                    //Inbound
                    KCacheEntry rawParam = view().universe().model().manager().entry(param, AccessMode.WRITE);
                    Set<Long> previousInbounds;
                    if (rawParam.get(Index.INBOUNDS_INDEX) != null && rawParam.get(Index.INBOUNDS_INDEX) instanceof Set) {
                        previousInbounds = (Set<Long>) rawParam.get(Index.INBOUNDS_INDEX);
                    } else {
                        if (rawParam.get(Index.INBOUNDS_INDEX) != null) {
                            try {
                                throw new Exception("Bad cache values in KMF, " + rawParam.get(Index.INBOUNDS_INDEX) + " is not an instance of Set for the INBOUNDS manager");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        previousInbounds = new HashSet<Long>();
                        rawParam.set(Index.INBOUNDS_INDEX, previousInbounds);
                    }
                    previousInbounds.add(uuid());
                    //Opposite
                    final KObject self = this;
                    if (metaReference.opposite() != null && setOpposite) {
                        if (previous != null) {
                            view().lookup((Long) previous, new Callback<KObject>() {
                                @Override
                                public void on(KObject resolved) {
                                    ((AbstractKObject) resolved).internal_mutate(KActionType.REMOVE, metaReference.opposite(), self, false, inDelete);
                                }
                            });
                        }
                        ((AbstractKObject) param).internal_mutate(KActionType.ADD, metaReference.opposite(), this, false, inDelete);
                    }
                }
            }
        } else if (actionType.equals(KActionType.REMOVE)) {
            if (metaReference.single()) {
                KCacheEntry raw = view().universe().model().manager().entry(this, AccessMode.WRITE);
                Object previousKid = raw.get(metaReference.index());
                raw.set(metaReference.index(), null);
                if (previousKid != null) {
                    final KObject self = this;
                    _view.universe().model().manager().lookup(_view, (Long) previousKid, new Callback<KObject>() {
                        @Override
                        public void on(KObject resolvedParam) {
                            if (resolvedParam != null) {
                                if (metaReference.contained()) {
                                    ((AbstractKObject) resolvedParam).set_parent(null, null);
                                }
                                if (metaReference.opposite() != null && setOpposite) {
                                    ((AbstractKObject) resolvedParam).internal_mutate(KActionType.REMOVE, metaReference.opposite(), self, false, inDelete);
                                }
                                //Inbound
                                KCacheEntry rawParam = view().universe().model().manager().entry(resolvedParam, AccessMode.WRITE);
                                Set<Long> previousInbounds = null;
                                if (rawParam != null && rawParam.get(Index.INBOUNDS_INDEX) != null && rawParam.get(Index.INBOUNDS_INDEX) instanceof Set) {
                                    previousInbounds = (Set<Long>) rawParam.get(Index.INBOUNDS_INDEX);
                                } else {
                                    if (rawParam != null) {
                                        if (rawParam.get(Index.INBOUNDS_INDEX) != null) {
                                            try {
                                                throw new Exception("Bad cache values in KMF, " + rawParam.get(Index.INBOUNDS_INDEX) + " is not an instance of Set for the INBOUNDS manager");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        previousInbounds = new HashSet<Long>();
                                        rawParam.set(Index.INBOUNDS_INDEX, previousInbounds);
                                    }
                                }
                                if (previousInbounds != null) {
                                    previousInbounds.remove(_uuid);
                                }
                            }
                        }
                    });
                }
            } else {
                KCacheEntry payload = view().universe().model().manager().entry(this, AccessMode.WRITE);
                Object previous = payload.get(metaReference.index());
                if (previous != null) {
                    Set<Long> previousList = (Set<Long>) previous;
                    previousList.remove(param.uuid());
                    if (!inDelete && metaReference.contained()) {
                        ((AbstractKObject) param).set_parent(null, null);
                    }
                    if (metaReference.opposite() != null && setOpposite) {
                        ((AbstractKObject) param).internal_mutate(KActionType.REMOVE, metaReference.opposite(), this, false, inDelete);
                    }
                }
                //Inbounds
                if (!inDelete) {
                    //Inbound
                    KCacheEntry rawParam = view().universe().model().manager().entry(param, AccessMode.WRITE);
                    if (rawParam != null && rawParam.get(Index.INBOUNDS_INDEX) != null && rawParam.get(Index.INBOUNDS_INDEX) instanceof Set) {
                        Set<Long> previousInbounds = (Set<Long>) rawParam.get(Index.INBOUNDS_INDEX);
                        previousInbounds.remove(_uuid);
                    }
                }
            }
        }
    }

    public int size(MetaReference p_metaReference) {
        MetaReference transposed = internal_transpose_ref(p_metaReference);
        if (transposed == null) {
            throw new RuntimeException("Bad KMF usage, the attribute named " + p_metaReference.metaName() + " is not part of " + metaClass().metaName());
        } else {
            KCacheEntry raw = view().universe().model().manager().entry(this, AccessMode.READ);
            if (raw != null) {
                Object ref = raw.get(transposed.index());
                if (ref == null) {
                    return 0;
                } else {
                    Set<Object> refSet = (Set<Object>) ref;
                    return refSet.size();
                }
            } else {
                return 0;
            }
        }
    }

    public void ref(MetaReference p_metaReference, final Callback<KObject[]> p_callback) {
        if (!Checker.isDefined(p_callback)) {
            return;
        }
        MetaReference transposed = internal_transpose_ref(p_metaReference);
        if (transposed == null) {
            throw new RuntimeException("Bad KMF usage, the reference named " + p_metaReference.metaName() + " is not part of " + metaClass().metaName());
        } else {
            KCacheEntry raw = view().universe().model().manager().entry(this, AccessMode.READ);
            if (raw == null) {
                p_callback.on(new KObject[0]);
            } else {
                Object o = raw.get(transposed.index());
                if (o == null) {
                    p_callback.on(new KObject[0]);
                } else if (o instanceof Set) {
                    Set<Long> objs = (Set<Long>) o;
                    Long[] setContent = objs.toArray(new Long[objs.size()]);
                    view().lookupAll(setContent, new Callback<KObject[]>() {
                        @Override
                        public void on(KObject[] result) {
                            try {
                                p_callback.on(result);
                            } catch (Throwable t) {
                                t.printStackTrace();
                                p_callback.on(null);
                            }
                        }
                    });
                } else {
                    Long[] content = new Long[]{(Long) o};
                    view().lookupAll(content, new Callback<KObject[]>() {
                        @Override
                        public void on(KObject[] result) {
                            try {
                                p_callback.on(result);
                            } catch (Throwable t) {
                                t.printStackTrace();
                                p_callback.on(null);
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void inferRef(MetaReference p_metaReference, Callback<KObject[]> p_callback) {
        //TODO
    }

    @Override
    public void visitAttributes(ModelAttributeVisitor visitor) {
        if (!Checker.isDefined(visitor)) {
            return;
        }
        MetaAttribute[] metaAttributes = metaClass().metaAttributes();
        for (int i = 0; i < metaAttributes.length; i++) {
            visitor.visit(metaAttributes[i], get(metaAttributes[i]));
        }
    }

    public void visit(ModelVisitor p_visitor, Callback<Throwable> p_end, VisitRequest p_request) {
        if (p_request.equals(VisitRequest.CHILDREN)) {
            internal_visit(p_visitor, p_end, false, false, null, null);
        } else if (p_request.equals(VisitRequest.ALL)) {
            internal_visit(p_visitor, p_end, true, false, new HashSet<Long>(), new HashSet<Long>());
        } else if (p_request.equals(VisitRequest.CONTAINED)) {
            internal_visit(p_visitor, p_end, true, true, null, null);
        }
    }

    private void internal_visit(final ModelVisitor visitor, final Callback<Throwable> end, boolean deep, boolean containedOnly, final HashSet<Long> visited, final HashSet<Long> traversed) {
        if (!Checker.isDefined(visitor)) {
            return;
        }
        if (traversed != null) {
            traversed.add(_uuid);
        }
        final Set<Long> toResolveIds = new HashSet<Long>();
        for (int i = 0; i < metaClass().metaReferences().length; i++) {
            final MetaReference reference = metaClass().metaReferences()[i];
            if (!(containedOnly && !reference.contained())) {
                KCacheEntry raw = view().universe().model().manager().entry(this, AccessMode.READ);
                Object obj = null;
                if (raw != null) {
                    obj = raw.get(reference.index());
                }
                if (obj != null) {
                    if (obj instanceof Set) {
                        Set<Long> ids = (Set<Long>) obj;
                        Long[] idArr = ids.toArray(new Long[ids.size()]);
                        for (int k = 0; k < idArr.length; k++) {
                            if (traversed == null || !traversed.contains(idArr[k])) { // this is for optimization
                                toResolveIds.add(idArr[k]);
                            }
                        }
                    } else {
                        if (traversed == null || !traversed.contains(obj)) { // this is for optimization
                            toResolveIds.add((Long) obj);
                        }
                    }
                }
            }
        }
        if (toResolveIds.isEmpty()) {
            if (Checker.isDefined(end)) {
                end.on(null);
            }
        } else {
            final Long[] toResolveIdsArr = toResolveIds.toArray(new Long[toResolveIds.size()]);
            view().lookupAll(toResolveIdsArr, new Callback<KObject[]>() {
                @Override
                public void on(KObject[] resolvedArr) {
                    final List<KObject> nextDeep = new ArrayList<KObject>();

                    for (int i = 0; i < resolvedArr.length; i++) {
                        final KObject resolved = resolvedArr[i];
                        VisitResult result = VisitResult.CONTINUE;
                        if (visitor != null && (visited == null || !visited.contains(resolved.uuid()))) {
                            result = visitor.visit(resolved);
                        }
                        if (visited != null) {
                            visited.add(resolved.uuid());
                        }
                        if (result != null && result.equals(VisitResult.STOP)) {
                            if (Checker.isDefined(end)) {
                                end.on(null);
                            }
                        } else {
                            if (deep) {
                                if (result.equals(VisitResult.CONTINUE)) {
                                    if (traversed == null || !traversed.contains(resolved.uuid())) {
                                        nextDeep.add(resolved);
                                    }
                                }
                            }
                        }
                    }
                    if (!nextDeep.isEmpty()) {
                        final int[] index = new int[1];
                        index[0] = 0;
                        final List<Callback<Throwable>> next = new ArrayList<Callback<Throwable>>();
                        next.add(new Callback<Throwable>() {
                            @Override
                            public void on(Throwable throwable) {
                                index[0] = index[0] + 1;
                                if (index[0] == nextDeep.size()) {
                                    if (Checker.isDefined(end)) {
                                        end.on(null);
                                    }
                                } else {
                                    final AbstractKObject abstractKObject = (AbstractKObject) nextDeep.get(index[0]);
                                    if (containedOnly) {
                                        abstractKObject.internal_visit(visitor, next.get(0), true, true, visited, traversed);
                                    } else {
                                        abstractKObject.internal_visit(visitor, next.get(0), true, false, visited, traversed);
                                    }
                                }
                            }
                        });

                        final AbstractKObject abstractKObject = (AbstractKObject) nextDeep.get(index[0]);
                        if (containedOnly) {
                            abstractKObject.internal_visit(visitor, next.get(0), true, true, visited, traversed);
                        } else {
                            abstractKObject.internal_visit(visitor, next.get(0), true, false, visited, traversed);
                        }
                    } else {
                        if (Checker.isDefined(end)) {
                            end.on(null);
                        }
                    }
                }
            });
        }
    }

    public String toJSON() {
        KCacheEntry raw = view().universe().model().manager().entry(this, AccessMode.READ);
        if (raw != null) {
            return JsonRaw.encode(raw, _uuid, _metaClass, true, false);
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return toJSON();
    }

    @Override
    public ModelTrace[] traces(TraceRequest request) {
        if (!Checker.isDefined(request)) {
            return null;
        }
        List<ModelTrace> traces = new ArrayList<ModelTrace>();
        if (TraceRequest.ATTRIBUTES_ONLY.equals(request) || TraceRequest.ATTRIBUTES_REFERENCES.equals(request)) {
            for (int i = 0; i < metaClass().metaAttributes().length; i++) {
                MetaAttribute current = metaClass().metaAttributes()[i];
                Object payload = get(current);
                if (payload != null) {
                    traces.add(new ModelSetTrace(_uuid, current, payload));
                }
            }
        }
        if (TraceRequest.REFERENCES_ONLY.equals(request) || TraceRequest.ATTRIBUTES_REFERENCES.equals(request)) {
            for (int i = 0; i < metaClass().metaReferences().length; i++) {
                MetaReference ref = metaClass().metaReferences()[i];
                KCacheEntry raw = view().universe().model().manager().entry(this, AccessMode.READ);
                Object o = null;
                if (raw != null) {
                    o = raw.get(ref.index());
                }
                if (o instanceof Set) {
                    Set<Long> contents = (Set<Long>) o;
                    Long[] contentsArr = contents.toArray(new Long[contents.size()]);
                    for (int j = 0; j < contentsArr.length; j++) {
                        traces.add(new ModelAddTrace(_uuid, ref, contentsArr[j]));
                    }
                } else if (o != null) {
                    traces.add(new ModelAddTrace(_uuid, ref, (Long) o));
                }
            }
        }
        return traces.toArray(new ModelTrace[traces.size()]);
    }

    public void inbounds(Callback<KObject[]> callback) {
        KCacheEntry rawPayload = view().universe().model().manager().entry(this, AccessMode.READ);
        if (rawPayload != null) {
            Object payload = rawPayload.get(Index.INBOUNDS_INDEX);
            if (payload != null) {
                Set<Long> inboundsKeys = (Set<Long>) payload;
                Long[] keysArr = inboundsKeys.toArray(new Long[inboundsKeys.size()]);
                _view.lookupAll(keysArr, callback);
            } else {
                callback.on(new KObject[0]);
            }
        } else {
            callback.on(new KObject[0]);
        }
    }

    public void set_parent(Long p_parentKID, MetaReference p_metaReference) {
        KCacheEntry raw = _view.universe().model().manager().entry(this, AccessMode.WRITE);
        if (raw != null) {
            raw.set(Index.PARENT_INDEX, p_parentKID);
            raw.set(Index.REF_IN_PARENT_INDEX, p_metaReference);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractKObject)) {
            return false;
        } else {
            AbstractKObject casted = (AbstractKObject) obj;
            return (casted.uuid() == _uuid) && _view.equals(casted._view);
        }
    }

    @Override
    public int hashCode() {
        String hashString = uuid() + "-" + view().now() + "-" + view().universe().key();
        return hashString.hashCode();
    }

    @Override
    public void diff(KObject target, Callback<TraceSequence> callback) {
        DefaultModelCompare.diff(this, target, callback);
    }

    @Override
    public void merge(KObject target, Callback<TraceSequence> callback) {
        DefaultModelCompare.merge(this, target, callback);
    }

    @Override
    public void intersection(KObject target, Callback<TraceSequence> callback) {
        DefaultModelCompare.intersection(this, target, callback);
    }

    @Override
    public <U extends KObject> void jump(long time, final Callback<U> callback) {
        view().universe().time(time).lookup(_uuid, new Callback<KObject>() {
            @Override
            public void on(KObject kObject) {
                if (callback != null) {
                    try {
                        callback.on((U) kObject);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        callback.on(null);
                    }
                }
            }
        });
    }

    public MetaReference internal_transpose_ref(MetaReference p) {
        if (!Checker.isDefined(p)) {
            return null;
        } else {
            return metaClass().metaReference(p.metaName());
        }
    }

    public MetaAttribute internal_transpose_att(MetaAttribute p) {
        if (!Checker.isDefined(p)) {
            return null;
        } else {
            return metaClass().metaAttribute(p.metaName());
        }
    }

    public MetaOperation internal_transpose_op(MetaOperation p) {
        if (!Checker.isDefined(p)) {
            return null;
        } else {
            return metaClass().metaOperation(p.metaName());
        }
    }

    public KTraversal traverse(MetaReference p_metaReference) {
        return new DefaultKTraversal(this, new KTraverseAction(p_metaReference));
    }

    public KTraversal traverseQuery(String metaReferenceQuery) {
        return new DefaultKTraversal(this, new KTraverseQueryAction(metaReferenceQuery));
    }

    public KTraversal traverseInbounds(String metaReferenceQuery) {
        return new DefaultKTraversal(this, new KReverseQueryAction(metaReferenceQuery));
    }

    public KTraversal traverseParent() {
        return new DefaultKTraversal(this, new KParentsAction());
    }


    @Override
    public MetaReference[] referencesWith(KObject o) {
        if (Checker.isDefined(o)) {
            KCacheEntry raw = _view.universe().model().manager().entry(this, AccessMode.READ);
            if (raw != null) {
                MetaReference[] allReferences = metaClass().metaReferences();
                List<MetaReference> selected = new ArrayList<MetaReference>();
                for (int i = 0; i < allReferences.length; i++) {
                    Object rawI = raw.get(allReferences[i].index());
                    if (rawI instanceof Set) {
                        try {
                            Set<Long> castedSet = (Set<Long>) rawI;
                            if (castedSet.contains(o.uuid())) {
                                selected.add(allReferences[i]);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (rawI != null) {
                        try {
                            Long casted = (Long) rawI;
                            if (casted == o.uuid()) {
                                selected.add(allReferences[i]);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return selected.toArray(new MetaReference[selected.size()]);
            } else {
                return new MetaReference[0];
            }
        } else {
            return new MetaReference[0];
        }
    }

    @Override
    public KTask<Throwable> taskVisit(ModelVisitor p_visitor, VisitRequest p_request) {
        AbstractKTaskWrapper<Throwable> task = new AbstractKTaskWrapper<Throwable>();
        visit(p_visitor, task.initCallback(), p_request);
        return task;
    }

    @Override
    public KTask<Throwable> taskDelete() {
        AbstractKTaskWrapper<Throwable> task = new AbstractKTaskWrapper<Throwable>();
        delete(task.initCallback());
        return task;
    }

    @Override
    public KTask<KObject> taskParent() {
        AbstractKTaskWrapper<KObject> task = new AbstractKTaskWrapper<KObject>();
        parent(task.initCallback());
        return task;
    }

    @Override
    public KTask<KObject[]> taskSelect(String p_query) {
        AbstractKTaskWrapper<KObject[]> task = new AbstractKTaskWrapper<KObject[]>();
        select(p_query, task.initCallback());
        return task;
    }

    @Override
    public KTask<KObject[]> taskRef(MetaReference p_metaReference) {
        AbstractKTaskWrapper<KObject[]> task = new AbstractKTaskWrapper<KObject[]>();
        ref(p_metaReference, task.initCallback());
        return task;
    }

    @Override
    public KTask<KObject[]> taskInbounds() {
        AbstractKTaskWrapper<KObject[]> task = new AbstractKTaskWrapper<KObject[]>();
        inbounds(task.initCallback());
        return task;
    }

    @Override
    public KTask<TraceSequence> taskDiff(KObject p_target) {
        AbstractKTaskWrapper<TraceSequence> task = new AbstractKTaskWrapper<TraceSequence>();
        diff(p_target, task.initCallback());
        return task;
    }

    @Override
    public KTask<TraceSequence> taskMerge(KObject p_target) {
        AbstractKTaskWrapper<TraceSequence> task = new AbstractKTaskWrapper<TraceSequence>();
        merge(p_target, task.initCallback());
        return task;
    }

    @Override
    public KTask<TraceSequence> taskIntersection(KObject p_target) {
        AbstractKTaskWrapper<TraceSequence> task = new AbstractKTaskWrapper<TraceSequence>();
        intersection(p_target, task.initCallback());
        return task;
    }

    @Override
    public <U extends KObject> KTask<U> taskJump(long p_time) {
        AbstractKTaskWrapper<U> task = new AbstractKTaskWrapper<U>();
        jump(p_time, task.initCallback());
        return task;
    }

    @Override
    public KTask<String> taskPath() {
        AbstractKTaskWrapper<String> task = new AbstractKTaskWrapper<String>();
        path(task.initCallback());
        return task;
    }

    @Override
    public KTask<KInfer[]> taskInferObjects() {
        AbstractKTaskWrapper<KInfer[]> task = new AbstractKTaskWrapper<KInfer[]>();
        inferObjects(task.initCallback());
        return task;
    }

    @Override
    public void inferObjects(Callback<KInfer[]> p_callback) {
        //TODO
    }

    public void call(MetaOperation p_operation, Object[] p_params, Callback<Object> p_callback) {
        view().universe().model().manager().operationManager().call(this, p_operation, p_params, p_callback);
    }

    @Override
    public KTask<Object> taskCall(MetaOperation p_operation, Object[] p_params) {
        AbstractKTaskWrapper<Object> temp_task = new AbstractKTaskWrapper<Object>();
        call(p_operation, p_params, temp_task.initCallback());
        return temp_task;
    }

    @Override
    public Object inferAttribute(MetaAttribute attribute) {
        //TODO
        return null;
    }

    @Override
    public void inferCall(MetaOperation operation, Object[] params, Callback<Object> callback) {
        //TODO
    }

}
