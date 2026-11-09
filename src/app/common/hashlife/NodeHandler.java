package app.common.hashlife;

import app.common.IRule;

import java.util.ArrayList;

/*
* хранит все ноды в одной хеш-таблице (ключ - четыре дочерних нода, сравниваются через ==),
* чтобы одинаковые квадраты были одним объектом
* не потокобезопасен: ноды создаются только из одного потока за раз
*
* таблица - это и кэш шагов, поэтому при переполнении из неё убираются давно не использованные ноды,
* а не все кроме текущего мира: шаблон повторяет прошлые состояния, и их результаты нужны снова
*/
public abstract class NodeHandler implements IRule {
    /*
    * сколько нодов держать в таблице (~48 байт на нод), задаётся -Dhashlife.maxNodes=...
    * больше - меньше пересчёта, но дольше полные сборки мусора JVM
    */
    private static final int    DEFAULT_MAX_NODES = 4 << 20;
    private static final int    AGE_BUCKETS = 1 << 16;    //возраст нода в шагах для выбора, что оставить

    private final Node[]        leaves = { new Node(0), new Node(1) };
    private final ArrayList<Node> zeroPlanes = new ArrayList<>();//пустые ноды по уровню

    private Node[]              table;
    private int                 size;
    private final int           maxSize;//при этом размере таблица чистится от давно не использованных нодов

    int                         generation;//номер шага, ноды помечаются им при использовании

    public NodeHandler() {
        //не больше четверти кучи (~64 байта на нод вместе с таблицей)
        long byHeap = Runtime.getRuntime().maxMemory() / 256;
        maxSize = (int) Math.min(Long.getLong("hashlife.maxNodes", DEFAULT_MAX_NODES), byHeap);

        //таблица создаётся сразу полного размера, чтобы не было пауз на её увеличение
        table = new Node[Integer.highestOneBit(maxSize + maxSize / 3 - 1) << 1];
        zeroPlanes.add(leaves[0]);
    }

    public void printCashSize() {
        System.out.printf("nodes %s (max %s);\tzero cash size %s;\n", size, maxSize, zeroPlanes.size());
    }

    public void nextGeneration() {
        generation++;
    }

    protected Node getLeaf(int alive) {
        return leaves[alive];
    }

    protected Node getZeroByLevel(int level) {
        while (zeroPlanes.size() <= level) {
            Node sub = zeroPlanes.get(zeroPlanes.size() - 1);
            zeroPlanes.add(find(sub, sub, sub, sub));
        }
        return zeroPlanes.get(level);
    }

    private static int hash(Node lU, Node rU, Node lL, Node rL) {
        int h = lU.hash * 0x9E3779B1 + rU.hash;
        h = h * 0x85EBCA77 + lL.hash;
        h = h * 0xC2B2AE3D + rL.hash;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        return h ^ (h >>> 12);
    }

    /*
    * возвращает единственный нод с такими дочерними нодами, при необходимости создаёт его
    */
    protected Node find(Node lU, Node rU, Node lL, Node rL) {
        int h = hash(lU, rU, lL, rL);
        int index = h & (table.length - 1);

        for (Node n = table[index]; n != null; n = n.chain) {
            if (n.hash == h && n.leftUpper == lU && n.rightUpper == rU && n.leftLower == lL && n.rightLower == rL) {
                n.lastUsed = generation;
                return n;
            }
        }

        Node res = new Node(lU, rU, lL, rL, h);
        res.lastUsed = generation;
        res.chain = table[index];
        table[index] = res;

        //чистка только между шагами, поэтому внутри большого шага таблица может переполниться
        if (++size > table.length) {
            resize(table.length << 1);
        }
        return res;
    }

    private void resize(int capacity) {
        Node[] old = table;
        table = new Node[capacity];
        for (Node head : old) {
            for (Node n = head; n != null; ) {
                Node next = n.chain;
                int index = n.hash & (capacity - 1);
                n.chain = table[index];
                table[index] = n;
                n = next;
            }
        }
    }

    /*
    * если таблица заполнена, оставляет в ней примерно половину нодов - использованные последними,
    * вызывать только когда шаг не вычисляется
    */
    public void collectGarbageIfNeeded(Node root) {
        if (size >= maxSize) {
            collectGarbage(root, recentCutoff());
        }
    }

    //оставляет в таблице только root и пустые ноды (например после загрузки)
    public void collectAllGarbage(Node root) {
        collectGarbage(root, generation + 1);
    }

    /*
    * минимальный lastUsed, при котором нодов не больше половины maxSize
    */
    private int recentCutoff() {
        int[] countByAge = new int[AGE_BUCKETS];
        for (Node head : table) {
            for (Node n = head; n != null; n = n.chain) {
                int age = Math.max(0, generation - n.lastUsed);
                countByAge[Math.min(age, AGE_BUCKETS - 1)]++;
            }
        }

        int kept = 0;
        int window = 0;
        while (window < AGE_BUCKETS && kept + countByAge[window] <= maxSize / 2) {
            kept += countByAge[window];
            window++;
        }
        return generation - window + 1;
    }

    /*
    * оставляет ноды с lastUsed >= cutoff, root и пустые ноды,
    * а также всё, на что они ссылаются (дочерние ноды и результаты шагов), чтобы ноды оставались единственными
    * таблица не пересоздаётся: размер тот же, поэтому корзины только фильтруются
    */
    private void collectGarbage(Node root, int cutoff) {
        long start = System.nanoTime();
        int before = size;

        keep(root, cutoff);
        for (Node zero : zeroPlanes) {
            keep(zero, cutoff);
        }
        for (Node head : table) {
            for (Node n = head; n != null; n = n.chain) {
                if (n.lastUsed >= cutoff) {
                    keepReferenced(n, cutoff);
                }
            }
        }

        size = 0;
        for (int i = 0; i < table.length; i++) {
            Node kept = null;
            for (Node n = table[i]; n != null; ) {
                Node next = n.chain;
                if (n.lastUsed >= cutoff) {
                    n.chain = kept;
                    kept = n;
                    size++;
                }
                else {
                    n.chain = null;
                }
                n = next;
            }
            table[i] = kept;
        }

        System.out.printf("hashlife gc: nodes %s -> %s, kept last %s generations, %.0f ms\n",
                before, size, generation - cutoff + 1, (System.nanoTime() - start) / 1e6);
    }

    private static void keep(Node node, int cutoff) {
        if (node.getLevel() > 0 && node.lastUsed < cutoff) {
            node.lastUsed = cutoff;
            keepReferenced(node, cutoff);
        }
    }

    private static void keepReferenced(Node node, int cutoff) {
        keep(node.leftUpper, cutoff);
        keep(node.rightUpper, cutoff);
        keep(node.leftLower, cutoff);
        keep(node.rightLower, cutoff);
        if (node.result != null) {
            keep(node.result, cutoff);
        }
    }

    /*
     * row 0   0  0  0 0 - x
     * row 1   0 c0 с1 0
     * row 2   0 с2 с3 0
     * row 3   0  0  0 0
     *         |
     *         y
     *
     * возвращает центр 2x2 квадрата 4x4 через один шаг
     */
    protected Node handleNodeSide4(Node node) {
        return find(
                nextCell(node, 1, 1), nextCell(node, 2, 1),
                nextCell(node, 1, 2), nextCell(node, 2, 2));
    }

    private Node nextCell(Node node, int x, int y) {
        short count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dy != 0) {
                    count += cell(node, x + dx, y + dy);
                }
            }
        }
        return leaves[rule(count, (short) cell(node, x, y)) > 0 ? 1 : 0];
    }

    //клетка (x, y) нода 4x4
    private static int cell(Node node, int x, int y) {
        Node quarter = y < 2 ? (x < 2 ? node.leftUpper : node.rightUpper)
                             : (x < 2 ? node.leftLower : node.rightLower);
        x &= 1;
        y &= 1;
        Node leaf = y == 0 ? (x == 0 ? quarter.leftUpper : quarter.rightUpper)
                           : (x == 0 ? quarter.leftLower : quarter.rightLower);
        return leaf.getAlive();
    }
}
