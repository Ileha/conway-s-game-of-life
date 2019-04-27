package app.common.hashlife;

import app.common.IRule;

public class Node {
    public Node rightUpper;
    public Node leftUpper;

    public Node rightLower;
    public Node leftLower;

    private int level;
    private short alive;

    private IRule rule;

    public Node(IRule rule) {
        this.rule = rule;
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

    public void expandUniverse() {

        rightUpper = new Node(rule,
                new Node(rule, level), new Node(rule, level),
                new Node(rule, level), rightUpper
                );

        leftUpper = new Node(rule,
                new Node(rule, level), new Node(rule, level),
                leftUpper, new Node(rule, level)
                );

        rightLower = new Node(rule,
                new Node(rule, level), rightLower,
                new Node(rule, level), new Node(rule, level)
                );

        leftLower = new Node(rule,
                leftLower, new Node(rule, level),
                new Node(rule, level), new Node(rule, level)
                );
        level++;
    }
//
//    public Node nextStep() {
//
//    }


    @Override
    public String toString() {
        if (level == 0) {
            return String.valueOf(alive);
        }

        //StringBuilder sb = new StringBuilder();
        return String.format("[%s, %s, %s, %s]", rightUpper, leftUpper, rightLower, leftLower);
    }
}
