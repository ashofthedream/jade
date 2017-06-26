package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.*;
import ashes.of.jade.lang.parser.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;


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



    private final ForkJoinPool pool = ForkJoinPool.commonPool();
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
        Map<String, Node> vars = scope.vars;
        Deque<Node> stack = scope.stack;
        
        log.trace("vars  <-- {}", vars);
        log.trace("stack <-- {}", stack);

        Iterator<Node> it = nodes.descendingIterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.is(NodeType.NL) || node.is(NodeType.EOF))
                continue;

            log.debug("eval: {}", node);
            log.trace("vars  <-- {}", vars);
            log.trace("stack <-- {}", stack);

            if (isOperator(node)) {
                op(node, stack);
            }

            switch (node.getType()) {
                case INTEGER:
                case DOUBLE:
                case STRING:
                case LAMBDA:    push(node, stack); break;

                case STORE:     store(node, vars, stack); break;
                case LOAD:      load(node, vars, stack); break;

                case OUT:       out(node, stack); break;
                case PRINT:     print(node, stack); break;
                case MAP:       map(node, stack); break;
                case REDUCE:    reduce(node, stack); break;
                case SEQ:       sequence(node, stack); break;
            }
        }


        log.debug("eval ends with stack {} and vars {}", stack, vars);
        return scope;
    }

    private void push(Node node, Deque<Node> stack) {
        log.trace("stack.push {}", node);
        stack.push(node);
    }

    /**
     * Creates sequence and pushes it to stack
     *
     * @param node SEQ node
     * @param stack stack
     */
    private void sequence(Node node, Deque<Node> stack) {
        if (stack.size() < 2)
            throw new EvalException("Can't create sequence. Stack size is less than two elements. ", node.getLocation());

        Node l = stack.pop();
        if (!l.isInteger())
            throw new EvalException("Can't create sequence. Only Integer is allowed. ", l.getLocation());

        Node r = stack.pop();
        if (!r.isInteger())
            throw new EvalException("Can't create sequence. Only Integer is allowed. ", r.getLocation());

        long start = r.toInteger();
        long end = l.toInteger();
        long[] a = new long[(int) (end - start)] ;

        for (long i = 0; i < end - start; i++) {
            a[(int) i] = start + i;
        }

        IntegerSeqNode seq = new IntegerSeqNode(a);
        log.trace("stack.push {}", node);
        stack.push(seq);
    }


    private boolean isOperator(Node node) {
        return  node.is(NodeType.ADD) ||
                node.is(NodeType.SUB) ||
                node.is(NodeType.MUL) ||
                node.is(NodeType.DIV) ||
                node.is(NodeType.POWER);
    }


    private void reduce(Node node, Deque<Node> stack) {
        if (stack.size() < 3)
            throw new EvalException("Can't eval REDUCE. Stack size is less than three elements. ", node.getLocation());

        Node lambda = stack.pop();
        if (!lambda.is(NodeType.LAMBDA))
            throw new EvalException("Can't eval REDUCE. Invalid type, expected lambda.", lambda.getLocation());

        Node n = stack.pop();
        if (!n.is(NodeType.INTEGER) && !n.is(NodeType.DOUBLE))
            throw new EvalException("Can't eval REDUCE. Invalid type, expected Integer or Double.", n.getLocation());

        Node seq = stack.pop();
        if (!seq.is(NodeType.INTEGERSEQ) && !seq.is(NodeType.DOUBLESEQ))
            throw new EvalException("Can't eval REDUCE. Invalid type, expected IntegerSeq or DoubleSeq.", seq.getLocation());


        log.trace("call reduce({}, {}, {})", seq, n, lambda);

        Node reduced = seq.isIntegerSeq() ?
                reduce(seq.toIntegerSeq(), n, lambda) :
                reduce(seq.toDoubleSeq(), n, lambda);

        stack.push(reduced);
    }

    private void map(Node node, Deque<Node> stack) {
        if (stack.size() < 2)
            throw new EvalException("Can't eval MAP. Stack size is less than two elements. ", node.getLocation());

        Node lambda = stack.pop();
        if (!lambda.is(NodeType.LAMBDA))
            throw new EvalException("Can't eval MAP. Invalid type, expected lambda.", lambda.getLocation());

        Node seq = stack.pop();
        if (!seq.is(NodeType.INTEGERSEQ) && !seq.is(NodeType.DOUBLESEQ))
            throw new EvalException("Can't eval REDUCE. Invalid type, expected IntegerSeq or DoubleSeq.", seq.getLocation());

        log.trace("call map({}, {})", seq, lambda);

        Node mapped = seq.isIntegerSeq() ?
                map(seq.toIntegerSeq(), lambda) :
                map(seq.toDoubleSeq(), lambda);

        stack.push(mapped);
    }


    /**
     * Loads value from local score and pushes it to stack
     *
     * @param node store node
     * @param vars var scope
     * @param stack stack
     */
    private void load(Node node, Map<String, Node> vars, Deque<Node> stack) {
        Node var = vars.get(node.getContent());
        if (var == null)
            throw new EvalException("Can't eval LOAD, no value found with name " + node.getContent(), node.getLocation());

        log.trace("vars.get {} stack.push {}", node.getContent(), var);
        stack.push(var);
    }

    /**
     * Stores value from stack to local scope
     *
     * @param node store node
     * @param vars var scope
     * @param stack stack
     */
    private void store(Node node, Map<String, Node> vars, Deque<Node> stack) {
        if (stack.isEmpty())
            throw new EvalException("Can't eval STORE. Stack is empty. ", node.getLocation());

        Node pop = stack.pop();

        log.trace("vars.put {} -> {}", node.getContent(), node);
        vars.put(node.getContent(), pop);
    }


    /**
     * Prints integer or double values to the output stream
     *
     * @param node print node
     * @param stack stack
     */
    private void out(Node node, Deque<Node> stack) {
        if (stack.isEmpty())
            throw new EvalException("Can't eval OUT. Stack is empty. ", node.getLocation());

        Node pop = stack.pop();
        log.trace("out {}", pop);
        if (!pop.isInteger() && !pop.isIntegerSeq() && !pop.isDouble() && !pop.isDoubleSeq())
            throw new EvalException("Invalid type for out: Integer, Double, Integer[], Double[] are allowed", pop.getLocation());

        out.println(pop);
    }

    /**
     * Prints string value to the output stream
     *
     * @param node print node
     * @param stack stack
     */
    private void print(Node node, Deque<Node> stack) {
        if (stack.isEmpty())
            throw new EvalException("Can't eval PRINT. Stack is empty. ", node.getLocation());
        Node pop = stack.pop();

        log.trace("print {}", pop);
        if (!pop.isString())
            throw new EvalException("Invalid type for print: only String is allowed", pop.getLocation());

        out.print(pop.toString());
    }


    /**
     * A op B
     */
    private void op(Node node, Deque<Node> stack) {
        if (stack.size() < 2)
            throw new EvalException("Can't eval OP. Stack size is less than two elements. ", node.getLocation());

        Node b = stack.pop();
        if (!b.isNumber())
            throw new EvalException("Can't eval OP. Invalid type, expected Number.", b.getLocation());

        Node a = stack.pop();
        if (!a.isNumber())
            throw new EvalException("Can't eval OP. Invalid type, expected Number.", a.getLocation());

        log.trace("operator: {} {} {}", node, a, b);
        Node result = op(node, a, b);
        push(result, stack);
    }

    private Node op(Node node, Node a, Node b) {
        switch (node.getType()) {
            case ADD:   return add(a, b);
            case SUB:   return subtract(a, b);
            case MUL:   return multiply(a, b);
            case DIV:   return divide(a, b);
            case POWER: return power(a, b);
        }

        throw new EvalException("Unknown operator", node.getLocation());
    }

    private Node add(Node l, Node r) {
        return l.isDouble() || r.isDouble() ?
                new DoubleNode(r.toDouble() + l.toDouble()) :
                new IntNode(r.toInteger() + l.toInteger());
    }

    private Node subtract(Node l, Node r) {
        return l.isDouble() || r.isDouble() ?
                new DoubleNode(r.toDouble() - l.toDouble()) :
                new IntNode(r.toInteger() - l.toInteger());
    }

    private Node multiply(Node l, Node r) {
        return l.isDouble() || r.isDouble() ?
                new DoubleNode(r.toDouble() * l.toDouble()) :
                new IntNode(r.toInteger() * l.toInteger());
    }

    private Node divide(Node l, Node r) {
        return l.isDouble() || r.isDouble() ?
                new DoubleNode(r.toDouble() / l.toDouble()) :
                new IntNode(r.toInteger() / l.toInteger());
    }

    private Node power(Node l, Node r) {
        double pow = Math.pow(r.toDouble(), l.toDouble());

        return l.isDouble() || r.isDouble() ?
                new DoubleNode(pow) :
                new IntNode(Math.round(pow));
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

        for (int i = 1; i < seq.seq.length; i++) {
            Deque<Node> stack = new ArrayDeque<>();
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
}

