package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.*;
import ashes.of.jade.lang.parser.ParseException;
import ashes.of.jade.lang.parser.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;


public class Interpreter {
    private static final Logger log = LogManager.getLogger(Interpreter.class);

    public Deque<Node> eval(Deque<Node> nodes) {
        return eval(new ArrayDeque<>(), new HashMap<>(), nodes);
    }

    public Deque<Node> eval(Deque<Node> stack, Deque<Node> nodes) {
        return eval(stack, new HashMap<>(), nodes);
    }

    public Deque<Node> eval(Deque<Node> stack, Map<String, Node>  scope, Deque<Node> nodes) {
        log.debug("eval {} nodes: {}", nodes.size(), nodes);
        log.trace("scope <-- {}", scope);
        log.trace("stack <-- {}", stack);

        Iterator<Node> it = nodes.descendingIterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.is(NodeType.NL) || node.is(NodeType.EOF))
                continue;


            log.debug("eval: {}", node);
            log.trace("scope {}", scope);
            log.trace("stack {}", stack);
//            System.out.printf("eval> %-40s %-60s %-60s %s%n", node, scope, stack, nodes);

            if (node.isInteger() || node.isDouble() || node.isString()) {
                log.trace("stack.push {}", node);
                stack.push(node);
                continue;
            }

            if (node.is(NodeType.STORE)) {
                Node pop = stack.pop();
                log.trace("scope.put {} -> {}", node.getContent(), node);
                scope.put(node.getContent(), pop);
            }

            if (node.is(NodeType.LOAD)) {
                Node pop = scope.get(node.getContent());
                log.trace("scope.get {} stack.push {}", node.getContent(), pop);
                stack.push(pop);
            }

            if (node.is(NodeType.OUT)) {
                Node pop = stack.pop();
                log.trace("out {}", pop);
                if (pop.isString())
                    throw new EvalException("String isn't allowed at " + pop.getLocation());

                System.out.println("\tOUT  \t\t" + pop);
            }

            if (node.is(NodeType.PRINT)) {
                Node pop = stack.pop();
                log.trace("print {}", pop);
                if (!pop.isString())
                    throw new EvalException("Invalid type for print, only string is allowed at " + pop.getLocation());

                System.out.println("\tPRINT\t\t" + pop.toString());
            }

            if (node.is(NodeType.MAP)) {
                Node lambda = stack.pop();
                Node seq = stack.pop();

                log.trace("call map({}, {})", seq, lambda);

                Node mapped = seq.isIntegerSeq() ?
                        map(seq.toIntegerSeq(), lambda) :
                        map(seq.toDoubleSeq(), lambda);

                stack.push(mapped);
            }

            if (node.is(NodeType.REDUCE)) {
                Node lambda = stack.pop();
                Node n = stack.pop();
                Node seq = stack.pop();

                log.trace("call reduce({}, {}, {})", seq, n, lambda);

                Node reduced = seq.isIntegerSeq() ?
                        reduce(seq.toIntegerSeq(), n, lambda) :
                        reduce(seq.toDoubleSeq(), n, lambda);

                stack.push(reduced);
            }



            if (node.is(NodeType.LAMBDA)) {
                log.trace("stack.push {}", node);
                stack.push(node);
            }

            if (node.is(NodeType.SEQ)) {
                Node l = stack.pop();
                Node r = stack.pop();

                SeqNode seq = new SeqNode(r, l);
                log.trace("stack.push {}", node);
                stack.push(seq);
            }

            if (isOperator(node)) {
                Node b = stack.pop();
                Node a = stack.pop();

                log.trace("operator: {} {} {}", node, a, b);
                Node result = operate(node, a, b);
                log.trace("stack.push {}", result);
                stack.push(result);
            }
        }


        log.debug("eval ends with stack {} and scope {}", stack, scope);
        return stack;
    }

    private Node operate(Node node, Node a, Node b) {
        switch (node.getType()) {
            case ADD:   return add(a, b);
            case SUB:   return subtract(a, b);
            case MUL:   return multiply(a, b);
            case DIV:   return divide(a, b);
            case POWER: return new DoubleNode(Math.pow(b.toDouble(), a.toDouble()));
        }

        throw new EvalException("Unknown operator " + node);
    }


    private Node divide(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() / l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode(r.toInteger() / l.toInteger());
        }

        throw new EvalException("Can't " + l + " * " + r);
    }

    private Node multiply(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() * l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode(r.toInteger() * l.toInteger());
        }

        throw new EvalException("Can't " + l + " * " + r);
    }

    private Node subtract(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() - l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode(r.toInteger() - l.toInteger());
        }

        throw new EvalException("Can't " + l + " - " + r);
    }


    private Node add(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() + l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode( r.toInteger() + l.toInteger());
        }

        throw new EvalException("Can't eval(" + l + " + " + r + ")");
    }



    private boolean isOperator(Node node) {
        return  node.is(NodeType.ADD) ||
                node.is(NodeType.SUB) ||
                node.is(NodeType.MUL) ||
                node.is(NodeType.DIV) ||
                node.is(NodeType.POWER);
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
            stack.push(new DoubleNode(seq.seq[i]));
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
                "var seq = {4, 6}\n" +
                "var sequence = map(seq, i -> i * i)\n" +
//                "var pi = 3.1415 * reduce (sequence, 0, x y -> x + y)\n" +
//                "var pi = 1 * reduce(sequence, 1000, acc y -> acc + y)\n" +
                "var pi = 1 * reduce(sequence, 1, acc y -> acc * y)\n" +
                "print \"pi = \"\n" +
                "out pi\n" +
                "" ;

        try {
            System.out.println(expr);
            System.out.println();

            Lexer lexer = new Lexer();
            List<Lexem> lexems = lexer.parse(expr);
            System.out.println(lexems);
            System.out.println();

            Parser parser = new Parser(lexems);
            Deque<Node> rpn = parser.parse();

            System.out.println();
            System.out.println("byLineStack:");
            rpn.stream()
                    .map(x -> x.getType() == NodeType.NL ? "^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ \n" : x.toString())
                    .forEach(System.out::println);

            System.out.println();
            System.out.println();

            Interpreter interpreter = new Interpreter();
            interpreter.eval(rpn);

        } catch (ParseException e) {

            StringBuilder b = new StringBuilder()
                    .append(e.getLine())
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

