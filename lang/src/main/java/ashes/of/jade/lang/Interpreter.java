package ashes.of.jade.lang;

import java.util.*;

public class Interpreter {

    private final Deque<Node> stack = new ArrayDeque<>();
    private final List<Map<String, Node>> scopes = new ArrayList<>();
    private int scope = 0;


    public void eval(Deque<Node> nodes) {
        scopes.add(new HashMap<>());

        System.out.printf("eval> %-40s %-60s %-60s %s%n", "NODE", "SCOPE", "STACK", "INPUT");
        while (!nodes.isEmpty()) {
            Node node = nodes.removeLast();
            if (node.is(LexemType.NewLine) || node.is(LexemType.EOF)) {
                System.out.println();
                System.out.println();
                continue;
            }

            System.out.printf("eval> %-40s %-60s %-60s %s%n", node, scopes.get(scope), stack, nodes);

            if (node.is(LexemType.IntegerNumber) || node.is(LexemType.DoubleNumber) || node.is(LexemType.String)) {
                stack.push(node);
                continue;
            }

            if (node.is(LexemType.STORE)) {
                Node pop = stack.pop();
                scopes.get(0).put(node.getContent(), pop);
            }

            if (node.is(LexemType.LOAD)) {
                Node pop = scopes.get(0).get(node.getContent());
                stack.push(pop);
            }

            if (node.is(LexemType.Out)) {
                Node pop = stack.pop();
                System.out.println("\t\t\t" + pop);
            }


            if (node.is(LexemType.Map)) {
                Node lambda = stack.pop();
                Node seq = stack.pop();

                stack.push(seq);
                System.out.println("invoke map " + seq + " " + lambda);
            }

            if (node.is(LexemType.Reduce)) {
                Node lambda = stack.pop();
                Node n = stack.pop();
                Node seq = stack.pop();

                stack.push(new IntNode(new Lexem(LexemType.IntegerNumber, node.getLocation(), ""), -1));
                System.out.println("invoke reduce " + seq + ", " + n + ", " + lambda);
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
                System.out.println("\t\t\t" + pop.toString());
            }

            if (node.getType().isOperator()) {
                Node l = stack.pop();
                Node r = stack.pop();
                switch (node.getType()) {
                    case Plus:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.getLexem(), r.toDouble() + l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.getLexem(), r.toInteger() + l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " + " + r);

                    case Minus:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.getLexem(), r.toDouble() - l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.getLexem(), r.toInteger() - l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " - " + r);

                    case Multiply:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.getLexem(), r.toDouble() * l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.getLexem(), r.toInteger() * l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " * " + r);

                    case Divide:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.getLexem(), r.toDouble() / l.toDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.getLexem(), l.toInteger() / l.toInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " * " + r);

                    case Power:

                        stack.push(new DoubleNode(l.getLexem(),  Math.pow(r.toDouble(), l.toDouble())));
                        break;

                }
            }
        }


        System.out.println();
        System.out.println();
        System.out.printf("eval> %-40s %-60s %s%n", "", scopes.get(scope), stack);
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
//                "var seq = {0, n + 100}\n" +
//                "var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))\n" +
                "var sequence = map({0, 5}, i -> (i * i) + 1)\n" +
                "var pi = 3.1415 * reduce (sequence, 0, x y -> x + y)\n" +
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

