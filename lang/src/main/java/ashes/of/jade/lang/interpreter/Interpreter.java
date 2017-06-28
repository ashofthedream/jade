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


    private static final int REDUCE_SEQ_BATCH_SIZE = 1024;

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

        public Node load(String name) {
            return vars.get(name);
        }

        public Node store(String name, Node node) {
            return vars.put(name, node);
        }

        @Override
        public String toString() {
            return "Scope{" +
                    "vars=" + vars +
                    ", stack=" + stack +
                    '}';
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

            switch (node.getType()) {
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                case POWER:     op(node, stack); break;



                case INTEGER:
                case DOUBLE:
                case STRING:
                case LAMBDA:    push(node, stack); break;

                case STORE:     store(node, stack, vars); break;
                case LOAD:      load(vars, node, stack); break;

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
        long len = (end - start) + 1;
        long[] a = new long[(int)len];

        for (int i = 0; i < len; i++)
            a[i] = start + i;

        IntegerSeqNode seq = new IntegerSeqNode(a);
        log.trace("stack.push {}", node);
        stack.push(seq);
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

        log.trace("stack.push {}", mapped);
        stack.push(mapped);
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

        log.trace("stack.push {}", reduced);
        stack.push(reduced);
    }




    interface ReduceFunction {
        Node reduce(Node a, Node b);
    }

    public static class DoubleReduceRecursiveTask extends RecursiveTask<Node> {

        private final int minLen;
        private final double[] seq;
        private final int left;
        private final int right;
        private final ReduceFunction f;

        public DoubleReduceRecursiveTask(int minLen, double[] seq, int left, int right, ReduceFunction f) {
            this.minLen = minLen;
            this.seq = seq;
            this.left = left;
            this.right = right;
            this.f = f;
        }

        @Override
        protected Node compute() {
            int length = right - left;

            if (length <= minLen)
                return reduce(seq, left, right, f);

            DoubleReduceRecursiveTask l = new DoubleReduceRecursiveTask(minLen, seq, left, left + length / 2,  f);
            DoubleReduceRecursiveTask r = new DoubleReduceRecursiveTask(minLen, seq, left + length / 2, right, f);

            ForkJoinTask<Node> fl = l.fork();
            ForkJoinTask<Node> fr = r.fork();
            return f.reduce(fl.join(), fr.join());
        }
    }



    public static class LongReduceRecursiveTask extends RecursiveTask<Node> {

        private final int minLen;
        private final long[] seq;
        private final int left;
        private final int right;
        private final ReduceFunction f;

        public LongReduceRecursiveTask(int minLen, long[] seq, int left, int right, ReduceFunction f) {
            this.minLen = minLen;
            this.seq = seq;
            this.left = left;
            this.right = right;
            this.f = f;
        }

        @Override
        protected Node compute() {
            int length = right - left;

            if (length <= minLen)
                return reduce(seq, left, right, f);

            LongReduceRecursiveTask l = new LongReduceRecursiveTask(minLen, seq, left, left + length / 2,  f);
            LongReduceRecursiveTask r = new LongReduceRecursiveTask(minLen, seq, left + length / 2, right, f);

            ForkJoinTask<Node> fl = l.fork();
            ForkJoinTask<Node> fr = r.fork();
            return f.reduce(fl.join(), fr.join());
        }
    }

    public static Node reduce(long[] seq, int left, int right, ReduceFunction f) {
        Node acc = new IntNode(seq[left]);
        for (int i = left + 1; i < right; i++)
            acc = f.reduce(acc, new IntNode(seq[i]));

        return acc;
    }

    public static Node reduce(double[] seq, int left, int right, ReduceFunction f) {
        Node acc = new DoubleNode(seq[left]);
        for (int i = left + 1; i < right; i++)
            acc = f.reduce(acc, new DoubleNode(seq[i]));

        return acc;
    }

    private Node reduce(IntegerSeqNode seq, Node acc, Node lambda) {
        ReduceFunction reduce = (a, b) -> {
            Deque<Node> stack = new ArrayDeque<>();
            stack.push(a);
            stack.push(b);
            eval(stack, lambda.getNodes());

            return stack.pop();
        };

        ForkJoinTask<Node> reduced = pool
                .submit(new LongReduceRecursiveTask(REDUCE_SEQ_BATCH_SIZE, seq.seq, 0, seq.seq.length, reduce));

        return reduce.reduce(acc, reduced.join());
    }

    private Node reduce(DoubleSeqNode seq, Node acc, Node lambda) {
        ReduceFunction reduce = (a, b) -> {
            Deque<Node> stack = new ArrayDeque<>();
            stack.push(a);
            stack.push(b);
            eval(stack, lambda.getNodes());

            return stack.pop();
        };

        ForkJoinTask<Node> reduced = pool
                .submit(new DoubleReduceRecursiveTask(REDUCE_SEQ_BATCH_SIZE, seq.seq, 0, seq.seq.length, reduce));

        return reduce.reduce(acc, reduced.join());
    }



    /**
     * Loads value from local score and pushes it to stack
     *
     * @param vars var scope
     * @param node store node
     * @param stack stack
     */
    private void load(Map<String, Node> vars, Node node, Deque<Node> stack) {
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
     * @param stack stack
     * @param vars var scope
     */
    private void store(Node node, Deque<Node> stack, Map<String, Node> vars) {
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

