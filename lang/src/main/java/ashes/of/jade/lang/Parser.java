package ashes.of.jade.lang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static ashes.of.jade.lang.LexemType.Out;


public class Parser {

    private int index = -1;
    private final List<Lexem> lexems;
    private final Deque<Node> nodes = new ArrayDeque<>();


    public Parser(List<Lexem> lexems) {
        this.lexems = lexems;
    }

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
        Deque<Node> stack = new ArrayDeque<>();
        Deque<Node> out = new ArrayDeque<>();

        for (Lexem lexem : lexems) {
            if (lexem.is(LexemType.IntegerNumber)) {
                out.push(parseInt(lexem));
            }

            if (lexem.is(LexemType.DoubleNumber)) {
                out.push(parseDouble(lexem));
            }

            if (lexem.is(LexemType.String)) {
                out.push(parseString(lexem));
            }

            if (lexem.is(LexemType.Map) || lexem.is(LexemType.Reduce) || lexem.is(Out)) {
                stack.push(new LexemNode(lexem));
            }


            if (lexem.is(LexemType.Comma)) {
                while (!stack.isEmpty() && !stack.peek().is(LexemType.ParentOpen))
                    out.push(stack.pop());
            }

            if (isOperator(lexem)) {
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), lexem))
                    out.push(stack.pop());

                stack.push(new LexemNode(lexem));
            }


            if (lexem.is(LexemType.ParentOpen)) {
                stack.push(new LexemNode(lexem));
            }

            if (lexem.is(LexemType.ParentClose)) {
                while (!stack.isEmpty() && !stack.peek().is(LexemType.ParentOpen))
                    out.push(stack.pop());

                stack.pop();
                if (!stack.isEmpty() && isFunction(stack.peek()))
                    out.push(stack.pop());
            }


            if (lexem.is(LexemType.NewLine)) {
                while (!stack.isEmpty()) {
                    Node pop = stack.pop();
                    out.push(pop);
                }

                out.push(new LexemNode(lexem));
            }

            if (lexem.is(LexemType.Var)) {
                stack.push(new LexemNode(lexem));
            }

            if (lexem.is(LexemType.Identifier)) {
                if (!stack.isEmpty() && stack.peek().is(LexemType.Var)) {
                    stack.pop();
                    stack.push(new LexemNode(new Lexem(LexemType.STORE, lexem.getLocation(), lexem.getContent())));
                    continue;
                }

                out.push(new LexemNode(new Lexem(LexemType.LOAD, lexem.getLocation(), lexem.getContent())));
            }
        }

        while (!stack.isEmpty())
            out.push(stack.pop());

        return out;
    }



    private StringNode parseString(Lexem lexem) {
        return new StringNode(lexem.getContent());
    }

    private DoubleNode parseDouble(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new DoubleNode(Double.parseDouble(value));
        } catch (Exception e) {
            error("Invalid integer", value);
            return null;
        }
    }

    private IntNode parseInt(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new IntNode(Integer.parseInt(value));
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
            throw new RuntimeException("Eof");

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
