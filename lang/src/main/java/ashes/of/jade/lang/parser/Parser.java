package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.nodes.LambdaNode;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.NodeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import static ashes.of.jade.lang.nodes.NodeUtil.createNodeFromLexem;
import static ashes.of.jade.lang.nodes.NodeUtil.isFunction;
import static ashes.of.jade.lang.nodes.NodeUtil.isHighPrecedence;
import static ashes.of.jade.lang.nodes.NodeUtil.isOperator;


public class Parser {
    private static final Logger log = LogManager.getLogger(Parser.class);


    private class Scope {
        public int function = 0;
        public int sequence = 0;

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

        public void pushStack(Node node) {
            log.trace("stack.push {}", node);
            stack.push(node);
        }

        public Node popStack() {
            Node node = stack.pop();
            log.trace("stack.pop {}");
            return node;
        }

        public boolean isEmptyStack() {
            return stack.isEmpty();
        }


        public void pushOut(Node node) {
            log.trace("out.push {}", node);
            out.push(node);
        }

        public Node popOut() {
            Node node = out.pop();
            log.trace("out.pop {}");
            return node;
        }

        public boolean isEmptyOut() {
            return out.isEmpty();
        }
    }


    private final Deque<Scope> scopes = new ArrayDeque<>();

    /**
     * expr ::= expr op expr | (expr) | identifier | { expr, expr } | number | map(expr, identifier -> expr) | reduce(expr, expr, identifier identifier -> expr)
     * op ::= + | - | * | / | ^
     * stmt ::= var identifier = expr | out expr | print "string"
     * program ::= stmt | program stmt
     * <p>
     * <p>
     * var n = 500
     * var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))
     * var pi = 4 * reduce (sequence, 0, x y -> x + y)
     * print "pi = "
     * out pi
     */
    public Deque<Node> parse(List<Lexem> lexems) {
        scopes.push(new Scope());
        for (int i = 0; i < lexems.size(); i++) {
            Lexem lexem = lexems.get(i);
            Scope current = scopes.peek();

            log.debug("parse {}   @ {}", lexem, i);
            log.trace("stack <-- {}", current.stack);
            log.trace("out   <-- {}", current.out);


            switch (lexem.getType()) {
                case INTEGER:
                case DOUBLE:
                case STRING:
                case LOAD:
                    parseVar(current, lexem);
                    break;

                case STORE:
                    parseStore(current, lexem);
                    break;

                case PLUS:
                case MINUS:
                case MULTIPLY:
                case DIVIDE:
                case POWER:
                    parseOperator(current, lexem);
                    break;

                case MAP:
                case REDUCE:
                    parseMapAndReduce(current, lexem);
                    break;

                case OUT:
                case PRINT:
                    parsePrintAndOut(current, lexem);
                    break;

                case CURLY_OPEN:
                    parseCurlyOpen(current, lexem);
                    break;

                case CURLY_CLOSE:
                    parseCurlyClose(current, lexem);
                    break;


                case PARENT_OPEN:
                    parseParentOpen(current, lexem);
                    break;

                case PARENT_CLOSE:
                    if (parseParentClose(current, lexem))
                        i--;
                    break;


                case COMMA: parseComma(current, lexem); break;
                case ARROW: parseArrow(current, lexem); break;

                case NL:
                case EOF:   parseNewLineAndEOF(current, lexem); break;
            }
        }

        Scope scope = scopes.pop();
        log.trace("End of parse. stack -> push");
        scope.drainStackToOut();

        log.trace("out   <-- {}", scope.out);
        return scope.out;
    }


    private void parseVar(Scope scope, Lexem lexem) {
        checkIsNotStmtStart(scope, lexem.getLocation());
        scope.pushOut(createNodeFromLexem(lexem));
    }


    private void parseStore(Scope scope, Lexem lexem) {
        scope.pushStack(createNodeFromLexem(lexem));
    }


    private void parseOperator(Scope scope, Lexem lexem) {
        Node op = createNodeFromLexem(lexem);
        scope.drainStackToOut(peek -> isOperator(peek) && isHighPrecedence(peek, op));
        scope.pushStack(op);
    }


    private void parsePrintAndOut(Scope scope, Lexem lexem) {
        checkIsStmtStart(scope, lexem.getLocation());
        scope.pushStack(createNodeFromLexem(lexem));
    }


    private void parseMapAndReduce(Scope scope, Lexem lexem) {
        scope.function++;
        scope.pushStack(createNodeFromLexem(lexem));
    }


    private void parseCurlyOpen(Scope scope, Lexem lexem) {
        checkIsNotStmtStart(scope, lexem.getLocation());

        scope.sequence++;
        scope.pushStack(createNodeFromLexem(lexem));
    }

    private void parseCurlyClose(Scope scope, Lexem lexem) {
        if (scope.out.size() < 2)
            throw new ParseException(lexem.getLocation(), "Sequence should contains at two expressions");

        scope.drainStackToOut(peek -> !peek.is(NodeType.CURLY_OPEN));

        if (scope.isEmptyStack())
            throw new ParseException(lexem.getLocation(), "No sequence start found");

        Node open = scope.popStack();
        scope.sequence--;
        scope.pushOut(new Node(NodeType.NEWSEQUENCE, open.getLocation()));
    }



    private void parseParentOpen(Scope scope, Lexem lexem) {
        checkIsNotStmtStart(scope, lexem.getLocation());
        scope.pushStack(createNodeFromLexem(lexem));
    }

    private boolean parseParentClose(Scope scope, Lexem lexem) {
        scope.drainStackToOut(peek -> !peek.is(NodeType.PARENT_OPEN));

        if (scopes.size() > 1 && scope.isEmptyStack()) {
            log.trace("ParentClose). it's a lambda. stack -> out. back");
            LambdaNode closure = scope.lambda;
            closure.stack = scope.out;
            scopes.pop();
            Scope main = scopes.peek();
            main.pushOut(closure);

            log.debug("main out.push {}", closure);
            return true;
        }

        if (scope.isEmptyStack())
            throw new ParseException(lexem.getLocation(), "Unexpected symbol )");

        scope.popStack();

        if (!scope.isEmptyStack() && isFunction(scope.stack.peek())) {
            scope.function--;
            log.trace("ParentClose). Stack isn't empty and .peek is function, decrease ", scope.function);
            scope.pushOut(scope.popStack());
        }

        return false;
    }


    private void parseArrow(Scope global, Lexem lexem) {
        log.trace("Arrow. push new Scope");
        Scope current = new Scope();
        scopes.push(current);

        while (!global.isEmptyOut()) {
            Node pop = global.popOut();
            if (pop.is(NodeType.COMMA))
                break;

            current.pushOut(new Node(NodeType.STORE, pop.getLocation(), pop.getContent()));
        }

        current.lambda = new LambdaNode(lexem.getLocation());
    }


    private void parseComma(Scope scope, Lexem lexem) {
        scope.drainStackToOut(n -> !n.is(NodeType.PARENT_OPEN) && !n.is(NodeType.CURLY_OPEN));

        if (scope.function > 0) {
            log.trace("Comma. Scope function={}", scope.function);
            scope.pushOut(createNodeFromLexem(lexem));
        }
    }


    private void parseNewLineAndEOF(Scope scope, Lexem lexem) {
        if (scope.sequence > 0)
            throw new ParseException(lexem.getLocation(), "Unexpected EOF");

        scope.drainStackToOut();
        scope.pushOut(createNodeFromLexem(lexem));
    }


    private void checkIsStmtStart(Scope scope, Location location) {
        Deque<Node> out = scope.out;
        Deque<Node> stack = scope.stack;
        if (false)
            throw new ParseException(location, "Can't be part of expression");
    }

    private void checkIsNotStmtStart(Scope scope, Location location) {
        Deque<Node> out = scope.out;
        Deque<Node> stack = scope.stack;

        if (!out.isEmpty() && checkOut(out.peek()) && !stack.isEmpty() && checkStack(stack.peek()))
            throw new ParseException(location, "Statement expected");
    }

    private boolean checkStack(Node node) {
        return !(isOperator(node) || isFunction(node) || node.is(NodeType.PARENT_OPEN) || node.is(NodeType.CURLY_OPEN));
    }

    private boolean checkOut(Node node) {
        return  node.is(NodeType.LOAD) ||
                node.is(NodeType.INTEGER) ||
                node.is(NodeType.DOUBLE) ||
                node.is(NodeType.SEQUENCE) ||
                node.is(NodeType.NEWSEQUENCE) ||
                node.is(NodeType.STRING);
    }
}
