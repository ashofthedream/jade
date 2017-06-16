package ashes.of.jade.lang;

import java.util.*;
import java.util.stream.Collectors;

import static ashes.of.jade.lang.LexemType.NewLine;
import static ashes.of.jade.lang.LexemType.Out;


public class Interpreter {

    private final Deque<Node> stack = new ArrayDeque<>();
    private final List<Map<String, Node>> scopes = new ArrayList<>();
    private int scope = 0;


    public void eval(Deque<Node> nodes) {

        scopes.add(new HashMap<>());

        while (!nodes.isEmpty()) {
            Node node = nodes.removeLast();
            System.out.printf("-> %-40s %-60s %s%n", node, scopes.get(scope), stack);

            if (node.is(LexemType.IntegerNumber)) {
                stack.push(node);
                continue;
            }

            if (node.is(LexemType.STORE)) {
                Node pop = stack.pop();
                scopes.get(0).put(node.asString(), pop);
            }

            if (node.is(LexemType.LOAD)) {
                Node pop = scopes.get(0).get(node.asString());
                stack.push(new DoubleNode(pop.asDouble()));
            }

            if (node.is(Out)) {
                Node pop = stack.pop();
                System.out.println("\t\t\t" + pop.asString());
            }

            if (node.getType().isOperator()) {
                Node l = stack.pop();
                Node r = stack.pop();
                switch (node.getType()) {
                    case Plus:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.asDouble() + l.asDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.asInteger() + l.asInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " + " + r);

                    case Minus:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.asDouble() - l.asDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.asInteger() - l.asInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " - " + r);

                    case Multiply:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.asDouble() * l.asDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.asInteger() * l.asInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " * " + r);

                    case Divide:
                        if (l.isDouble() || r.isDouble()) {
                            stack.push(new DoubleNode(l.asDouble() / l.asDouble()));
                            break;
                        }

                        if (l.isInteger() && r.isInteger()) {
                            stack.push(new IntNode(l.asInteger() / l.asInteger()));
                            break;
                        }

                        throw new RuntimeException("Can't " + l + " * " + r);

                    case Power:

                        stack.push(new DoubleNode( Math.pow(r.asDouble(), l.asDouble())));
                        break;

                }
            }
        }


        System.out.println();
        System.out.println();
        System.out.println("scope: " + scopes.get(scope) + "    stack: " + stack );
    }



    public static void main(String... args) {
        String expr =
//                "3 + out((1 + 7) * 4)\n" +
//                "var a = 0\n" +
//                "var b = (a * 2) + 1\n" +
//                "out(7)\n" +
                "var fiveHundred = 400 + 50 * (3 - 1) / 2\n" +
                "var n = 0 + fiveHundred\n" +
//                "var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))\n" +
//                "var pi = 3.1415 * reduce (sequence, 0, x y -> x + y)\n" +
//                "print \"pi = \"\n" +
                "out fiveHundred\n" +
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

            System.out.println("byLineStack:");
            rpn.stream()
                    .map(x -> x.getType() == NewLine ? "\n" : x.toString())
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

