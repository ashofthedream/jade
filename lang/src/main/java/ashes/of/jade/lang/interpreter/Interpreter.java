package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.*;
import ashes.of.jade.lang.parser.ParseException;
import ashes.of.jade.lang.parser.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;
import java.util.*;


public class Interpreter {
    private static final Logger log = LogManager.getLogger(Interpreter.class);


    public static class Scope {
        public final Map<String, Node> vars;
        public final Deque<Node> stack;

        public Scope(Map<String, Node> vars, Deque<Node> stack) {
            this.vars = vars;
            this.stack = stack;
        }
        
        public Scope(Deque<Node> stack) {
            this(new HashMap<>(), stack);
        }        
        
        public Scope() {
            this(new HashMap<>(), new ArrayDeque<>());
        }
    }


    private final Lexer lexer;
    private final Parser parser;
    private PrintStream out = System.out;

    public Interpreter(Lexer lexer, Parser parser) {
        this.lexer = lexer;
        this.parser = parser;
    }

    public Interpreter() {
        this(new Lexer(), new Parser());
    }


    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }


    public Scope eval(String text) {
        log.info("source: {}", text);

        List<Lexem> lexems = lexer.parse(text);

        System.out.println(lexems);
        System.out.println();

        Deque<Node> rpn = parser.parse(lexems);

        System.out.println();
        System.out.println("byLineStack:");
        rpn.stream()
                .map(x -> x.getType() == NodeType.NL ? "^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ \n" : x.toString())
                .forEach(System.out::println);

        System.out.println();
        System.out.println();

        return eval(rpn);
    }

    public Scope eval(Deque<Node> nodes) {
        return eval(new Scope(), nodes);
    }

    public Scope eval(Deque<Node> stack, Deque<Node> nodes) {
        return eval(new Scope(stack), nodes);
    }

    public Scope eval(Scope scope, Deque<Node> nodes) {
        log.debug("eval {} nodes: {}", nodes.size(), nodes);
        log.trace("vars  <-- {}", scope.vars);
        log.trace("stack <-- {}", scope.stack);

        Iterator<Node> it = nodes.descendingIterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.is(NodeType.NL) || node.is(NodeType.EOF))
                continue;

            log.debug("eval: {}", node);
            log.trace("vars  <-- {}", scope.vars);
            log.trace("stack <-- {}", scope.stack);

            if (node.isInteger() || node.isDouble() || node.isString()) {
                log.trace("scope.stack.push {}", node);
                scope.stack.push(node);
                continue;
            }

            if (node.is(NodeType.STORE)) {
                Node pop = scope.stack.pop();
                log.trace("vars.put {} -> {}", node.getContent(), node);
                scope.vars.put(node.getContent(), pop);
            }

            if (node.is(NodeType.LOAD)) {
                Node pop = scope.vars.get(node.getContent());
                log.trace("vars.get {} scope.stack.push {}", node.getContent(), pop);
                scope.stack.push(pop);
            }

            if (node.is(NodeType.OUT)) {
                Node pop = scope.stack.pop();
                log.trace("out {}", pop);
                if (pop.isString())
                    throw new EvalException("String isn't allowed for out ", "", pop.getLocation());

                out.println(pop);
            }

            if (node.is(NodeType.PRINT)) {
                Node pop = scope.stack.pop();
                log.trace("print {}", pop);
                if (!pop.isString())
                    throw new EvalException("Invalid type for print, only string is allowed", "", pop.getLocation());

                out.print(pop.toString());
            }

            if (node.is(NodeType.MAP)) {
                Node lambda = scope.stack.pop();
                Node seq = scope.stack.pop();

                log.trace("call map({}, {})", seq, lambda);

                Node mapped = seq.isIntegerSeq() ?
                        map(seq.toIntegerSeq(), lambda) :
                        map(seq.toDoubleSeq(), lambda);

                scope.stack.push(mapped);
            }

            if (node.is(NodeType.REDUCE)) {
                Node lambda = scope.stack.pop();
                Node n = scope.stack.pop();
                Node seq = scope.stack.pop();

                log.trace("call reduce({}, {}, {})", seq, n, lambda);

                Node reduced = seq.isIntegerSeq() ?
                        reduce(seq.toIntegerSeq(), n, lambda) :
                        reduce(seq.toDoubleSeq(), n, lambda);

                scope.stack.push(reduced);
            }


            if (node.is(NodeType.LAMBDA)) {
                log.trace("scope.stack.push {}", node);
                scope.stack.push(node);
            }

            if (node.is(NodeType.SEQ)) {
                Node l = scope.stack.pop();
                Node r = scope.stack.pop();

                SeqNode seq = new SeqNode(r, l);
                log.trace("scope.stack.push {}", node);
                scope.stack.push(seq);
            }

            if (isOperator(node)) {
                Node b = scope.stack.pop();
                Node a = scope.stack.pop();

                log.trace("operator: {} {} {}", node, a, b);
                Node result = operate(node, a, b);
                log.trace("scope.stack.push {}", result);
                scope.stack.push(result);
            }
        }


        log.debug("eval ends with stack {} and vars {}", scope.stack, scope.vars);
        return scope;
    }

    private Node operate(Node node, Node a, Node b) {
        switch (node.getType()) {
            case ADD:   return add(a, b);
            case SUB:   return subtract(a, b);
            case MUL:   return multiply(a, b);
            case DIV:   return divide(a, b);
            case POWER: return new DoubleNode(Math.pow(b.toDouble(), a.toDouble()));
        }

        throw new EvalException("Unknown operator ", "", node.getLocation());
    }


    private Node divide(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() / l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode(r.toInteger() / l.toInteger());
        }

        throw new EvalException("Can't " + l + " * " + r, "", l.getLocation());
    }

    private Node multiply(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() * l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode(r.toInteger() * l.toInteger());
        }

        throw new EvalException("Can't " + l + " * " + r, "", l.getLocation());
    }

    private Node subtract(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() - l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode(r.toInteger() - l.toInteger());
        }

        throw new EvalException("Can't " + l + " - " + r, "", l.getLocation());
    }


    private Node add(Node l, Node r) {
        if (l.isDouble() || r.isDouble()) {
            return new DoubleNode(r.toDouble() + l.toDouble());
        }

        if (l.isInteger() && r.isInteger()) {
            return new IntNode( r.toInteger() + l.toInteger());
        }

        throw new EvalException("Can't eval(" + l + " + " + r + ")", "", l.getLocation());
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
            Interpreter interpreter = new Interpreter();
            Interpreter.Scope state = interpreter.eval(expr);
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

