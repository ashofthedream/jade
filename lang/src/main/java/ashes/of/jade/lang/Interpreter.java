package ashes.of.jade.lang;

import java.util.*;


public class Interpreter {

    public Deque<Node> eval(Deque<Node> nodes) {
        return eval(new ArrayDeque<>(), new HashMap<>(), nodes);
    }

    public Deque<Node> eval(Deque<Node> stack, Deque<Node> nodes) {
        return eval(stack, new HashMap<>(), nodes);
    }

    public Deque<Node> eval(Deque<Node> stack, Map<String, Node>  scope, Deque<Node> nodes) {
        System.out.println();
        System.out.printf("HEAD> %-40s %-60s %-60s %s%n", "NODE", "SCOPE", "STACK", "INPUT");
        System.out.printf("INPT> %-40s %-60s %-60s %s%n",     "", 0, stack, nodes);
        System.out.printf("");

        Iterator<Node> it = nodes.descendingIterator();

        while (it.hasNext()) {
            Node node = it.next();
            if (node.is(LexemType.NewLine) || node.is(LexemType.EOF)) {
                System.out.println();
                System.out.println();
                continue;
            }

            System.out.printf("eval> %-40s %-60s %-60s %s%n", node, scope, stack, nodes);

            if (node.is(LexemType.IntegerNumber) || node.is(LexemType.DoubleNumber) || node.is(LexemType.String)) {
                stack.push(node);
                continue;
            }

            if (node.is(LexemType.STORE)) {
                Node pop = stack.pop();
                scope.put(node.getContent(), pop);
            }

            if (node.is(LexemType.LOAD)) {
                Node pop = scope.get(node.getContent());
                stack.push(pop);
            }

            if (node.is(LexemType.Out)) {
                Node pop = stack.pop();
                System.out.println("\tOUT  \t\t" + pop);
            }


            if (node.is(LexemType.Map)) {
                Node lambda = stack.pop();
                Node seq = stack.pop();

                Node mapped = seq.isIntegerSeq() ?
                        map(seq.toIntegerSeq(), lambda) :
                        map(seq.toDoubleSeq(), lambda);

                stack.push(mapped);
            }

            if (node.is(LexemType.Reduce)) {
                Node lambda = stack.pop();
                Node n = stack.pop();
                Node seq = stack.pop();

                Node reduced = seq.isIntegerSeq() ?
                        reduce(seq.toIntegerSeq(), n, lambda) :
                        reduce(seq.toDoubleSeq(), n, lambda);

                stack.push(reduced);
            }

            if (node.is(LexemType.LAMBDA)) {
                stack.push(node);
            }

            if (node.is(LexemType.Seq)) {
                Node l = stack.pop();
                Node r = stack.pop();

                stack.push(new SeqNode(node.getLexem(), r, l));
            }

            if (node.is(LexemType.Print)) {
                Node pop = stack.pop();
                System.out.println("\tPRINT\t\t" + pop.toString());
            }

            if (node.getType().isOperator()) {
                Node l = stack.pop();
                Node r = stack.pop();
                switch (node.getType()) {
                    case Plus:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(r.toDouble() + l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode( r.toInteger() + l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " + " + r);

                    case Minus:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(r.toDouble() - l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(r.toInteger() - l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " - " + r);

                    case Multiply:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(r.toDouble() * l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(r.toInteger() * l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " * " + r);

                    case Divide:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(r.toDouble() / l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.toInteger() / l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " * " + r);

                    case Power:

                        stack.push(new DoubleNode(Math.pow(r.toDouble(), l.toDouble())));
                        break;

                }
            }
        }


        System.out.printf("RET > %-40s %-60s %s%n", "", scope, stack);
        System.out.println();
        System.out.println();
        return stack;
    }

    private Node map(IntegerSeqNode seq, Node lambda) {
        long[] l = null;
        double[] d = null;
        Deque<Node> stack = new ArrayDeque<>();
        for (int i = 0; i < seq.seq.length; i++) {
            stack.push(new IntNode(seq.seq[i]));
            eval(stack, lambda.getNodes());

            Node result = stack.pop();
            if (l == null && d == null) {
                if (result.isInteger())
                    l = new long[seq.seq.length];

                else if (result.isDouble())
                    d = new double[seq.seq.length];

                else
                    throw new IllegalStateException("Int or Double expected");
            }

            if (l != null && result.isInteger())
                l[i] = result.toInteger();

            else if (d != null && result.isDouble())
                d[i] = result.toDouble();

            else
                throw new IllegalStateException("Int or Double expected");
        }

        return l != null ?
                new IntegerSeqNode(l) : new DoubleSeqNode(d);
    }

    private Node map(DoubleSeqNode seq, Node lambda) {
        double[] d = new double[seq.seq.length];
        Deque<Node> stack = new ArrayDeque<>();
        for (int i = 0; i < seq.seq.length; i++) {
            stack.push(new DoubleNode(new Lexem(LexemType.DoubleNumber), seq.seq[i]));
            eval(stack, lambda.getNodes());

            Node result = stack.pop();
            if (!result.isDouble())
                throw new IllegalStateException("Int or Double expected");

            d[i] = result.toDouble();
        }

        return new DoubleSeqNode(d);
    }

    private Node reduce(IntegerSeqNode seq, Node n, Node lambda) {
        Node acc = n;

        Deque<Node> stack = new ArrayDeque<>();
        for (int i = 1; i < seq.seq.length; i++) {
            stack.push(acc);
            stack.push(new IntNode(seq.seq[i]));
            eval(stack, lambda.getNodes());

            acc = stack.pop();
        }

        return acc;
    }

    private Node reduce(DoubleSeqNode seq, Node n, Node lambda) {
        Node acc = n;

        Deque<Node> stack = new ArrayDeque<>();
        for (int i = 1; i < seq.seq.length; i++) {
            stack.push(acc);
            stack.push(new DoubleNode(seq.seq[i]));
            eval(stack, lambda.getNodes());

            acc = stack.pop();
        }

        return acc;
    }

    public static void main(String... args) {
        String expr =
//                "3 + out((1 + 7) * 4)\n" +
//                "var a = 0\n" +
//                "var b = (a * 2) + 1\n" +
//                "out(7)\n" +
//                "var fiveHundred = 400 + 50 * (3 - 1) / 2\n" +
//                "var n = 2\n" +
//                "var x = 0\n" +
//                "var n = x + 500\n" +
                "var seq = {4, 6}\n" +
                "var sequence = map(seq, i -> i + 1)\n" +
//                "var sequence = map({0, 3}, i -> (i * i) + 1)\n" +
//                "var pi = 3.1415 * reduce (sequence, 0, x y -> x + y)\n" +
//                "var pi = 1 * reduce(sequence, 1000, acc y -> acc + y)\n" +
                "var pi = 1 * reduce(sequence, 1, acc y -> acc * y)\n" +
                "print \"pi = \"\n" +
                "out pi\n" +
                "" ;

        try {
            Lexer lexer = new Lexer();
            List<Lexem> lexems = lexer.parse(expr);

            System.out.println(expr);
            System.out.println();
            System.out.println(lexems);
            System.out.println();

            Parser parser = new Parser(lexems);
            Deque<Node> rpn = parser.parse();

            System.out.println();
            System.out.println("byLineStack:");
            rpn.stream()
                    .map(x -> x.getType() == LexemType.NewLine ? "^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ \n" : x.toString())
                    .forEach(System.out::println);

            System.out.println();
            System.out.println();

            Interpreter interpreter = new Interpreter();
            interpreter.eval(rpn);

        } catch (ParseException e) {

            StringBuilder b = new StringBuilder()
                    .append(e.getCode())
                    .append("\n");

            Location location = e.getLocation();
            for (int i = 0; i < location.offset; i++) {
                b.append(" ");
            }

            b.append("^ ").append(e.getMessage());

            System.err.println(b);
        }
    }
}

