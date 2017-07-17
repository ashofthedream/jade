package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.nodes.LambdaNode;
import ashes.of.jade.lang.nodes.Node;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Predicate;


/**
 * Scope that contains nodes in reverse polish notation based on Shunting-yard algorithm
 */
public class Scope {
    private static final Logger log = LogManager.getLogger(Parser.class);

    public LambdaNode lambda;
    public Deque<Node> stack = new ArrayDeque<>();
    public Deque<Node> out = new ArrayDeque<>();

    public void drainStackToOut(Predicate<Node> predicate) {
        log.trace("drain stack -> out");
        while (!stack.isEmpty() && predicate.test(stack.peek()))
            out.push(stack.pop());
    }

    public void drainStackToOut() {
        drainStackToOut(x -> true);
    }


    public void checkStackSize(Location location, int depth) {
        if (stack.size() < depth)
            throw new ParseException(location, "Stack size %d is less than %d", stack.size(), depth);
    }

    public void pushStack(Node node) {
        log.trace("stack.push {}", node);
        stack.push(node);
    }

    public Node lookupStack(Location location, int depth) {
        checkStackSize(location, depth);

        Iterator<Node> it = stack.iterator();
        Node node = null;
        for (int i = 0; i < depth; i++)
            node = it.next();

        return node;
    }


    public Node peekStack() {
        Node node = stack.peek();
        log.trace("stack.peek {}", node);
        return node;
    }

    public Node popStack() {
        Node node = stack.pop();
        log.trace("stack.pop {}", node);
        return node;
    }

    public boolean isEmptyStack() {
        return stack.isEmpty();
    }


    public void pushOut(Node node) {
        log.trace("out.push {}", node);
        out.push(node);
    }

    public Node peekOut() {
        Node node = out.peek();
        log.trace("out.peek {}", node);
        return node;
    }

    public Node popOut() {
        Node node = out.pop();
        log.trace("out.pop {}", node);
        return node;
    }

    public boolean isEmptyOut() {
        return out.isEmpty();
    }

}
