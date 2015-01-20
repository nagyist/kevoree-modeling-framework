package org.kevoree.modeling.microframework.test.storage;

import org.junit.Assert;
import org.junit.Test;
import org.kevoree.modeling.api.Callback;
import org.kevoree.modeling.api.InboundReference;
import org.kevoree.modeling.api.KObject;
import org.kevoree.modeling.microframework.test.cloud.CloudUniverse;
import org.kevoree.modeling.microframework.test.cloud.CloudModel;
import org.kevoree.modeling.microframework.test.cloud.CloudView;
import org.kevoree.modeling.microframework.test.cloud.Node;
import org.kevoree.modeling.microframework.test.cloud.meta.MetaNode;

/**
 * Created by duke on 28/11/14.
 */
public class ParentStorageTest {

    @Test
    public void discardTest() {
        CloudModel universe = new CloudModel();
        universe.connect(null);
        //model.connect(null);

        CloudUniverse dimension0 = universe.newUniverse();
        CloudView time0 = dimension0.time(0l);

        Node root = time0.createNode();
        root.setName("root");
        time0.setRoot(root, null);

        Node n1 = time0.createNode();
        n1.setName("n1");

        Node n2 = time0.createNode();
        n2.setName("n2");

        root.addChildren(n1);
        root.addChildren(n2);

        Long val = 1L;

        Assert.assertEquals(n1.parentUuid(), val);
        try {
            root.eachChildren(null);
        } catch (Exception e) {
            Assert.assertNull(e);
        }
        //We clear the cache
        dimension0.discard(null);

        /*
        try {
            root.eachChildren(null);
            Assert.assertNull(root);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }*/

        time0.lookup(n1.uuid(), new Callback<KObject>() {
            @Override
            public void on(KObject r_n1) {
                Assert.assertNull(r_n1);
            }
        });
    }

    @Test
    public void parentTest() {

        //MemoryKDataBase.DEBUG = true;

        CloudModel universe = new CloudModel();
        universe.connect(null);

        CloudUniverse dimension0 = universe.newUniverse();
        CloudView time0 = dimension0.time(0l);

        Node root = time0.createNode();
        root.setName("root");
        time0.setRoot(root, null);

        Node n1 = time0.createNode();
        n1.setName("n1");

        Node n2 = time0.createNode();
        n2.setName("n2");

        root.addChildren(n1);
        root.addChildren(n2);
        Long val = 1L;
        Assert.assertEquals(n1.parentUuid(), val);
        Assert.assertEquals(n1.referenceInParent(), MetaNode.REF_CHILDREN);


        InboundReference[] inbounds = n1.inbounds();
        Assert.assertEquals(1, inbounds.length);

        try {
            root.eachChildren(null);
        } catch (Exception e) {
            Assert.assertNull(e);
        }
        //We clear the cache
        dimension0.saveUnload(null);

        /*
        try {
            root.eachChildren(null);
            Assert.assertNull(root);
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
*/

        time0.lookup(n1.uuid(), new Callback<KObject>() {
            @Override
            public void on(KObject r_n1) {
                Assert.assertNotNull(r_n1.parentUuid());
                Assert.assertNotNull(r_n1.referenceInParent());

                InboundReference[] references = r_n1.inbounds();
                Assert.assertEquals(1, references.length);
                //i[0]++;
            }
        });

        //lookup test
        //Assert.assertEquals(3, i[0]);

    }

}
