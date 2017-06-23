package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.nodes.*;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.LexemType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;



public class Parser {
    private static final Logger log = LogManager.getLogger(Parser.class);


    private class Scope {
        public LambdaNode closure;
        public Deque<Node> stack = new ArrayDeque<>();
        public Deque<Node> out = new ArrayDeque<>();
    }


    private final Deque<Scope> scopes = new ArrayDeque<>();

    /**
     * expr ::= expr op expr | (expr) | identifier | { expr, expr } | number | map(expr, identifier -> expr) | reduce(expr, expr, identifier identifier -> expr)
     * op ::= + | - | * | / | ^
     * stmt ::= var identifier = expr | out expr | print "string"
     * program ::= stmt | program stmt
     *
     *
     * var n = 500
     * var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))
     * var pi = 4 * reduce (sequence, 0, x y -> x + y)
     * print "pi = "
     * out pi
     */
    public Deque<Node> parse(List<Lexem> lexems) {
        boolean function = false;
        scopes.push(new Scope());
        for (int i = 0; i < lexems.size(); i++) {
            Lexem lexem = lexems.get(i);
            Scope current = scopes.peek();
            Deque<Node> stack = current.stack;
            Deque<Node> out = current.out;

            log.debug("parse {}   @ {}", lexem, i);
            log.trace("stack <-- {}", stack);
            log.trace("out   <-- {}", out);

            if (lexem.is(LexemType.IntegerNumber)) {
                IntNode node = parseInt(lexem);
                log.trace("Op Plus +. stack -> out. out.push {}", node);
                out.push(node);
            }

            if (lexem.is(LexemType.DoubleNumber)) {
                DoubleNode node = parseDouble(lexem);
                log.trace("Op Plus +. stack -> out. out.push {}", node);
                out.push(node);
            }

            if (lexem.is(LexemType.String)) {
                StringNode node = parseString(lexem);
                log.trace("Op Plus +. stack -> out. out.push {}", node);
                out.push(node);
            }



            if (lexem.is(LexemType.Plus)) {
                Node n = new Node(NodeType.ADD, lexem.getLocation());
                log.trace("Op Plus +. stack -> out. stack.push {}", n);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Minus)) {
                Node n = new Node(NodeType.SUB, lexem.getLocation());
                log.trace("Op Minus -. stack -> out. stack.push {}", n);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Multiply)) {
                Node n = new Node(NodeType.MUL, lexem.getLocation());
                log.trace("Op Multiply *. stack -> out. stack.push {}", n);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Divide)) {
                Node n = new Node(NodeType.DIV, lexem.getLocation());
                log.trace("Op Divide /. stack -> out. stack.push {}", n);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Power)) {
                Node n = new Node(NodeType.POWER, lexem.getLocation());
                log.trace("Op Power ^. stack -> out. stack.push {}", n);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.CurlyOpen)) {
                Node node = new Node(NodeType.CurlyOpen, lexem.getLocation());
                log.trace("CurlyOpen{. stack.push {}", node);
                stack.push(node);
            }


            if (lexem.is(LexemType.CurlyClose)) {
                log.trace("CurlyClose). stack -> out");
                while (!stack.isEmpty() && !stack.peek().is(NodeType.CurlyOpen))
                    out.push(stack.pop());

                stack.pop();
                Node node = new Node(NodeType.SEQ);
                log.trace("CurlyClose}. stack.push {}", node);
                stack.push(node);
            }


            if (lexem.is(LexemType.ParentOpen)) {
                stack.push(new Node(NodeType.ParentOpen));
            }

            if (lexem.is(LexemType.ParentClose)) {
                log.trace("ParentClose). stack -> out");
                while (!stack.isEmpty() && !stack.peek().is(NodeType.ParentOpen))
                    out.push(stack.pop());

                
                if (scopes.size() > 1 && stack.isEmpty()) {
                    log.trace("ParentClose). it's a lambda. stack -> out. back");
                    i--;
                    LambdaNode closure = current.closure;
                    closure.stack = out;
                    scopes.pop();
                    Scope main = scopes.peek();
                    main.out.push(closure);

                    log.debug("main out.push {}", closure);
                    continue;
                }

                stack.pop();
                function = !stack.isEmpty() && !isFunction(stack.peek());
                log.trace("ParentClose). Change vars function={}. stack -> out", function);
                if (!stack.isEmpty() && isFunction(stack.peek()))
                    out.push(stack.pop());

            }


            if (lexem.is(LexemType.Map)) {
                function = true;
                Node node = new Node(NodeType.MAP);
                log.trace("Map. function=true. stack.push {}", node);
                stack.push(node);
            }

            if (lexem.is(LexemType.Reduce)) {
                function = true;
                Node node = new Node(NodeType.REDUCE);
                log.trace("Reduce. function=true. stack.push {}", node);
                stack.push(node);
            }

            if (lexem.is(LexemType.Out)) {
                function = true;
                Node node = new Node(NodeType.OUT);
                log.trace("Out. function=true. stack.push {}", node);
                stack.push(node);
            }

            if (lexem.is(LexemType.Print)) {
                function = true;
                Node node = new Node(NodeType.PRINT);
                log.trace("Print. function=true. stack.push {}", node);
                stack.push(node);
            }


            if (lexem.is(LexemType.Comma)) {
                log.trace("Comma. stack -> pop");
                while (!stack.isEmpty() && !stack.peek().is(NodeType.ParentOpen) && !stack.peek().is(NodeType.CurlyOpen))
                    out.push(stack.pop());

                if (function) {
                    Node node = new Node(NodeType.COMMA, lexem.getLocation());
                    log.trace("Comma. Scope function -> out.push {}", node);
                    out.push(node);
                }
            }


            if (lexem.is(LexemType.Store)) {
                Node node = new Node(NodeType.STORE, lexem.getLocation(), lexem.getContent());
                log.trace("Store. stack.push {}", node);
                stack.push(node);
            }

            if (lexem.is(LexemType.Arrow)) {
                log.trace("Arrow. push new Scope");
                Scope closureScope = new Scope();

                scopes.push(closureScope);

                while (!out.isEmpty()) {
                    Node pop = out.pop();
                    if (pop.is(NodeType.COMMA))
                        break;

                    closureScope.out.push(new Node(NodeType.STORE, pop.getLocation(), pop.getContent()));
                }

                closureScope.closure = new LambdaNode(lexem.getLocation());
            }


            if (lexem.is(LexemType.Load)) {
                Node node = new Node(NodeType.LOAD, lexem.getLocation(), lexem.getContent());
                log.trace("Arrow. out.push {}", node);
                out.push(node);
            }


            if (lexem.is(LexemType.NewLine) || lexem.is(LexemType.EOF)) {
                log.trace("NewLine | EOF. stack -> pop");
                while (!stack.isEmpty())
                    out.push(stack.pop());

                // todo it's a temporal
                Node node = new Node(lexem.is(LexemType.NewLine) ? NodeType.NL : NodeType.EOF);
                log.trace("NewLine | EOF. out.push {}", node);
                out.push(node);
            }
        }


        Scope scope = scopes.pop();
        Deque<Node> stack = scope.stack;
        Deque<Node> out = scope.out;

        log.trace("End of parse. stack -> push");
        while (!stack.isEmpty())
            out.push(stack.pop());

        return out;
    }

    private Node checkAndPop(Deque<Node> stack, String msg, Object... args) {
        if (stack.isEmpty())
            throw new RuntimeException(String.format(msg, args) +  ", but not found");

        return stack.pop();
    }


    private StringNode parseString(Lexem lexem) {
        return new StringNode(lexem.getLocation(), lexem.getContent());
    }

    private DoubleNode parseDouble(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new DoubleNode(lexem.getLocation(), Double.parseDouble(value));
        } catch (Exception e) {
            error("Invalid integer", value);
            return null;
        }
    }

    private IntNode parseInt(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new IntNode(lexem.getLocation(), Integer.parseInt(value));
        } catch (Exception e) {
            error("Invalid integer", value);
            return null;
        }
    }



    private int precedenceOf(Node node) {
        switch (node.getType()) {
            case ADD:   return 1;
            case SUB:   return 1;
            case MUL:   return 2;
            case DIV:   return 2;
            case POWER: return 3;
            default:    return 0;
        }
    }
    private boolean isHighPrecedence(Node a, Node b) {
        return precedenceOf(a) >= precedenceOf(b);
    }


    private boolean isOperator(Node node) {
        return precedenceOf(node) > 0;
    }

    private boolean isFunction(Node node) {
        return isFunction(node.getType());
    }

    private boolean isFunction(NodeType type) {
        return type == NodeType.MAP || type == NodeType.REDUCE;
    }

    private void error(String msg, Object... args) {
        throw new RuntimeException(String.format(msg, args));
    }

}
