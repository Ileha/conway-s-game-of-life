package app.common.hashlife;

import app.common.IRule;

/*
* leftUpper rightUpper
* leftLower rightLower
*/
public class Node {
    public Node                     rightUpper;
    public Node                     leftUpper;

    public Node                     rightLower;
    public Node                     leftLower;

    private int                     level;
    private short                   alive;

    private IRule                   rule;
    protected ThreadLocal<RuleInfo> ruleInfoByThread;//информация для тестов

    public Node(IRule rule) {
        this.rule = rule;
        ruleInfoByThread = new ThreadLocal<RuleInfo>() {
            @Override
            protected RuleInfo initialValue() {
                return new RuleInfo();
            }
        };
    }

    public Node(IRule rule, int level) {
        this(rule);
        this.level = level;

        if (level == 0) {
            this.rule = rule;
            this.alive = 0;
            return;
        }

        int nextLevel = level-1;

        rightUpper = new Node(rule, nextLevel);
        leftUpper = new Node(rule, nextLevel);

        rightLower = new Node(rule, nextLevel);
        leftLower = new Node(rule, nextLevel);
    }

    public Node(IRule rule, Node rU, Node lU, Node rL, Node lL) {
        this(rule);
        rightUpper = rU;
        leftUpper = lU;

        rightLower = rL;
        leftLower = lL;

        level = rU.level+1;
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

    public void expandUniverse() {

        rightUpper = new Node(rule,
                new Node(rule, level-1), new Node(rule, level-1),
                new Node(rule, level-1), rightUpper
                );

        leftUpper = new Node(rule,
                new Node(rule, level-1), new Node(rule, level-1),
                leftUpper, new Node(rule, level-1)
                );

        rightLower = new Node(rule,
                new Node(rule, level-1), rightLower,
                new Node(rule, level-1), new Node(rule, level-1)
                );

        leftLower = new Node(rule,
                leftLower, new Node(rule, level-1),
                new Node(rule, level-1), new Node(rule, level-1)
                );
        level++;
    }

    public void setCell(int x, int y) {
        if (level == 0) {
            alive = (short)1;
            return;
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

        /*
        * квадрат со стороной 4
        */
        if (level == 2) {
            /*
             * row 0   0  0  0 0
             * row 1   0 c0 с1 0
             * row 2   0 с2 с3 0 - x
             * row 3   0  0  0 0
             *            |
             *            y
             */
            RuleInfo currentThreadRI = ruleInfoByThread.get();
            if (leftUpper.leftUpper.alive == 1) {
                currentThreadRI.counts[0]+=1;
            }
            if (leftUpper.rightUpper.alive == 1) {
                currentThreadRI.counts[0]+=1;
                currentThreadRI.counts[1]+=1;
            }
            if (leftUpper.rightLower.alive == 1) {
                currentThreadRI.counts[0]+=1;
                currentThreadRI.counts[2]+=1;
            }
            if (leftUpper.rightLower.alive == 1) {//cell 0
                currentThreadRI.states[0] = 1;

                currentThreadRI.counts[1]+=1;
                currentThreadRI.counts[2]+=1;
                currentThreadRI.counts[3]+=1;
            }

            if (rightUpper.leftUpper.alive == 1) {
                currentThreadRI.counts[0]+=1;
                currentThreadRI.counts[1]+=1;
            }
            if (rightUpper.rightUpper.alive == 1) {
                currentThreadRI.counts[1]+=1;
            }
            if (rightUpper.rightLower.alive == 1) {//cell 1
                currentThreadRI.states[1] = 1;

                currentThreadRI.counts[0]+=1;
                currentThreadRI.counts[2]+=1;
                currentThreadRI.counts[3]+=1;
            }
            if (rightUpper.rightLower.alive == 1) {
                currentThreadRI.counts[1]+=1;
                currentThreadRI.counts[3]+=1;
            }

            if (leftLower.leftUpper.alive == 1) {
                currentThreadRI.counts[0]+=1;
                currentThreadRI.counts[2]+=1;
            }
            if (leftLower.rightUpper.alive == 1) {//cell 2
                currentThreadRI.states[2] = 1;

                currentThreadRI.counts[0]+=1;
                currentThreadRI.counts[1]+=1;
                currentThreadRI.counts[3]+=1;
            }
            if (leftLower.rightLower.alive == 1) {
                currentThreadRI.counts[2]+=1;
            }
            if (leftLower.rightLower.alive == 1) {
                currentThreadRI.counts[2]+=1;
                currentThreadRI.counts[3]+=1;
            }

            if (leftLower.leftUpper.alive == 1) {//cell 3
                currentThreadRI.states[3] = 1;

                currentThreadRI.counts[0]+=1;
                currentThreadRI.counts[1]+=1;
                currentThreadRI.counts[2]+=1;
            }
            if (leftLower.rightUpper.alive == 1) {
                currentThreadRI.counts[1]+=1;
                currentThreadRI.counts[3]+=1;
            }
            if (leftLower.rightLower.alive == 1) {
                currentThreadRI.counts[2]+=1;
                currentThreadRI.counts[3]+=1;
            }
            if (leftLower.rightLower.alive == 1) {
                currentThreadRI.counts[3]+=1;
            }

            Node res = new Node(rule, 2);
            for (int i = 0; i < currentThreadRI.counts.length; i++) {
                if (rule.rule(currentThreadRI.counts[i], currentThreadRI.states[i]) > 0) {
                    int nx = i%2;
                    int ny = Math.floorDiv(i, 2);
                    res.setCell(nx, ny);
                }
            }

            return res;
        }

        Node n0 = leftUpper.nextStep();
        Node n1 = centeredHorizontal(leftUpper, rightUpper).nextStep();
        Node n2 = rightUpper.nextStep();

        Node n3 = centeredVertical(leftUpper, leftLower).nextStep();
        Node n4 = centeredSubnode().nextStep();
        Node n5 = centeredVertical(rightUpper, rightLower).nextStep();

        Node n6 = leftLower.nextStep();
        Node n7 = centeredHorizontal(leftLower, rightLower).nextStep();
        Node n8 = rightLower.nextStep();


        /*
        * 0 1 2
        * 3 4 5
        * 6 7 8
        */
        return new Node(
                rule,
                uniteNodeInCentere(n0, n1, n3, n4),
                uniteNodeInCentere(n1, n2, n4, n5),
                uniteNodeInCentere(n3, n4, n6, n7),
                uniteNodeInCentere(n4, n5, n7, n8)
        ) ;
    }


    @Override
    public String toString() {
        if (level == 0) {
            return String.valueOf(alive);
        }

        //StringBuilder sb = new StringBuilder();
        return String.format("\n[%s, %s, %s, %s]", leftUpper, rightUpper, leftLower, rightLower);
    }

    public Node centeredSubnode() {
        return new Node(rule,
                leftUpper.rightLower,
                rightUpper.leftLower,
                leftLower.rightUpper,
                rightLower.leftUpper);
    }

    static Node centeredHorizontal(Node left, Node right) {
        return new Node(right.rule,
                left.rightUpper,
                right.leftUpper,
                left.rightLower,
                right.leftLower);
    }

    static Node centeredVertical(Node upper, Node lower) {
        return new Node(upper.rule,
                upper.leftLower,
                upper.rightLower,
                lower.leftUpper,
                lower.rightUpper);
    }

    static Node uniteNodeInCentere(Node rightUpper,
                                   Node leftUpper,
                                   Node rightLower,
                                   Node leftLower)
    {
        return new Node(rightUpper.rule,
                leftUpper.rightUpper,
                rightUpper.leftLower,
                leftLower.rightUpper,
                rightLower.leftUpper);
    }
}
