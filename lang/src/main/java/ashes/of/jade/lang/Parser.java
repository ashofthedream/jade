package ashes.of.jade.lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;



public class Parser {

    private int index = -1;
    private final List<Lexem> lexems;
    private final Deque<Node> nodes = new ArrayDeque<>();


    public Parser(List<Lexem> lexems) {
        this.lexems = lexems;
    }

    private class State {
        public LambdaNode closure;
        public Deque<Node> stack = new ArrayDeque<>();
        public Deque<Node> out = new ArrayDeque<>();
    }

    private final List<State> scopes = new ArrayList<>();
    private int currentScope;

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
        currentScope = 0;
        scopes.add(new State());
        System.out.printf("parse> %-25s %3s  %-100s %-100s %n", "lexem", "ST#", "STACK", "OUT");

        for (int i = 0; i < lexems.size(); i++) {
            Lexem lexem = lexems.get(i);
            State current = scopes.get(currentScope);
            Deque<Node> stack = current.stack;
            Deque<Node> out = current.out;

            System.out.printf("parse> %-25s %3d  %-100s %-100s %n", lexem, currentScope, stack, out);

            if (lexem.is(LexemType.IntegerNumber)) {
                out.push(parseInt(lexem));
            }

            if (lexem.is(LexemType.DoubleNumber)) {
                out.push(parseDouble(lexem));
            }

            if (lexem.is(LexemType.String)) {
                out.push(parseString(lexem));
            }


            if (isOperator(lexem)) {
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), lexem))
                    out.push(stack.pop());

                stack.push(new Node(lexem));
            }

            if (lexem.is(LexemType.Arrow)) {
                System.out.println();
            }

            if (lexem.is(LexemType.CurlyOpen)) {
                stack.push(new Node(new Lexem(LexemType.Seq, lexem.getLocation())));
                stack.push(new Node(lexem));
            }


            if (lexem.is(LexemType.CurlyClose)) {
                while (!stack.isEmpty() && !stack.peek().is(LexemType.CurlyOpen))
                    out.push(stack.pop());

                stack.pop();
                if (!stack.isEmpty() && stack.peek().is(LexemType.Seq))
                    out.push(stack.pop());
            }


            if (lexem.is(LexemType.ParentOpen)) {
                stack.push(new Node(lexem));
            }

            if (lexem.is(LexemType.ParentClose)) {
                while (!stack.isEmpty() && !stack.peek().is(LexemType.ParentOpen))
                    out.push(stack.pop());

                
                if (currentScope != 0 && stack.isEmpty()) {
                    i--;
                    System.out.printf("EOS^^^ %-25s %3d  %-100s %-100s %n", lexem, currentScope, stack, out);
                    currentScope--;
                    LambdaNode closure = current.closure;
                    closure.stack = out;
                    scopes.get(currentScope).out.push(closure);
                    continue;
                }

                stack.pop();
                if (!stack.isEmpty() && isFunction(stack.peek()))
                    out.push(stack.pop());
            }


            if (lexem.is(LexemType.Map) || lexem.is(LexemType.Reduce) || lexem.is(LexemType.Out) || lexem.is(LexemType.Print)) {
                stack.push(new Node(lexem));
            }


            if (lexem.is(LexemType.Comma)) {
                while (!stack.isEmpty() && !stack.peek().is(LexemType.ParentOpen) && !stack.peek().is(LexemType.CurlyOpen))
                    out.push(stack.pop());
            }

            if (lexem.is(LexemType.Var)) {
                stack.push(new Node(lexem));
            }

            if (lexem.is(LexemType.Assign)) {
                Node id = out.pop();
                checkAndPop(stack, "Expected node %s", "var");

                stack.push(new Node(new Lexem(LexemType.STORE, id.getLocation(), id.getContent())));
            }

            if (lexem.is(LexemType.Arrow)) {
                State closureState = new State();
                currentScope++;
                scopes.add(closureState);

                while (!out.isEmpty() && out.peek().is(LexemType.LOAD)) {
                    Node pop = out.pop();
                    closureState.out.push(new Node(new Lexem(LexemType.STORE, pop.getLocation(), pop.getContent())));
                }

                closureState.closure = new LambdaNode(new Lexem(LexemType.LAMBDA, lexem.getLocation(), lexem.getContent()));
            }


            if (lexem.is(LexemType.Identifier)) {
                out.push(new Node(new Lexem(LexemType.LOAD, lexem.getLocation(), lexem.getContent())));
            }


            if (lexem.is(LexemType.NewLine) || lexem.is(LexemType.EOF)) {
                while (!stack.isEmpty()) {
                    Node pop = stack.pop();
                    out.push(pop);
                }

                out.push(new Node(lexem));
            }
        }


        Deque<Node> stack = scopes.get(0).stack;
        Deque<Node> out = scopes.get(0).out;

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
        return new StringNode(lexem, lexem.getContent());
    }

    private DoubleNode parseDouble(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new DoubleNode(lexem, Double.parseDouble(value));
        } catch (Exception e) {
            error("Invalid integer", value);
            return null;
        }
    }

    private IntNode parseInt(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new IntNode(lexem, Integer.parseInt(value));
        } catch (Exception e) {
            error("Invalid integer", value);
            return null;
        }
    }



    private int precedenceOf(LexemType type) {
        switch (type) {
            case Plus:      return 1;
            case Minus:     return 1;
            case Multiply:  return 2;
            case Divide:    return 2;
            case Power:     return 3;
            default:        return 0;
        }
    }
    private boolean isHighPrecedence(Node a, Lexem b) {
        return precedenceOf(a.getType()) >= precedenceOf(b.getType());
    }

    private boolean isHighPrecedence(LexemType a, LexemType b) {
        return precedenceOf(a) >= precedenceOf(b);
    }

    private boolean isOperator(Lexem lexem) {
        return lexem.getType().isOperator();
    }

    private boolean isOperator(Node node) {
        return node.getType().isOperator();
    }

    private boolean isFunction(Node node) {
        return isFunction(node.getType());
    }

    private boolean isFunction(LexemType type) {
        return type == LexemType.Map || type == LexemType.Reduce;
    }

    private boolean hasNext() {
        return index + 1 < lexems.size();
    }

    private Lexem next() {
        if (!hasNext())
            throw new RuntimeException("EOF");

        index++;
//        System.out.println("next: " + current());
        return lexems.get(index);
    }


    //    stmt ::= var identifier = expr | out expr | print "string"


//    private StmtNode parseStmtOut(Lexem lexem) {
//        Node expr = parseExpr();
//        if (expr == null)
//            throw new RuntimeException("out expr is null");
//
//        return new OutNode(expr);
//    }
//
//    private StmtNode parseStmtPrint(Lexem lexem) {
//        Lexem identifier = ensureNext(Identifier);
//        Node expr = parseExpr();
//        if (expr == null)
//            throw new RuntimeException("print expr is null");
//
//        return new VarNode(identifier.getContent(), expr);
//    }

    private Lexem ensureNext(LexemType expected) {
        if (!hasNext())
            throw new RuntimeException("Expected " + expected + " not found");

        Lexem next = next();
        if (next.getType() != expected)
            throw new RuntimeException("Expected " + expected + " but found " + next.getType());

        return next;
    }


    private void error(String msg, Object... args){
        throw new RuntimeException(String.format(msg, args));
    }

}
