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
                case LAMBDA:        push(node, stack); break;

                case STORE:         store(node, stack, vars); break;
                case LOAD:          load(vars, node, stack); break;

                case OUT:           out(node, stack); break;
                case PRINT:         print(node, stack); break;
                case MAP:           map(node, stack); break;
                case REDUCE:        reduce(node, stack); break;
                case NEWSEQUENCE:   sequence(node, stack); break;
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
     * @param node NEWSEQUENCE node
     * @param stack stack
     */
    private void sequence(Node node, Deque<Node> stack) {
        if (stack.size() < 2)
            throw new EvalException(node.getLocation(), "Can't create sequence. Stack size is less than two elements. ");

        Node l = stack.pop();
        if (!l.isInteger())
            throw new EvalException(l.getLocation(), "Can't create sequence. Only Integer is allowed. ");

        Node r = stack.pop();
        if (!r.isInteger())
            throw new EvalException(r.getLocation(), "Can't create sequence. Only Integer is allowed. ");

        SequenceNode seq = new SequenceNode(node.getLocation(), r.toInteger(), l.toInteger());
        log.trace("stack.push {}", node);
        stack.push(seq);
    }


    private void map(Node node, Deque<Node> stack) {
        if (stack.size() < 2)
            throw new EvalException(node.getLocation(), "Stack size is less than two elements. ");

        Node lambda = stack.pop();
        if (!lambda.is(NodeType.LAMBDA))
            throw new EvalException(lambda.getLocation(), "Invalid type, expected lambda.");

        Node seq = stack.pop();
        if (!seq.is(NodeType.SEQUENCE))
            throw new EvalException(seq.getLocation(), "Invalid type, expected IntegerSeq or DoubleSeq.");

        Node mapped = map(seq.toSeq(), lambda);

        log.trace("stack.push {}", mapped);
        stack.push(mapped);
    }

    private Node map(SequenceNode seq, Node lambda) {
        log.trace("call map({}, {})", seq, lambda);

        Deque<Node> stack = new ArrayDeque<>();
        for (int i = 0; i < seq.seq.length; i++) {
            stack.push(seq.seq[i]);
            eval(stack, lambda.getNodes());

            Node result = stack.pop();
            if (!result.isDouble() && !result.isInteger())
                throw new IllegalStateException("Int or Double expected");

            seq.seq[i] = result;
        }

        return seq;
    }



    private void reduce(Node node, Deque<Node> stack) {
        if (stack.size() < 3)
            throw new EvalException(node.getLocation(), "Stack size is less than three elements. ");

        Node lambda = stack.pop();
        if (!lambda.is(NodeType.LAMBDA))
            throw new EvalException(lambda.getLocation(), "Invalid type, expected lambda.");

        Node acc = stack.pop();
        if (!acc.isNumber())
            throw new EvalException(acc.getLocation(), "Invalid type, expected Integer or Double.");

        Node seq = stack.pop();
        if (!seq.is(NodeType.SEQUENCE))
            throw new EvalException(seq.getLocation(), "Invalid type, expected IntegerSeq or DoubleSeq.");

        Node reduced = reduce(seq.toSeq(), acc, lambda);

        log.trace("stack.push {}", reduced);
        stack.push(reduced);
    }

    private Node reduce(SequenceNode seq, Node acc, Node lambda) {
        log.trace("call reduce({}, {}, {})", seq, acc, lambda);
        ReduceFunction reduce = (a, b) -> {
            Deque<Node> stack = new ArrayDeque<>();
            stack.push(a);
            stack.push(b);
            eval(stack, lambda.getNodes());

            return stack.pop();
        };

        ForkJoinTask<Node> reduced = pool
                .submit(new ReduceRecursiveTask(REDUCE_SEQ_BATCH_SIZE, seq.seq, 0, seq.seq.length, reduce));

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
            throw new EvalException(node.getLocation(), "Can't eval LOAD, no value found with name ", node.getContent());

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
            throw new EvalException(node.getLocation(), "Stack is empty.");

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
            throw new EvalException(node.getLocation(), "Stack is empty.");

        Node pop = stack.pop();
        log.trace("out {}", pop);
        if (!pop.isNumber() && !pop.isSeq())
            throw new EvalException(pop.getLocation(), "Invalid type for out: Integer, Double or Sequence are allowed");

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
            throw new EvalException(node.getLocation(), "Can't eval PRINT. Stack is empty. ");
        Node pop = stack.pop();

        log.trace("print {}", pop);
        if (!pop.isString())
            throw new EvalException(pop.getLocation(), "Invalid type for print: only String is allowed");

        out.print(pop.toString());
    }


    /**
     * A op B
     */
    private void op(Node node, Deque<Node> stack) {
        if (stack.size() < 2)
            throw new EvalException(node.getLocation(), "Stack size is less than two elements. ");

        Node b = stack.pop();
        if (!b.isNumber())
            throw new EvalException(b.getLocation(), "Invalid type, expected Number.");

        Node a = stack.pop();
        if (!a.isNumber())
            throw new EvalException(a.getLocation(), "Invalid type, expected Number.");

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

        throw new EvalException(node.getLocation(), "Unexpected operator");
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

