package app.common.hashlife;

/*
* leftUpper rightUpper
* leftLower rightLower
*
* ноды неизменяемые и уникальные: одинаковые квадраты - это один и тот же объект,
* поэтому сравниваются через ==, а следующий шаг каждого нода считается только один раз.
* создаются только через NodeHandler
*/
public class Node {
    private static NodeHandler  handler;//обработчик блока клеток

    public final Node           leftUpper;
    public final Node           rightUpper;

    public final Node           leftLower;
    public final Node           rightLower;

    private final int           level;
    private final int           alive;
    final int                   hash;

    Node                        result;//центр нода через один шаг (кэш nextStep)
    Node                        chain; //следующий нод в той же корзине хеш-таблицы NodeHandler
    int                         lastUsed;//последний шаг, на котором нод был найден или взят его результат

    //лист (одна клетка)
    Node(int alive) {
        leftUpper = rightUpper = leftLower = rightLower = null;
        level = 0;
        this.alive = alive;
        hash = alive;
    }

    Node(Node lU, Node rU, Node lL, Node rL, int hash) {
        leftUpper = lU;
        rightUpper = rU;
        leftLower = lL;
        rightLower = rL;

        level = lU.level + 1;
        alive = lU.alive + rU.alive + lL.alive + rL.alive;
        this.hash = hash;
    }

    public static Node uniteAll(Node lU, Node rU, Node lL, Node rL) {
        return handler.find(lU, rU, lL, rL);
    }

    public static void setHandler(NodeHandler newHandler) {
        handler = newHandler;
    }

    public int sideSize() {
        return 1 << level;
    }

    public int getLevel() {
        return level;
    }

    public int getAlive() {
        return alive;
    }

    /*
    * увеличивает мир в 4 раза и существующий помещает в центр
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
     */
    public Node relativeExpandUniverse() {
        Node zero = handler.getZeroByLevel(level);
        return Node.uniteAll(
                this, zero,
                zero, zero);
    }

    /*
     * не изменяет существующие ноды (они общие),
     * собирает новый путь до клетки и возвращает новый нод
     */
    public Node setCell(int x, int y) {
        if (level == 0) {
            return handler.getLeaf(1);
        }

        int half = 1 << (level-1);
        Node lU = leftUpper, rU = rightUpper, lL = leftLower, rL = rightLower;

        if (y < half) {
            if (x < half) { lU = lU.setCell(x, y); }
            else          { rU = rU.setCell(x-half, y); }
        }
        else {
            if (x < half) { lL = lL.setCell(x, y-half); }
            else          { rL = rL.setCell(x-half, y-half); }
        }

        return Node.uniteAll(lU, rU, lL, rL);
    }


    /*
     * возвращает центр нода (уровень на 1 меньше) через один шаг
     *
     * leftUpper rightUpper     leftUpper rightUpper
     * leftLower rightLower     leftLower rightLower
     *
     * leftUpper rightUpper     leftUpper rightUpper
     * leftLower rightLower     leftLower rightLower
     */
    public Node nextStep() {
        if (result != null) {
            lastUsed = handler.generation;
            return result;
        }

        if (alive == 0) {
            result = handler.getZeroByLevel(level-1);
        }
        /*
        * квадрат со стороной 4
        */
        else if (level == 2) {
            result = handler.handleNodeSide4(this);
        }
        else {
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

            result = Node.uniteAll(
                    Node.uniteAll(n0, n1, n3, n4).nextStep(),
                    Node.uniteAll(n1, n2, n4, n5).nextStep(),
                    Node.uniteAll(n3, n4, n6, n7).nextStep(),
                    Node.uniteAll(n4, n5, n7, n8).nextStep()
            );
        }
        return result;
    }

    /*
    * узел который включает только центр текущего
    */
    public Node centeredSubnode() {
        return Node.uniteAll(
                leftUpper.rightLower,
                rightUpper.leftLower,
                leftLower.rightUpper,
                rightLower.leftUpper);
    }

    /*
     * узел который включает только центр центра текущего
     */
    public Node centeredSubSubnode() {
        return Node.uniteAll(
                leftUpper.rightLower.rightLower,
                rightUpper.leftLower.leftLower,
                leftLower.rightUpper.rightUpper,
                rightLower.leftUpper.leftUpper);
    }

    /*
     * узел который включает правую часть left и левую часть right
     */
    static Node centeredHorizontal(Node left, Node right) {
        return Node.uniteAll(
                left.rightUpper.rightLower,
                right.leftUpper.leftLower,
                left.rightLower.rightUpper,
                right.leftLower.leftUpper);
    }

    /*
     * узел который включает нижнюю часть upper и верхнюю часть lower
     */
    static Node centeredVertical(Node upper, Node lower) {
        return Node.uniteAll(
                upper.leftLower.rightLower,
                upper.rightLower.leftLower,
                lower.leftUpper.rightUpper,
                lower.rightUpper.leftUpper);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
