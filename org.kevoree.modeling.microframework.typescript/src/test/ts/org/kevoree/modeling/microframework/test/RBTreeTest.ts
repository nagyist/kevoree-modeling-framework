///<reference path="../../../../junit/Test.ts"/>
///<reference path="../../../../junit/Assert.ts"/>
///<reference path="../../api/time/rbtree/TreeNode.ts"/>
///<reference path="../../api/time/rbtree/RBTree.ts"/>
///<reference path="../../api/time/rbtree/State.ts"/>
///<reference path="../../../../../java/util/LinkedList.ts"/>
///<reference path="../../../../../java/util/Queue.ts"/>

class RBTreeTest {

  public nextTest(): void {
    var MIN: number = 0L;
    var MAX: number = 99L;
    for (var j: number = MIN; j <= MAX; j++) {
      var tree: RBTree = new RBTree();
      for (var i: number = MIN; i <= j; i++) {
        if ((i % 3) == 0L) {
          tree.insert(i, State.DELETED);
        } else {
          tree.insert(i, State.EXISTS);
        }
      }
      for (var i: number = MIN; i < j - 1; i++) {
        Assert.assertTrue("I: " + i + " -> " + tree.next(i).getKey() + " != " + (i + 1), tree.next(i).getKey() == i + 1);
      }
      Assert.assertTrue("I: " + j + " -> " + tree.next(j) + " != null", tree.next(j) == null);
    }
  }

  private printTree(root: TreeNode): void {
    var queue: Queue<TreeNode> = new LinkedList<TreeNode>();
    queue.add(root);
    queue.add(null);
    while (!queue.isEmpty()){
      var current: TreeNode = queue.poll();
      while (current != null){
        System.out.print("| " + current.getKey() + " ");
        if (current.getLeft() != null) {
          queue.add(current.getLeft());
        }
        if (current.getRight() != null) {
          queue.add(current.getRight());
        }
        current = queue.poll();
      }
      System.out.println();
      if (!queue.isEmpty()) {
        queue.add(null);
      }
    }
  }

  public previousTest(): void {
    var MIN: number = 0L;
    var MAX: number = 99L;
    for (var j: number = MIN + 1; j <= MAX; j++) {
      var tree: RBTree = new RBTree();
      for (var i: number = MIN; i <= j; i++) {
        if ((i % 7) == 0L) {
          tree.insert(i, State.DELETED);
        } else {
          tree.insert(i, State.EXISTS);
        }
      }
      for (var i: number = j; i > MIN; i--) {
        Assert.assertTrue("I: " + i + " -> " + tree.previous(i).getKey() + " != " + (i - 1), tree.previous(i).getKey() == i - 1);
      }
      Assert.assertTrue("I: " + j + " -> " + tree.previous(MIN) + " != null", tree.previous(MIN) == null);
    }
  }

  public nextWhileNotTest(): void {
    var tree: RBTree = new RBTree();
    for (var i: number = 0; i <= 6; i++) {
      tree.insert(i, State.EXISTS);
    }
    tree.insert(8L, State.DELETED);
    tree.insert(10L, State.EXISTS);
    tree.insert(11L, State.EXISTS);
    tree.insert(13L, State.EXISTS);
    for (var i: number = 0; i < 5; i++) {
      Assert.assertTrue(tree.nextWhileNot(i, State.DELETED).getKey() == (i + 1));
    }
    Assert.assertTrue(tree.nextWhileNot(5, State.DELETED) != null && tree.nextWhileNot(5, State.DELETED).getKey() == 6L);
    Assert.assertNull(tree.nextWhileNot(6, State.DELETED));
    Assert.assertNull(tree.nextWhileNot(7, State.DELETED));
    Assert.assertNull(tree.nextWhileNot(8, State.DELETED));
    Assert.assertTrue("" + tree.nextWhileNot(9, State.DELETED).getKey(), tree.nextWhileNot(9, State.DELETED) != null && tree.nextWhileNot(9, State.DELETED).getKey() == 10L);
    Assert.assertTrue(tree.nextWhileNot(10, State.DELETED) != null && tree.nextWhileNot(10, State.DELETED).getKey() == 11L);
  }

  public previousWhileNotTest(): void {
    var tree: RBTree = new RBTree();
    for (var i: number = 0; i <= 6; i++) {
      tree.insert(i, State.EXISTS);
    }
    tree.insert(8L, State.DELETED);
    tree.insert(10L, State.EXISTS);
    tree.insert(11L, State.EXISTS);
    tree.insert(13L, State.EXISTS);
    Assert.assertTrue(tree.previousWhileNot(14, State.DELETED) != null && tree.previousWhileNot(14, State.DELETED).getKey() == 13L);
    Assert.assertTrue(tree.previousWhileNot(13, State.DELETED) != null && tree.previousWhileNot(13, State.DELETED).getKey() == 11L);
    Assert.assertTrue(tree.previousWhileNot(12, State.DELETED) != null && tree.previousWhileNot(12, State.DELETED).getKey() == 11L);
    Assert.assertTrue(tree.previousWhileNot(11, State.DELETED) != null && tree.previousWhileNot(11, State.DELETED).getKey() == 10L);
    Assert.assertNull(tree.previousWhileNot(10, State.DELETED));
    Assert.assertNull(tree.previousWhileNot(9, State.DELETED));
    Assert.assertNull(tree.previousWhileNot(8, State.DELETED));
    Assert.assertTrue(tree.previousWhileNot(7, State.DELETED) != null && tree.previousWhileNot(7, State.DELETED).getKey() == 6L);
    Assert.assertTrue(tree.previousWhileNot(6, State.DELETED) != null && tree.previousWhileNot(6, State.DELETED).getKey() == 5L);
  }

  public firstTest(): void {
    var MIN: number = 0L;
    var MAX: number = 99L;
    for (var j: number = MIN + 1; j <= MAX; j++) {
      var tree: RBTree = new RBTree();
      for (var i: number = MIN; i <= j; i++) {
        if ((i % 3) == 0L) {
          tree.insert(i, State.DELETED);
        } else {
          tree.insert(i, State.EXISTS);
        }
      }
      Assert.assertTrue(tree.first().getKey() == MIN);
    }
  }

  public lastTest(): void {
    var MIN: number = 0L;
    var MAX: number = 99L;
    for (var j: number = MIN + 1; j <= MAX; j++) {
      var tree: RBTree = new RBTree();
      for (var i: number = MIN; i <= j; i++) {
        if ((i % 3) == 0L) {
          tree.insert(i, State.DELETED);
        } else {
          tree.insert(i, State.EXISTS);
        }
      }
      Assert.assertTrue("" + tree.last().getKey() + " != " + j, tree.last().getKey() == j);
    }
  }

  public firstWhileNot(): void {
    var tree: RBTree = new RBTree();
    for (var i: number = 0; i <= 6; i++) {
      tree.insert(i, State.EXISTS);
    }
    tree.insert(8L, State.DELETED);
    tree.insert(10L, State.EXISTS);
    tree.insert(11L, State.EXISTS);
    tree.insert(13L, State.EXISTS);
    Assert.assertTrue(tree.firstWhileNot(14, State.DELETED).getKey() == 10L);
    Assert.assertTrue(tree.firstWhileNot(13, State.DELETED).getKey() == 10L);
    Assert.assertTrue(tree.firstWhileNot(12, State.DELETED).getKey() == 10L);
    Assert.assertTrue(tree.firstWhileNot(11, State.DELETED).getKey() == 10L);
    Assert.assertTrue(tree.firstWhileNot(10, State.DELETED).getKey() == 10L);
    Assert.assertNull(tree.firstWhileNot(9, State.DELETED));
    Assert.assertNull(tree.firstWhileNot(8, State.DELETED));
    Assert.assertTrue("" + tree.firstWhileNot(7, State.DELETED).getKey(), tree.firstWhileNot(7, State.DELETED).getKey() == 0L);
    Assert.assertTrue(tree.firstWhileNot(6, State.DELETED).getKey() == 0L);
  }

  public lastWhileNot(): void {
    var tree: RBTree = new RBTree();
    for (var i: number = 0; i <= 6; i++) {
      tree.insert(i, State.EXISTS);
    }
    tree.insert(8L, State.DELETED);
    tree.insert(10L, State.EXISTS);
    tree.insert(11L, State.EXISTS);
    tree.insert(13L, State.EXISTS);
    Assert.assertTrue(tree.lastWhileNot(0, State.DELETED).getKey() == 6L);
    Assert.assertTrue(tree.lastWhileNot(5, State.DELETED).getKey() == 6L);
    Assert.assertTrue(tree.lastWhileNot(6, State.DELETED).getKey() == 6L);
    Assert.assertTrue(tree.lastWhileNot(7, State.DELETED).getKey() == 6L);
    Assert.assertNull(tree.lastWhileNot(8, State.DELETED));
    Assert.assertNull(tree.lastWhileNot(9, State.DELETED));
    Assert.assertTrue(tree.lastWhileNot(10, State.DELETED).getKey() == 13L);
    Assert.assertTrue(tree.lastWhileNot(11, State.DELETED).getKey() == 13L);
    Assert.assertTrue(tree.lastWhileNot(12, State.DELETED).getKey() == 13L);
    Assert.assertTrue(tree.lastWhileNot(13, State.DELETED).getKey() == 13L);
    Assert.assertTrue(tree.lastWhileNot(14, State.DELETED).getKey() == 13L);
  }

  public previousOrEqualTest(): void {
    var tree: RBTree = new RBTree();
    for (var i: number = 0; i <= 6; i++) {
      tree.insert(i, State.EXISTS);
    }
    tree.insert(8L, State.DELETED);
    tree.insert(10L, State.EXISTS);
    tree.insert(11L, State.EXISTS);
    tree.insert(13L, State.EXISTS);
    Assert.assertNull(tree.previousOrEqual(-1));
    Assert.assertEquals(tree.previousOrEqual(0).getKey(), 0L);
    Assert.assertEquals(tree.previousOrEqual(1).getKey(), 1L);
    Assert.assertEquals(tree.previousOrEqual(7).getKey(), 6L);
    Assert.assertEquals(tree.previousOrEqual(8).getKey(), 8L);
    Assert.assertEquals(tree.previousOrEqual(9).getKey(), 8L);
    Assert.assertEquals(tree.previousOrEqual(10).getKey(), 10L);
    Assert.assertEquals(tree.previousOrEqual(13).getKey(), 13L);
    Assert.assertEquals(tree.previousOrEqual(14).getKey(), 13L);
  }

  public nextOrEqualTest(): void {
    var tree: RBTree = new RBTree();
    for (var i: number = 0; i <= 6; i++) {
      tree.insert(i, State.EXISTS);
    }
    tree.insert(8L, State.DELETED);
    tree.insert(10L, State.EXISTS);
    tree.insert(11L, State.EXISTS);
    tree.insert(13L, State.EXISTS);
    Assert.assertTrue(tree.nextOrEqual(-1).getKey() == 0L);
    Assert.assertTrue(tree.nextOrEqual(0).getKey() == 0L);
    Assert.assertTrue(tree.nextOrEqual(1).getKey() == 1L);
    Assert.assertTrue(tree.nextOrEqual(7).getKey() == 8L);
    Assert.assertTrue(tree.nextOrEqual(8).getKey() == 8L);
    Assert.assertTrue(tree.nextOrEqual(9).getKey() == 10L);
    Assert.assertTrue(tree.nextOrEqual(10).getKey() == 10L);
    Assert.assertTrue(tree.nextOrEqual(13).getKey() == 13L);
    Assert.assertNull(tree.nextOrEqual(14));
  }

}

