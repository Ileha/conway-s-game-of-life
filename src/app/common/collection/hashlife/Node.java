package app.common.collection.hashlife;

/*
* leftUpper rightUpper
* leftLower rightLower
*/
public class Node {
    private static NodeHandler  handler;//обработчик блока клеток

    public Node                 leftUpper;
    public Node                 rightUpper;

    public Node                 leftLower;
    public Node                 rightLower;

    private int                 level;
    private short               alive;

    public Node() {}
    public Node(Node original) {
        leftUpper = original.leftUpper;
        rightUpper = original.rightUpper;
        leftLower = original.leftLower;
        rightLower = original.rightLower;

        alive = original.alive;
        level = original.level;
    }

    public static Node uniteAll(Node lU, Node rU, Node lL, Node rL) {
        Node res = new Node();
        res.leftUpper = lU;
        res.rightUpper = rU;
        res.leftLower = lL;
        res.rightLower = rL;

        res.alive = (short) (lU.alive + rU.alive + lL.alive + rL.alive);
        res.level = lU.level+1;

        if (res.level == 1) {
            res = handler.checkSimpleHash(res);
        }
        return res;
    }

    public static Node createEmptyNode(int level) {
        Node res = null;
        if (level == 0) {
            res = new Node();
            res.alive = 0;
            res.level = 0;
            return res;
        }

        int nextLevel = level-1;
        res = new Node();
        res.level = level;

        res.leftUpper = createEmptyNode(nextLevel);
        res.rightUpper = createEmptyNode(nextLevel);

        res.leftLower = createEmptyNode(nextLevel);
        res.rightLower = createEmptyNode(nextLevel);

        return res;
    }

    public static Node hashAll(Node node) {
        if (node.level == 1) {
            return handler.checkSimpleHash(node);
        }
        else {
            node.leftUpper = hashAll(node.leftUpper);
            node.rightUpper = hashAll(node.rightUpper);
            node.leftLower = hashAll(node.leftLower);
            node.rightLower = hashAll(node.rightLower);
            return node;
        }
    }

    public static void setHandler(NodeHandler newHandler) {
        handler = newHandler;
    }

    public int sideSize() {
        return (int) Math.pow(2, level);
    }

    public int getLevel() {
        return level;
    }

    public int getAlive() {
        return alive;
    }

    /*
    * увеличивает мир в 4 раза и существующий помещает в центр
    * !!! края являются одним и тем же нодом
    */
    public void expandUniverse() {

        Node subNode = handler.getZeroByLevel(level-1);

        leftUpper = Node.uniteAll(
                subNode, subNode,
                subNode, leftUpper
        );


        rightUpper = Node.uniteAll(
                subNode,    subNode,
                rightUpper, subNode
                );

        leftLower = Node.uniteAll(
                subNode, leftLower,
                subNode, subNode
        );

        rightLower = Node.uniteAll(
                rightLower, subNode,
                subNode,    subNode
                );

        level++;
    }

    /*
     * увеличивает мир в 4 раза и существующий помещает в левый верхний квадрат
     */
    public void relativeExpandUniverse() {
        leftUpper = Node.uniteAll(
                leftUpper, rightUpper,
                leftLower, rightLower);
        rightUpper = Node.createEmptyNode(level);
        leftLower = Node.createEmptyNode(level);
        rightLower = Node.createEmptyNode(level);

        level++;
    }

    public void setCell(int x, int y) {
        if (level == 0) {
            alive = (short)1;
            return;
        }
        else {
            alive++;
        }

        int sideNextSquare = (int) Math.pow(2, level-1);
        Node nextNode = leftUpper;
        int xOffset = 0;
        int yOffset = 0;

        if (x >= sideNextSquare) {
            xOffset = sideNextSquare;
            nextNode = rightUpper;
            if (y >= sideNextSquare) {
                yOffset = sideNextSquare;
                nextNode = rightLower;
            }
        }
        else {
            if (y >= sideNextSquare) {
                yOffset = sideNextSquare;
                nextNode = leftLower;
            }
        }

        nextNode.setCell(x-xOffset, y-yOffset);
    }


    /*
     * leftUpper rightUpper     leftUpper rightUpper
     * leftLower rightLower     leftLower rightLower
     *
     * leftUpper rightUpper     leftUpper rightUpper
     * leftLower rightLower     leftLower rightLower
     */
    public Node nextStep() {

        if (alive == 0) {
            return handler.getZeroByLevel(level-1);
        }
        /*
        * квадрат со стороной 4
        */
        if (level == 2) {
            return handler.handleNodeSide4(this);
        }

        Node n0 = leftUpper.centeredSubnode();
        Node n1 = centeredHorizontal(leftUpper, rightUpper);
        Node n2 = rightUpper.centeredSubnode();

        Node n3 = centeredVertical(leftUpper, leftLower);
        Node n4 = centeredSubSubnode();
        Node n5 = centeredVertical(rightUpper, rightLower);

        Node n6 = leftLower.centeredSubnode();
        Node n7 = centeredHorizontal(leftLower, rightLower);
        Node n8 = rightLower.centeredSubnode();

        /*
        * 0 1 2
        * 3 4 5
        * 6 7 8
        */

        return Node.uniteAll(
                Node.uniteAll(n0, n1, n3, n4).nextStep(),
                Node.uniteAll(n1, n2, n4, n5).nextStep(),
                Node.uniteAll(n3, n4, n6, n7).nextStep(),
                Node.uniteAll(n4, n5, n7, n8).nextStep()
        );
    }

    /*
    * создаёт новый узел который включает только центр текущего
    */
    public Node centeredSubnode() {
        return Node.uniteAll(
                leftUpper.rightLower,
                rightUpper.leftLower,
                leftLower.rightUpper,
                rightLower.leftUpper);
    }

    /*
     * !!! создаёт новый узел который включает только центр центра текущего
     */
    public Node centeredSubSubnode() {
        return Node.uniteAll(
                leftUpper.rightLower.rightLower,
                rightUpper.leftLower.leftLower,
                leftLower.rightUpper.rightUpper,
                rightLower.leftUpper.leftUpper);
    }

    /*
     * создаёт новый узел который включает правую часть left и левую часть right
     */
    static Node centeredHorizontal(Node left, Node right) {
        return Node.uniteAll(
                left.rightUpper.rightLower,
                right.leftUpper.leftLower,
                left.rightLower.rightUpper,
                right.leftLower.leftUpper);
    }

    /*
     * создаёт новый узел который включает нижнюю часть upper и верхнюю часть lower
     */
    static Node centeredVertical(Node upper, Node lower) {
        return Node.uniteAll(
                upper.leftLower.rightLower,
                upper.rightLower.leftLower,
                lower.leftUpper.rightUpper,
                lower.rightUpper.leftUpper);
    }

    /*@Override
    public String toString() {
        if (level == 0) {
            return String.valueOf(alive);
        }
        return String.format("\n[%s, %s, %s, %s]", leftUpper, rightUpper, leftLower, rightLower);
    }*/

    @Override
    public int hashCode() {

        if (level == 0) {
            return (int) alive;
        }
        else if (level == 1) {
            return  leftUpper.hashCode() +
                    11 * rightUpper.hashCode() +
                    101 * leftLower.hashCode() +
                    1007 * rightLower.hashCode();
        }
        return System.identityHashCode(leftUpper) +
                11 * System.identityHashCode(rightUpper) +
                101 * System.identityHashCode(leftLower) +
                1007 * System.identityHashCode(rightLower) ;
    }

    @Override
    public boolean equals(Object o) {
        Node t = (Node)o;
        if (level != t.level)
            return false;
        if (level == 0) {
            return alive == t.alive;
        }
        else if (level == 1) {
            return (leftUpper.equals(leftUpper) &&
                    rightUpper.equals(t.rightUpper) &&
                    leftLower.equals(t.leftLower) &&
                    rightLower.equals(t.rightLower));
        }
        return (leftUpper == t.leftUpper &&
                rightUpper == t.rightUpper &&
                leftLower  == t.leftLower &&
                rightLower == t.rightLower);
    }
}
