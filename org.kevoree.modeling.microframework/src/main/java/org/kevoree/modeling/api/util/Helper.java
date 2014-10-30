package org.kevoree.modeling.api.util;

import org.kevoree.modeling.api.Callback;
import org.kevoree.modeling.api.KObject;
import org.kevoree.modeling.api.meta.MetaReference;

/**
 * Created by duke on 10/8/14.
 */
public class Helper {

    public static <A> void forall(A[] inputs, final CallBackChain<A> each, final Callback<Throwable> end) {
        if (inputs == null) {
            return;
        }
        process(inputs, 0, each, end);
    }

    private static <A> void process(final A[] arr, final int index, final CallBackChain<A> each, final Callback<Throwable> end) {
        if (index >= arr.length) {
            if (end != null) {
                end.on(null);
            }
        } else {
            A obj = arr[index];
            each.on(obj, new Callback<Throwable>() {
                @Override
                public void on(Throwable err) {
                    if (err != null) {
                        if (end != null) {
                            end.on(err);
                        }
                    } else {
                        process(arr, index + 1, each, end);
                    }
                }
            });
        }
    }

    public static final char pathSep = '/';

    private static final char pathIDOpen = '[';
    private static final char pathIDClose = ']';


    private static final String rootPath = "/";

    public static String parentPath(String currentPath) {
        if (currentPath == null || currentPath.length() == 0) {
            return null;
        }
        if (currentPath.length() == 1) {
            return null;
        }
        int lastIndex = currentPath.lastIndexOf(pathSep);
        if (lastIndex != -1) {
            if (lastIndex == 0) {
                return rootPath;
            } else {
                return currentPath.substring(0, lastIndex);
            }
        } else {
            return null;
        }
    }

    public static boolean attachedToRoot(String path) {
        return path.length() > 0 && path.charAt(0) == pathSep;
    }

    public static boolean isRoot(String path) {
        return path.length() == 1 && path.charAt(0) == Helper.pathSep;
    }

    public static String path(String parent, MetaReference reference, KObject target) {
        if (isRoot(parent)) {
            return pathSep + reference.metaName() + pathIDOpen + target.domainKey() + pathIDClose;
        } else {
            return parent + pathSep + reference.metaName() + pathIDOpen + target.domainKey() + pathIDClose;
        }
    }

}
