package org.apache.lucene.util.unimas.index;

/**
 * B+树的定义：
 * <p>
 * 1.任意非叶子结点最多有M个子节点；且M>2；M为B+树的阶数
 * 2.除根结点以外的非叶子结点至少有 (M+1)/2个子节点；
 * 3.根结点至少有2个子节点；
 * 4.除根节点外每个结点存放至少（M-1）/2和至多M-1个关键字；（至少1个关键字）
 * 5.非叶子结点的子树指针比关键字多1个；
 * 6.非叶子节点的所有key按升序存放，假设节点的关键字分别为K[0], K[1] … K[M-2],
 * 指向子女的指针分别为P[0], P[1]…P[M-1]。则有：
 * P[0] < K[0] <= P[1] < K[1] …..< K[M-2] <= P[M-1]
 * 7.所有叶子结点位于同一层；
 * 8.为所有叶子结点增加一个链指针；
 * 9.所有关键字都在叶子结点出现
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public class BplusTree<K extends Comparable<K>, V> {

    /** 根节点 */
    protected BplusNode<K, V> root;

    /** 阶数，M值 */
    protected int order;

    /** 叶子节点的链表头 */
    protected BplusNode<K, V> head;

    /** 树高*/
    protected int height = 0;

    public BplusNode<K, V> getHead() {
        return head;
    }

    public void setHead(BplusNode<K, V> head) {
        this.head = head;
    }

    public BplusNode<K, V> getRoot() {
        return root;
    }

    public void setRoot(BplusNode<K, V> root) {
        this.root = root;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    /**
     * 取>=key && <=end的key
     * @param start
     * @param end
     * @return
     */
    public List<V> getRange(K start, K end) {
        List<V> res = new ArrayList<>();
        Entry<BplusNode<K, V>, Integer> small = root.getClosestSmallEqual(start);
        Entry<BplusNode<K, V>, Integer> big = root.getClosestBigEqual(end);
        if (small == null || big == null)
            return null;
        BplusNode<K, V> smallNode = small.getKey();
        BplusNode<K, V> bigNode = big.getKey();
        //上下界都在一个node里
        if (smallNode.equals(bigNode)) {
            for (int i = small.getValue(); i <= big.getValue(); i++)
                res.add(smallNode.entries_val.get(i));
        }
        //上下界跨node
        else {
            for (int i = small.getValue(); i <= smallNode.entries_val.size() - 1; i++)
                res.add(smallNode.entries_val.get(i));
            smallNode = smallNode.next;
            while (!smallNode.equals(bigNode)) {
                res.addAll(smallNode.entries_val);
                smallNode = smallNode.next;
            }
            for (int i = 0; i <= big.getValue(); i++)
                res.add(bigNode.entries_val.get(i));
        }
        return res;
    }

    /**
     * 取>=start的key
     * @param start
     * @return
     */
    public List<V> getRangeStart(K start) {
        List<V> res = new ArrayList<>();
        Entry<BplusNode<K, V>, Integer> small = root.getClosestSmallEqual(start);
        if (small == null)
            return null;
        BplusNode<K, V> smallNode = small.getKey();
        for (int i = small.getValue(); i <= smallNode.entries_val.size() - 1; i++)
            res.add(smallNode.entries_val.get(i));
        smallNode = smallNode.next;
        while (smallNode != null) {
            res.addAll(smallNode.entries_val);
            smallNode = smallNode.next;
        }
        return res;
    }

    /**
     * 取<=end的key
     * @param end
     * @return
     */
    public List<V> getRangeEnd(K end) {
        List<V> res = new ArrayList<>();
        Entry<BplusNode<K, V>, Integer> big = root.getClosestBigEqual(end);
        if (big == null)
            return null;
        BplusNode<K, V> smallNode = head;
        BplusNode<K, V> bigNode = big.getKey();
        //上下界都在一个node里
        if (smallNode.equals(bigNode)) {
            for (int i = 0; i <= big.getValue(); i++)
                res.add(smallNode.entries_val.get(i));
        }
        //上下界跨node
        else {
            for (int i = 0; i <= smallNode.entries_val.size() - 1; i++)
                res.add(smallNode.entries_val.get(i));
            smallNode = smallNode.next;
            while (!smallNode.equals(bigNode)) {
                res.addAll(smallNode.entries_val);
                smallNode = smallNode.next;
            }
            for (int i = 0; i <= big.getValue(); i++)
                res.add(bigNode.entries_val.get(i));
        }
        return res;
    }

    public V get(K key) {
        return root.get(key);
    }

    public V remove(K key) {
        return root.remove(key, this);
    }

    public void insertOrUpdate(K key, V value) {
        root.insertOrUpdate(key, value, this);
    }

    public BplusTree(int order) {
        if (order < 3) {
            System.out.print("order must be greater than 2");
            System.exit(0);
        }
        this.order = order;
        root = new BplusNode<K, V>(true, true);
        head = root;
    }

    // 测试
    public static void main(String[] args) {

        int size = 1000000;
        int order = 100;
//        testRandomInsert(size, order);

        testOrderInsert(size, order);

//        testRandomSearch(size, order);

        testOrderSearch(size, order);
//
//		 testRandomRemove(size, order);
//
//		 testOrderRemove(size, order);
    }

    private static void testOrderRemove(int size, int order) {
        BplusTree<Integer, Integer> tree = new BplusTree<Integer, Integer>(order);
        System.out.println("\nTest order remove " + size + " datas, of order:"
                + order);
        System.out.println("Begin order insert...");
        for (int i = 0; i < size; i++) {
            tree.insertOrUpdate(i, i);
        }
        System.out.println("Begin order remove...");
        long current = System.currentTimeMillis();
        for (int j = 0; j < size; j++) {
            if (tree.remove(j) == null) {
                System.err.println("得不到数据:" + j);
                break;
            }
        }
        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);
        System.out.println(tree.getHeight());
    }

    private static void testRandomRemove(int size, int order) {
        BplusTree<Integer, Integer> tree = new BplusTree<Integer, Integer>(order);
        System.out.println("\nTest random remove " + size + " datas, of order:"
                + order);
        Random random = new Random();
        boolean[] a = new boolean[size + 10];
        List<Integer> list = new ArrayList<Integer>();
        int randomNumber = 0;
        System.out.println("Begin random insert...");
        for (int i = 0; i < size; i++) {
            randomNumber = random.nextInt(size);
            a[randomNumber] = true;
            list.add(randomNumber);
            tree.insertOrUpdate(randomNumber, randomNumber);
        }
        System.out.println("Begin random remove...");
        long current = System.currentTimeMillis();
        for (int j = 0; j < size; j++) {
            randomNumber = list.get(j);
            if (a[randomNumber]) {
                if (tree.remove(randomNumber) == null) {
                    System.err.println("得不到数据:" + randomNumber);
                    break;
                } else {
                    a[randomNumber] = false;
                }
            }
        }
        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);
        System.out.println(tree.getHeight());
    }

    private static void testOrderSearch(int size, int order) {
        BplusTree<Integer, Integer> tree = new BplusTree<Integer, Integer>(order);
        System.out.println("\nTest order search " + size + " datas, of order:"
                + order);
        System.out.println("Begin order insert...");
        for (int i = 0; i < size; i++) {
            tree.insertOrUpdate(i, i);
        }
        System.out.println("Begin order search...");
        long current = System.currentTimeMillis();
        for (int j = 0; j < size; j++) {
            if (tree.get(j) == null) {
                System.err.println("得不到数据:" + j);
                break;
            }
        }
        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);
    }

    private static void testRandomSearch(int size, int order) {
        BplusTree<Integer, Integer> tree = new BplusTree<Integer, Integer>(order);
        System.out.println("\nTest random search " + size + " datas, of order:"
                + order);
        Random random = new Random();
        boolean[] a = new boolean[size + 10];
        int randomNumber = 0;
        System.out.println("Begin random insert...");
        for (int i = 0; i < size; i++) {
            randomNumber = random.nextInt(size);
            a[randomNumber] = true;
            tree.insertOrUpdate(randomNumber, randomNumber);
        }
        System.out.println("Begin random search...");
        long current = System.currentTimeMillis();
        for (int j = 0; j < size; j++) {
            randomNumber = random.nextInt(size);
            if (a[randomNumber]) {
                if (tree.get(randomNumber) == null) {
                    System.err.println("得不到数据:" + randomNumber);
                    break;
                }
            }
        }
        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);
    }

    private static void testRandomInsert(int size, int order) {
        BplusTree<Integer, Integer> tree = new BplusTree<Integer, Integer>(order);
        System.out.println("\nTest random insert " + size + " datas, of order:"
                + order);
        Random random = new Random();
        int randomNumber = 0;
        long current = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            randomNumber = random.nextInt(size);
            tree.insertOrUpdate(randomNumber, randomNumber);
        }
        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);

        System.out.println(tree.getHeight());
    }

    private static void testOrderInsert(int size, int order) {
        BplusTree<Integer, Integer> tree = new BplusTree<Integer, Integer>(order);
        System.out.println("\nTest order insert " + size + " datas, of order:"
                + order);
        long current = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            tree.insertOrUpdate(i, i);
        }

        BplusNode<Integer, Integer> head = tree.getHead();
        System.out.println(head);
        BplusNode<Integer, Integer> next = head.next;
        while (next != null) {
            System.out.println(next);
            next = next.next;
        }
        long duration = System.currentTimeMillis() - current;
        System.out.println("time elpsed for duration: " + duration);
    }
}