package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.nodes.*;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.LexemType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;



public class Parser {

    private final List<Lexem> lexems;

    public Parser(List<Lexem> lexems) {
        this.lexems = lexems;
    }

    private class State {
        public LambdaNode closure;
        public Deque<Node> stack = new ArrayDeque<>();
        public Deque<Node> out = new ArrayDeque<>();
    }

    private final Deque<State> scopes = new ArrayDeque<>();

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
    public Deque<Node> parse() {
        scopes.push(new State());
        System.out.printf("parse> %-25s %3s  %-100s %-100s %n", "lexem", "ST#", "STACK", "OUT");

        for (int i = 0; i < lexems.size(); i++) {
            Lexem lexem = lexems.get(i);
            State current = scopes.peek();
            Deque<Node> stack = current.stack;
            Deque<Node> out = current.out;

            System.out.printf("parse> %-25s %3d  %-100s %-100s %n", lexem, scopes.size(), stack, out);

            if (lexem.is(LexemType.IntegerNumber)) {
                out.push(parseInt(lexem));
            }

            if (lexem.is(LexemType.DoubleNumber)) {
                out.push(parseDouble(lexem));
            }

            if (lexem.is(LexemType.String)) {
                out.push(parseString(lexem));
            }



            if (lexem.is(LexemType.Plus)) {
                Node n = new Node(NodeType.ADD);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Minus)) {
                Node n = new Node(NodeType.SUB);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Multiply)) {
                Node n = new Node(NodeType.MUL);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Divide)) {
                Node n = new Node(NodeType.DIV);

                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }

            if (lexem.is(LexemType.Power)) {
                Node n = new Node(NodeType.POWER);
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), n))
                    out.push(stack.pop());

                stack.push(n);
            }




            if (lexem.is(LexemType.Arrow)) {
                System.out.println();
            }

            if (lexem.is(LexemType.CurlyOpen)) {
                stack.push(new Node(NodeType.CurlyOpen, lexem.getLocation()));
            }


            if (lexem.is(LexemType.CurlyClose)) {
                while (!stack.isEmpty() && !stack.peek().is(NodeType.CurlyOpen))
                    out.push(stack.pop());

                stack.pop();
                stack.push(new Node(NodeType.SEQ));
            }


            if (lexem.is(LexemType.ParentOpen)) {
                stack.push(new Node(NodeType.ParentOpen));
            }

            if (lexem.is(LexemType.ParentClose)) {
                while (!stack.isEmpty() && !stack.peek().is(NodeType.ParentOpen))
                    out.push(stack.pop());

                
                if (scopes.size() > 1 && stack.isEmpty()) {
                    i--;
                    System.out.printf("EOS^^^ %-25s %3d  %-100s %-100s %n", lexem, scopes.size(), stack, out);
                    System.out.println();

                    LambdaNode closure = current.closure;
                    closure.stack = out;
                    scopes.pop();
                    scopes.peek().out.push(closure);
                    continue;
                }

                stack.pop();
                if (!stack.isEmpty() && isFunction(stack.peek()))
                    out.push(stack.pop());
            }


            if (lexem.is(LexemType.Map)) {
                stack.push(new Node(NodeType.MAP));
            }

            if (lexem.is(LexemType.Reduce)) {
                stack.push(new Node(NodeType.REDUCE));
            }

            if (lexem.is(LexemType.Out)) {
                stack.push(new Node(NodeType.OUT));
            }

            if (lexem.is(LexemType.Print)) {
                stack.push(new Node(NodeType.PRINT));
            }


            if (lexem.is(LexemType.Comma)) {
                while (!stack.isEmpty() && !stack.peek().is(NodeType.ParentOpen) && !stack.peek().is(NodeType.CurlyOpen))
                    out.push(stack.pop());
            }


            if (lexem.is(LexemType.Store)) {
                stack.push(new Node(NodeType.STORE, lexem.getLocation(), lexem.getContent()));
            }

            if (lexem.is(LexemType.Arrow)) {
                State closureState = new State();

                scopes.push(closureState);

                while (!out.isEmpty() && out.peek().is(NodeType.LOAD)) {
                    Node pop = out.pop();
                    closureState.out.push(new Node(NodeType.STORE, pop.getLocation(), pop.getContent()));
                }

                closureState.closure = new LambdaNode(lexem.getLocation());
            }


            if (lexem.is(LexemType.Load)) {
                out.push(new Node(NodeType.LOAD, lexem.getLocation(), lexem.getContent()));
            }


            if (lexem.is(LexemType.NewLine) || lexem.is(LexemType.EOF)) {
                while (!stack.isEmpty()) {
                    Node pop = stack.pop();
                    out.push(pop);
                }

                out.push(new Node(lexem.is(LexemType.NewLine) ? NodeType.NL : NodeType.EOF));
            }
        }


        State state = scopes.pop();
        Deque<Node> stack = state.stack;
        Deque<Node> out = state.out;

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

    private void error(String msg, Object... args){
        throw new RuntimeException(String.format(msg, args));
    }

}
