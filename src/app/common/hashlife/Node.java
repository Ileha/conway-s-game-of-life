package app.common.hashlife;

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

    private short               level;
    private int                 alive;

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

        res.alive = lU.alive + rU.alive + lL.alive + rL.alive;
        res.level = (short) (lU.level+1);

        if (res.level == 1) {
            res = handler.checkSimpleHash(res);
        }
        return res;
    }

    public static Node createEmptyNode(short level) {
        Node res = null;
        if (level == 0) {
            res = new Node();
            res.alive = 0;
            res.level = 0;
            return res;
        }

        short nextLevel = (short) (level-1);
        res = new Node();
        res.level = level;

        res.leftUpper = createEmptyNode(nextLevel);
        res.rightUpper = createEmptyNode(nextLevel);

        res.leftLower = createEmptyNode(nextLevel);
        res.rightLower = createEmptyNode(nextLevel);

        return res;
    }

    public static Node hashAll(Node node) {
        if (node.alive == 0) {
            return handler.getZeroByLevel(node.level);//пустые ноды уже общие и захешированы
        }
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
    * не изменяет текущий нод, возвращает новый
    */
    public Node expandUniverse() {

        Node subNode = handler.getZeroByLevel(level-1);

        return Node.uniteAll(
                Node.uniteAll(
                        subNode, subNode,
                        subNode, leftUpper),
                Node.uniteAll(
                        subNode,    subNode,
                        rightUpper, subNode),
                Node.uniteAll(
                        subNode, leftLower,
                        subNode, subNode),
                Node.uniteAll(
                        rightLower, subNode,
                        subNode,    subNode)
        );
    }

    /*
     * увеличивает мир в 4 раза и существующий помещает в левый верхний квадрат
     * пустые квадраты общие (из кэша нулевых нодов), поэтому возвращает новый нод
     */
    public Node relativeExpandUniverse() {
        Node zero = handler.getZeroByLevel(level);
        return Node.uniteAll(
                this, zero,
                zero, zero);
    }

    /*
     * не изменяет существующие ноды (они могут быть общими),
     * копирует путь до клетки и возвращает новый нод
     */
    public Node setCell(int x, int y) {
        if (level == 0) {
            Node leaf = new Node();
            leaf.alive = 1;
            return leaf;
        }

        int half = 1 << (level-1);
        Node res = new Node(this);

        if (y < half) {
            if (x < half) { res.leftUpper = leftUpper.setCell(x, y); }
            else          { res.rightUpper = rightUpper.setCell(x-half, y); }
        }
        else {
            if (x < half) { res.leftLower = leftLower.setCell(x, y-half); }
            else          { res.rightLower = rightLower.setCell(x-half, y-half); }
        }

        res.alive = res.leftUpper.alive + res.rightUpper.alive +
                    res.leftLower.alive + res.rightLower.alive;
        return res;
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
            return alive;
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
