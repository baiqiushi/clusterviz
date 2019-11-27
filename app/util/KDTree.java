package util;

import javafx.util.Pair;

import java.util.*;

public class KDTree<PointType extends IKDPoint> {

    class Node {
        private PointType point;
        private List<PointType> duplicates;
        int align;
        int depth;
        Node left = null;
        Node right = null;
        boolean deleted = false;

        public Node(PointType point, int align, int depth) {
            this.point = point;
            this.align = align;
            this.depth = depth;
            this.duplicates = new LinkedList<>();
        }

        public PointType getPoint() {
            return this.point;
        }

        public List<PointType> getDuplicates() {
            return this.duplicates;
        }

        public void addDuplicate(PointType point) {
            this.duplicates.add(point);
        }
    }

    private Node root;
    private int k;
    private int height = 0;
    private int size = 0;

    public KDTree(int k) {
        this.k = k;
        this.root = null;
    }

    public void insert(PointType point) {
        size ++;

        // empty tree
        if (root == null) {
            root = new Node(point, 0, 0);
            height = 1;
            return;
        }

        // dimension index to align
        int align = 0;
        Node currentNode = root;
        Node parentNode = currentNode;
        boolean left = true;
        // find the position to insert
        while (currentNode != null) {
            PointType currentPoint = currentNode.getPoint();
            // duplicate
            if (currentPoint.equalsTo(point)) {
                if (currentNode.deleted) {
                    currentNode.deleted = false;
                    currentNode.point = point;
                }
                else {
                    currentNode.addDuplicate(point);
                }
                return;
            }
            else if (point.getDimensionValue(align) < currentPoint.getDimensionValue(align)) {
                parentNode = currentNode;
                currentNode = currentNode.left;
                left = true;
            }
            else {
                parentNode = currentNode;
                currentNode = currentNode.right;
                left = false;
            }
            align = (align + 1) % this.k;
        }
        // parentNode points to the parent of new node
        currentNode = new Node(point, align, parentNode.depth + 1);
        if (currentNode.depth + 1 > height) {
            height = currentNode.depth + 1;
        }
        if (left) {
            parentNode.left = currentNode;
        }
        else {
            parentNode.right = currentNode;
        }
    }

    public void load(PointType[] points) {
        for (int i = 0; i < points.length; i ++) {
            this.insert(points[i]);
        }
    }

    public void delete(PointType point) {
        // dimension index to align
        int align = 0;
        Node currentNode = root;

        // find the point
        while (currentNode != null) {
            PointType currentPoint = currentNode.getPoint();
            // hit
            if (currentPoint.equalsTo(point)) {
                // if hit the node's point
                if (currentPoint.getId() == point.getId() && !currentNode.deleted) {
                    //clone currentPoint and make it a tombstone here
                    PointType tombstone = (PointType) currentPoint.clone();
                    currentNode.point = tombstone;
                    currentNode.deleted = true;
                }
                // else hit the node's duplicate point
                else {
                    for (Iterator<PointType> iter = currentNode.duplicates.iterator(); iter.hasNext();) {
                        PointType p = iter.next();
                        if (p.getId() == point.getId()) {
                            iter.remove();
                        }
                    }
                }
                size --;
                return;
            }
            else if (point.getDimensionValue(align) < currentPoint.getDimensionValue(align)) {
                currentNode = currentNode.left;
            }
            else {
                currentNode = currentNode.right;
            }
            align = (align + 1) % this.k;
        }
        // didn't find the point
    }

    public List<PointType> within(IKDPoint point, double radius) {
        List<PointType> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) {
            Node currentNode = queue.poll();
            int align = currentNode.align;
            PointType currentPoint = currentNode.getPoint();
            // if current node within range, put it into result, and put both children to queue
            if (currentPoint.distanceTo(point) <= radius) {
                if (!currentNode.deleted) {
                    result.add(currentPoint);
                }
                // also add duplicates inside current node
                for (PointType duplicate: currentNode.getDuplicates()) {
                    result.add(duplicate);
                }
                if (currentNode.left != null) {
                    queue.add(currentNode.left);
                }
                if (currentNode.right != null) {
                    queue.add(currentNode.right);
                }
            }
            // else current node outside range
            else {
                // but if NOT (currentNode.x + r) < point.x, left child still needs to be checked
                if (currentPoint.getDimensionValue(align) + radius >= point.getDimensionValue(align)) {
                    if (currentNode.left != null) {
                        queue.add(currentNode.left);
                    }
                }
                // but if NOT point.x < (currentNode.x - r), right child still needs to be checked
                if (point.getDimensionValue(align) >= currentPoint.getDimensionValue(align) - radius) {
                    if (currentNode.right != null) {
                        queue.add(currentNode.right);
                    }
                }
            }
        }
        return result;
    }

    public List<PointType> range(IKDPoint leftBottom, IKDPoint rightTop) {
        List<PointType> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) {
            Node currentNode = queue.poll();
            int align = currentNode.align;
            PointType currentPoint = currentNode.getPoint();
            // if current node within range, put it into result, and put both children to queue
            if (currentPoint.rightTo(leftBottom) && currentPoint.leftTo(rightTop)) {
                if (!currentNode.deleted) {
                    result.add(currentPoint);
                }
                // also add duplicates inside current node
                for (PointType duplicate: currentNode.getDuplicates()) {
                    result.add(duplicate);
                }
                if (currentNode.left != null) {
                    queue.add(currentNode.left);
                }
                if (currentNode.right != null) {
                    queue.add(currentNode.right);
                }
            }
            // else current node outside range
            else {
                if (rightTop.getDimensionValue(align) < currentPoint.getDimensionValue(align)) {
                    if (currentNode.left != null) {
                        queue.add(currentNode.left);
                    }
                }
                else if (leftBottom.getDimensionValue(align) > currentPoint.getDimensionValue(align)) {
                    if (currentNode.right != null) {
                        queue.add(currentNode.right);
                    }
                }
                // on the separation axis, node is between leftBottom and rightTop, both children need to be explored
                else {
                    if (currentNode.left != null) {
                        queue.add(currentNode.left);
                    }
                    if (currentNode.right != null) {
                        queue.add(currentNode.right);
                    }
                }
            }
        }
        return result;
    }

    public void print() {
        System.out.println("=================== KDTree ===================");
        Queue<Pair<Integer, Node>> queue = new LinkedList<>();
        queue.add(new Pair<>(0, root));
        int currentLevel = -1;
        while (queue.size() > 0) {
            Pair<Integer, Node> currentEntry = queue.poll();
            int level = currentEntry.getKey();
            Node currentNode = currentEntry.getValue();
            if (level > currentLevel) {
                System.out.println();
                System.out.print("[" + level + "] ");
                currentLevel = level;
            }
            System.out.print(currentNode.getPoint().getId());
            if (!currentNode.getDuplicates().isEmpty()) {
                System.out.print("[");
                for (PointType duplicate: currentNode.getDuplicates()) {
                    System.out.print(duplicate.getId() + ",");
                }
            }
            System.out.print(", ");
            if (currentNode.left != null) {
                queue.add(new Pair<>(level + 1, currentNode.left));
            }
            if (currentNode.right != null) {
                queue.add(new Pair<>(level + 1, currentNode.right));
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
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) {
            Node node = queue.poll();
            if (node.left != null) {
                System.out.println("\"[" + node.align + "] " + node.getPoint().getId() + "\" -> \"[" + node.left.align + "] " + node.left.getPoint().getId() + "\"");
                queue.add(node.left);
            }
            if (node.right != null) {
                System.out.println("\"[" + node.align + "] " + node.getPoint().getId() + "\" -> \"[" + node.right.align + "] " + node.right.getPoint().getId() + "\"");
                queue.add(node.right);
            }
        }
        System.out.println("}");
    }

    public int size() {
        return this.size;
    }
}
