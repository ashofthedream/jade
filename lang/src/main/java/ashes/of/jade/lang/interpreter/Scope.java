package ashes.of.jade.lang.interpreter;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;


public class Scope {
    private static final Logger log = LogManager.getLogger(Scope.class);

    private final Map<String, Node> vars;
    private final Deque<Node> stack;

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

    public Map<String, Node> getVars() {
        return vars;
    }

    public Deque<Node> getStack() {
        return stack;
    }

    public Node load(String name) {
        Node node = vars.get(name);

        log.trace("load  {} -> {}", name, node);
        return node;
    }

    public Node store(String name, Node node) {
        log.trace("store {} <- {}", name, node);
        return vars.put(name, node);
    }

    public void push(Node node) {
        log.trace("push <- {}", node);
        stack.push(node);
    }

    public Node pop() {
        Node node = stack.pop();
        log.trace("pop  -> {}", node);

        return node;
    }

    public Node pop(Predicate<Node> predicate, String message, Object... args) {
        Node node = stack.pop();
        log.trace("pop  -> {}", node);

        if (!predicate.test(node))
            throw new EvalException(node.getContent(), node.getLocation(), "Invalid type: " + message, args);

        return node;
    }

    public void checkStackSize(Location location, int size) {
        if (stack.size() < size)
            throw new EvalException(location, "Stack size %d is less than %d", stack.size(), size);
    }

    public void checkStackNotEmpty(Location location) {
        if (stack.isEmpty())
            throw new EvalException(location, "Stack is empty");
    }


    @Override
    public String toString() {
        return "Scope{" +
                "vars=" + vars +
                ", stack=" + stack +
                '}';
    }
}
