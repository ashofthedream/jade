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


    private static final int DEFAULT_PARALLELISM_MIN_SIZE = 1024 * 1024;


    private final Lexer lexer;
    private final Parser parser;

    // todo move to settings?
    private PrintStream out = System.out;
    private ForkJoinPool threadPool = ForkJoinPool.commonPool();
    private int mapParallelismSize = DEFAULT_PARALLELISM_MIN_SIZE;
    private int reduceParallelismSize = DEFAULT_PARALLELISM_MIN_SIZE;

    public Interpreter(Lexer lexer, Parser parser) {
        this.lexer = lexer;
        this.parser = parser;
    }


    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public ForkJoinPool getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ForkJoinPool threadPool) {
        this.threadPool = threadPool;
    }

    public int getMapParallelismSize() {
        return mapParallelismSize;
    }

    public void setMapParallelismSize(int mapParallelismSize) {
        this.mapParallelismSize = mapParallelismSize;
    }

    public int getReduceParallelismSize() {
        return reduceParallelismSize;
    }

    public void setReduceParallelismSize(int reduceParallelismSize) {
        this.reduceParallelismSize = reduceParallelismSize;
    }

    public Scope eval(String text) {
        log.info("eval source: {}", text);
        List<Lexem> lexems = lexer.parse(text);
        Deque<Node> rpn = parser.parse(lexems);
        return eval(rpn);
    }

    public Scope eval(Deque<Node> nodes) {
        return eval(new Scope(), nodes);
    }

    public Scope eval(Deque<Node> stack, Deque<Node> nodes) {
        return eval(new Scope(stack), nodes);
    }

    public Scope eval(Scope scope, Deque<Node> nodes) {
        long start = System.currentTimeMillis();
        log.info("eval {} nodes: {}", nodes.size(), nodes);
        
        log.trace("vars  <-- {}", scope.getVars());
        log.trace("stack <-- {}", scope.getStack());

        Iterator<Node> it = nodes.descendingIterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.is(NodeType.NL) || node.is(NodeType.EOF))
                continue;

            log.debug("eval: {}", node);
            log.trace("vars  <-- {}", scope.getVars());
            log.trace("stack <-- {}", scope.getStack());

            switch (node.getType()) {
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                case POWER:         op(node, scope); break;

                case INTEGER:
                case DOUBLE:
                case STRING:
                case LAMBDA:        push(node, scope); break;

                case STORE:         store(node, scope); break;
                case LOAD:          load(node, scope); break;

                case OUT:           out(node, scope); break;
                case PRINT:         print(node, scope); break;
                case MAP:           map(node, scope); break;
                case REDUCE:        reduce(node, scope); break;
                case NEWSEQUENCE:   sequence(node, scope); break;
            }
        }

        log.info("Eval ends after {}ms", System.currentTimeMillis() - start);
        log.debug("vars  <-- {}", scope.getVars());
        log.debug("stack <-- {}", scope.getStack());
        return scope;
    }

    /**
     * Pushes node to stack
     *
     * @param node node to push
     * @param scope current scope
     */
    private void push(Node node, Scope scope) {
        scope.push(node);
    }

    /**
     * Creates sequence and pushes it to stack
     *
     * @param node create sequence node
     * @param scope current scope
     */
    private void sequence(Node node, Scope scope) {
        scope.checkStackSize(node.getLocation(), 2);
        Node r = scope.pop(Node::isNumber, "Expected Integer");
        Node l = scope.pop(Node::isNumber, "Expected Integer");

        SequenceNode seq = new SequenceNode(node.getLocation(), l.toInteger(), r.toInteger());
        scope.push(seq);
    }


    private void map(Node node, Scope scope) {
        scope.checkStackSize(node.getLocation(), 2);
        Node lambda = scope.pop(Node::isLambda, "Expected Lambda");
        Node seq = scope.pop(Node::isSeq, "Expected Sequence");

        Node mapped = map(seq.toSeq(), lambda);
        scope.push(mapped);
    }

    private Node map(SequenceNode seq, Node lambda) {
        log.debug("call map({}, {})", seq, lambda);

        long time = System.currentTimeMillis();
        if (seq.size() < getMapParallelismSize()) {
            map(seq, lambda, 0, seq.size());
            log.trace("map.elapsed all: {}", System.currentTimeMillis() - time);
            return seq;
        }

        int threads = threadPool.getParallelism();
        int batchSize = seq.size() / (threads * 4 + 1);
        List<ForkJoinTask<?>> futures = new ArrayList<>();
        for (int start = 0; start < seq.size(); start += batchSize)
            futures.add(submitMap(seq, lambda, start, Math.min(seq.size(), start + batchSize)));

        futures.forEach(ForkJoinTask::join);
        log.trace("map.elapsed all: {} (tasks: {})", System.currentTimeMillis() - time, futures.size());
        return seq;
    }

    private ForkJoinTask<?> submitMap(SequenceNode seq, Node lambda, int start, int end) {
        return threadPool.submit(() -> map(seq, lambda, start, end));
    }

    private void map(SequenceNode seq, Node lambda, int start, int end) {
        long time = System.currentTimeMillis();
        Deque<Node> stack = new ArrayDeque<>();
        for (int i = start; i < end; i++) {
            stack.push(seq.seq[i]);
            Scope scope = eval(stack, lambda.getNodes());
            Node result = scope.pop(Node::isNumber, "Expected number");
            seq.seq[i] = result;
        }

        log.trace("map.elapsed task: {}", System.currentTimeMillis() - time);
    }


    private void reduce(Node node, Scope scope) {
        scope.checkStackSize(node.getLocation(), 3);
        Node lambda = scope.pop(Node::isLambda, "Expected Lambda");
        Node acc = scope.pop(Node::isNumber, "Expected Number");
        Node seq = scope.pop(Node::isSeq, "Expected Sequence");

        Node reduced = reduce(seq.toSeq(), acc, lambda);

        scope.push(reduced);
    }

    private Node reduce(SequenceNode seq, Node acc, Node lambda) {
        log.debug("call reduce({}, {}, {})", seq, acc, lambda);

        long start = System.currentTimeMillis();
        ReduceFunction reduce = (a, b) -> {
            Deque<Node> stack = new ArrayDeque<>();
            stack.push(a);
            stack.push(b);
            eval(stack, lambda.getNodes());

            return stack.pop();
        };

        ForkJoinTask<Node> task = threadPool
                .submit(new ReduceRecursiveTask(getReduceParallelismSize(), seq.seq, 0, seq.seq.length, reduce));

        Node reduced = reduce.reduce(acc, task.join());
        log.trace("reduce.elapsed {} (getReduceParallelismSize = {})",
                System.currentTimeMillis() - start, getReduceParallelismSize());
        return reduced;
    }



    /**
     * Loads value from local score and pushes it to stack
     *
     * @param node store node
     * @param scope current scope
     */
    private void load(Node node, Scope scope) {
        Node var = scope.load(node.getContent());
        if (var == null)
            throw new EvalException(node.getLocation(), "No value found with name %s", node.getContent());

        scope.push(var);
    }

    /**
     * Stores value from stack to local scope
     *
     * @param node store node
     * @param scope current scope
     */
    private void store(Node node, Scope scope) {
        scope.checkStackNotEmpty(node.getLocation());

        Node pop = scope.pop();
        scope.store(node.getContent(), pop);
    }


    /**
     * Prints integer or double values to the output stream
     *
     * @param node print node
     * @param scope current scope
     */
    private void out(Node node, Scope scope) {
        scope.checkStackNotEmpty(node.getLocation());

        Node pop = scope.pop(n -> n.isNumber() || n.isSeq(), "Expected Number or Sequence");

        out.println(pop);
    }

    /**
     * Prints string value to the output stream
     *
     * @param node print node
     * @param scope current scope
     */
    private void print(Node node, Scope scope) {
        scope.checkStackNotEmpty(node.getLocation());
        Node pop = scope.pop(Node::isString, "Expected String");

        log.trace("print {}", pop);
        out.print(pop.toString());
    }


    /**
     * A op B
     */
    private void op(Node node, Scope scope) {
        scope.checkStackSize(node.getLocation(), 2);
        Node b = scope.pop(Node::isNumber, "Expected Number");
        Node a = scope.pop(Node::isNumber, "Expected Number");

        log.trace("operator: {} {} {}", node, a, b);
        Node result = op(node, a, b);
        scope.push(result);
    }

    private Node op(Node op, Node a, Node b) {
        switch (op.getType()) {
            case ADD:   return add(a, b);
            case SUB:   return subtract(a, b);
            case MUL:   return multiply(a, b);
            case DIV:   return divide(a, b);
            case POWER: return power(a, b);
        }

        throw new EvalException(op.getLocation(), "Unexpected operator: %s", op);
    }

    private Node add(Node a, Node b) {
        return a.isDouble() || b.isDouble() ?
                new DoubleNode(a.getLocation(), a.toDouble() + b.toDouble()) :
                new IntNode(a.getLocation(),a.toInteger() + b.toInteger());
    }

    private Node subtract(Node a, Node b) {
        return a.isDouble() || b.isDouble() ?
                new DoubleNode(a.getLocation(),a.toDouble() - b.toDouble()) :
                new IntNode(a.getLocation(),a.toInteger() - b.toInteger());
    }

    private Node multiply(Node a, Node b) {
        return a.isDouble() || b.isDouble() ?
                new DoubleNode(a.getLocation(),a.toDouble() * b.toDouble()) :
                new IntNode(a.getLocation(),a.toInteger() * b.toInteger());
    }

    private Node divide(Node a, Node b) {
        return a.isDouble() || b.isDouble() ?
                new DoubleNode(a.getLocation(),a.toDouble() / b.toDouble()) :
                new IntNode(a.getLocation(),a.toInteger() / b.toInteger());
    }

    private Node power(Node a, Node b) {
        double pow = Math.pow(a.toDouble(), b.toDouble());

        return a.isDouble() || b.isDouble() ?
                new DoubleNode(pow) :
                new IntNode(Math.round(pow));
    }
}