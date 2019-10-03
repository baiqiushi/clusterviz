package util;

import javafx.util.Pair;
import model.Cluster;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class KDTree<T extends Number & Comparable<? super T>> {

    class Node {
        private IKDPoint<T> point;
        private List<IKDPoint<T>> duplicates;
        int align;
        int depth;
        Node left = null;
        Node right = null;

        public Node(IKDPoint<T> point, int align, int depth) {
            this.point = point;
            this.align = align;
            this.depth = depth;
            this.duplicates = new LinkedList<>();
        }

        public IKDPoint<T> getPoint() {
            return this.point;
        }

        public List<IKDPoint<T>> getDuplicates() {
            return this.duplicates;
        }

        public void addDuplicate(IKDPoint<T> point) {
            this.duplicates.add(point);
        }
    }

    Node root;
    int k;
    int height = 0;

    public KDTree(int k) {
        this.k = k;
        this.root = null;
    }

    public void insert(IKDPoint<T> point) {

        // empty tree
        if (root == null) {
            root = new Node(point, 0, 0);
            height = 1;
            return;
        }

        // dimension index to align
        int align = 0;
        Node p = root;
        Node p0 = p;
        boolean left = true;
        // find the position to insert
        while (p != null) {
            IKDPoint<T> pp = p.getPoint();
            // duplicate
            if (pp.equalsTo(point)) {
                p.addDuplicate(point);
                return;
            }
            else if (point.getDimensionValue(align).compareTo(pp.getDimensionValue(align)) < 0) {
                p0 = p;
                p = p.left;
                left = true;
            }
            else {
                p0 = p;
                p = p.right;
                left = false;
            }
            align = (align + 1) % this.k;
        }
        // p0 points to the parent of new node
        p = new Node(point, align, p0.depth + 1);
        if (p.depth + 1 > height) {
            height = p.depth + 1;
        }
        if (left) {
            p0.left = p;
        }
        else {
            p0.right = p;
        }
    }

    public void load(IKDPoint<T>[] points) {
        for (int i = 0; i < points.length; i ++) {
            this.insert(points[i]);
        }
    }

    public List<IKDPoint<T>> within(IKDPoint<T> point, T radius) {
        List<IKDPoint<T>> result = new ArrayList<IKDPoint<T>>();
        Queue<Node> queue = new LinkedList<Node>();
        queue.add(root);
        while (queue.size() > 0) {
            Node p = queue.poll();
            int align = p.align;
            IKDPoint<T> node = p.getPoint();
            // if node within range, put it into result, and put both children to queue
            if (node.distanceTo(point).compareTo(radius) <= 0) {
                result.add(node);
                // also add duplicates inside node p
                for (IKDPoint<T> duplicate: p.getDuplicates()) {
                    result.add(duplicate);
                }
                if (p.left != null) {
                    queue.add(p.left);
                }
                if (p.right != null) {
                    queue.add(p.right);
                }
            }
            // else node outside range
            else {
                // but if NOT (node.x + r) < point.x, left child still needs to be checked
                if ((node.getDimensionValue(align).doubleValue() + radius.doubleValue()) >= point.getDimensionValue(align).doubleValue()) {
                    if (p.left != null) {
                        queue.add(p.left);
                    }
                }
                // but if NOT point.x < (node.x - r), right child still needs to be checked
                if ((point.getDimensionValue(align).doubleValue() >= (node.getDimensionValue(align).doubleValue() - radius.doubleValue()))) {
                    if (p.right != null) {
                        queue.add(p.right);
                    }
                }
            }
        }
        return result;
    }

    public List<IKDPoint<T>> range(IKDPoint<T> leftBottom, IKDPoint<T> rightTop) {
        List<IKDPoint<T>> result = new ArrayList<IKDPoint<T>>();
        Queue<Node> queue = new LinkedList<Node>();
        queue.add(root);
        while (queue.size() > 0) {
            Node p = queue.poll();
            int align = p.align;
            IKDPoint<T> node = p.getPoint();
            // if node within range, put it into result, and put both children to queue
            if (node.rightTo(leftBottom) && node.leftTo(rightTop)) {
                result.add(node);
                // also add duplicates inside node p
                for (IKDPoint<T> duplicate: p.getDuplicates()) {
                    result.add(duplicate);
                }
                if (p.left != null) {
                    queue.add(p.left);
                }
                if (p.right != null) {
                    queue.add(p.right);
                }
            }
            // else node outside range
            else {
                if (rightTop.getDimensionValue(align).compareTo(node.getDimensionValue(align)) < 0) {
                    if (p.left != null) {
                        queue.add(p.left);
                    }
                }
                else if (leftBottom.getDimensionValue(align).compareTo(node.getDimensionValue(align)) > 0) {
                    if (p.right != null) {
                        queue.add(p.right);
                    }
                }
                // on the separation axis, node is between leftBottom and rightTop, both children need to be explored
                else {
                    if (p.left != null) {
                        queue.add(p.left);
                    }
                    if (p.right != null) {
                        queue.add(p.right);
                    }
                }
            }
        }
        return result;
    }

    public void print() {
        System.out.println("=================== KDTree ===================");
        Queue<Pair<Integer, Node>> queue = new LinkedList<Pair<Integer, Node>>();
        queue.add(new Pair<Integer, Node>(0, root));
        int currentLevel = -1;
        while (queue.size() > 0) {
            Pair<Integer, Node> p = queue.poll();
            int level = p.getKey();
            Node node = p.getValue();
            if (level > currentLevel) {
                System.out.println();
                System.out.print("[" + level + "] ");
                currentLevel = level;
            }
            System.out.print(((Cluster)node.getPoint()).id);
            if (!node.getDuplicates().isEmpty()) {
                System.out.print("[");
                for (IKDPoint<T> duplicate: node.getDuplicates()) {
                    System.out.print(((Cluster)duplicate).id + ",");
                }
            }
            System.out.print(", ");
            if (node.left != null) {
                queue.add(new Pair<Integer, Node>(level + 1, node.left));
            }
            if (node.right != null) {
                queue.add(new Pair<Integer, Node>(level + 1, node.right));
            }
        }
        System.out.println();
    }

    /**
     * Copy paste the output to this link: http://www.webgraphviz.com/
     */
    public void printGraphViz() {
        System.out.println("=================== KDTree ===================");
        System.out.println("digraph kdtree {");
        Queue<Node> queue = new LinkedList<Node>();
        queue.add(root);
        while (queue.size() > 0) {
            Node node = queue.poll();
            if (node.left != null) {
                System.out.println("\"[" + node.align + "] " + ((Cluster)node.getPoint()).id + "\" -> \"[" + node.left.align + "] " + ((Cluster)node.left.getPoint()).id + "\"");
                queue.add(node.left);
            }
            if (node.right != null) {
                System.out.println("\"[" + node.align + "] " + ((Cluster)node.getPoint()).id + "\" -> \"[" + node.right.align + "] " + ((Cluster)node.right.getPoint()).id + "\"");
                queue.add(node.right);
            }
        }
        System.out.println("}");
    }
}
