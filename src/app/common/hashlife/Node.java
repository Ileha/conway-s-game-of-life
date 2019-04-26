package app.common.hashlife;

import app.common.IRule;

public class Node {
    public Node rightUpper;
    public Node leftUpper;

    public Node rightLower;
    public Node leftLower;

    public int level;

    private IRule rule;

    public Node(IRule rule) {
        this.rule = rule;
    }

    public Node(IRule rule, int level) {
        this(rule);
        this.level = level;

        if (level == 0) {
            this.rule = rule;
            return;
        }

        int nextLevel = level-1;

        rightUpper = new Node(rule, nextLevel);
        leftUpper = new Node(rule, nextLevel);

        rightLower = new Node(rule, nextLevel);
        leftLower = new Node(rule, nextLevel);
    }

    public Node(IRule rule, Node rU, Node lU, Node rL, Node lL) {
        rightUpper = rU;
        leftUpper = lU;

        rightLower = rL;
        leftLower = lL;
    }

    public Node expandUniverse() {

    }

    public Node nextStep() {

    }
}
