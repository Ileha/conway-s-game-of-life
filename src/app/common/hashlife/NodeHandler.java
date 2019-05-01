package app.common.hashlife;

import app.common.IRule;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;

public abstract class NodeHandler implements IRule {

    protected ThreadLocal<RuleInfo> ruleInfoByThread;   //информация для тестов
    private Map<Node, Node>         cash;
    private TIntObjectHashMap<Node> zeroPlanes;
    private Map<Node, Node>         simpleNods;         //ноды 2x2

    public NodeHandler() {
        simpleNods = new HashMap<>(16);    //может быть не больше 16
        zeroPlanes = new TIntObjectHashMap<>();
        cash = new HashMap<>(Short.MAX_VALUE*2);
        ruleInfoByThread = new ThreadLocal<RuleInfo>() {
            @Override
            protected RuleInfo initialValue() {
                return new RuleInfo();
            }
        };
    }

    public void printCashSize() {
        System.out.printf("cash size %s elements;\tsimple elements %s;\tzero cash size %s;\n",
                cash.size(), simpleNods.size(), zeroPlanes.size());
    }

    protected Node getZeroByLevel(int level) {
        Node res = zeroPlanes.get(level);
        if (res == null) {
            res = getSimpleZeroByLevel(level);
            zeroPlanes.put(level, res);
        }
        return res;
    }
    private Node getSimpleZeroByLevel(int level) {
        if (level == 1) {
            return checkSimpleHash(Node.createEmptyNode((short) 1));
        }
        return Node.uniteAll(
                getSimpleZeroByLevel(level-1),
                getSimpleZeroByLevel(level-1),
                getSimpleZeroByLevel(level-1),
                getSimpleZeroByLevel(level-1)
        );
    }

    /*
     * проверяет есть ли среди нодов 2x2 уже созданный
     * если есть то отдаёт его в качестве результата,
     * иначе добавляет в ноды 2x2
     */
    protected Node checkSimpleHash(Node node) {
        if (node.getLevel() != 1) {
            return node;
        }

        Node simpleRes = simpleNods.get(node);
        if (simpleRes != null) {
            return simpleRes;
        }
        else {
            simpleNods.put(node, node);
            return node;
        }
    }


    protected Node handleNodeSide4(Node node) {
        /*
         * row 0   0  0  0 0 - x
         * row 1   0 c0 с1 0
         * row 2   0 с2 с3 0
         * row 3   0  0  0 0
         *         |
         *         y
         */
        Node res = cash.get(node);
        if (res != null) {
            return res;
        }

        RuleInfo currentThreadRI = ruleInfoByThread.get();
        Arrays.fill(currentThreadRI.counts,  (short) 0);
        Arrays.fill(currentThreadRI.states,  (short) 0);

        if (node.leftUpper.leftUpper.getAlive() == 1) {
            currentThreadRI.counts[0]+=1;
        }
        if (node.leftUpper.rightUpper.getAlive() == 1) {
            currentThreadRI.counts[0]+=1;
            currentThreadRI.counts[1]+=1;
        }
        if (node.leftUpper.leftLower.getAlive() == 1) {
            currentThreadRI.counts[0]+=1;
            currentThreadRI.counts[2]+=1;
        }
        if (node.leftUpper.rightLower.getAlive() == 1) {//cell 0
            currentThreadRI.states[0] = 1;

            currentThreadRI.counts[1]+=1;
            currentThreadRI.counts[2]+=1;
            currentThreadRI.counts[3]+=1;
        }


        if (node.rightUpper.leftUpper.getAlive() == 1) {
            currentThreadRI.counts[0]+=1;
            currentThreadRI.counts[1]+=1;
        }
        if (node.rightUpper.rightUpper.getAlive() == 1) {
            currentThreadRI.counts[1]+=1;
        }
        if (node.rightUpper.leftLower.getAlive() == 1) {//cell 1
            currentThreadRI.states[1] = 1;

            currentThreadRI.counts[0]+=1;
            currentThreadRI.counts[2]+=1;
            currentThreadRI.counts[3]+=1;
        }
        if (node.rightUpper.rightLower.getAlive() == 1) {
            currentThreadRI.counts[1]+=1;
            currentThreadRI.counts[3]+=1;
        }


        if (node.leftLower.leftUpper.getAlive() == 1) {
            currentThreadRI.counts[0]+=1;
            currentThreadRI.counts[2]+=1;
        }
        if (node.leftLower.rightUpper.getAlive() == 1) {//cell 2
            currentThreadRI.states[2] = 1;

            currentThreadRI.counts[0]+=1;
            currentThreadRI.counts[1]+=1;
            currentThreadRI.counts[3]+=1;
        }
        if (node.leftLower.leftLower.getAlive() == 1) {
            currentThreadRI.counts[2]+=1;
        }
        if (node.leftLower.rightLower.getAlive() == 1) {
            currentThreadRI.counts[2]+=1;
            currentThreadRI.counts[3]+=1;
        }


        if (node.rightLower.leftUpper.getAlive() == 1) {//cell 3
            currentThreadRI.states[3] = 1;

            currentThreadRI.counts[0]+=1;
            currentThreadRI.counts[1]+=1;
            currentThreadRI.counts[2]+=1;
        }
        if (node.rightLower.rightUpper.getAlive() == 1) {
            currentThreadRI.counts[1]+=1;
            currentThreadRI.counts[3]+=1;
        }
        if (node.rightLower.leftLower.getAlive() == 1) {
            currentThreadRI.counts[2]+=1;
            currentThreadRI.counts[3]+=1;
        }
        if (node.rightLower.rightLower.getAlive() == 1) {
            currentThreadRI.counts[3]+=1;
        }

        res = Node.createEmptyNode((short) 1);
        for (int i = 0; i < currentThreadRI.counts.length; i++) {
            if (rule(currentThreadRI.counts[i], currentThreadRI.states[i]) > 0) {
                int nx = i%2;
                int ny = Math.floorDiv(i, 2);
                res.setCell(nx, ny);
            }
        }
        res = checkSimpleHash(res);

        cash.put(node, res);

        return res;
    }
}
