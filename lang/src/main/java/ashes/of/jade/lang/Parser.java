package ashes.of.jade.lang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;



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
    public Node parse() {

        while (hasNext()) {
            Lexem lexem = next();
            System.out.println(lexem);

            if (lexem.is(LexemType.Var)) {
                Lexem identifier = ensureNext(LexemType.Identifier);
                Lexem assign = ensureNext(LexemType.Assign);
                Node node = new VarNode(identifier.getContent());

                nodes.push(node);
            }

            if (lexem.is(LexemType.IntegerNumber)) {
                IntNode node = parseInt(lexem);

                Node pop = nodes.pop();
                if (pop.is(LexemType.Var)) {
                    nodes.push(node);
                    nodes.push(pop);
//                    System.out.println("push: " + pop);
                    continue;
                }

//                if ()

                throw new RuntimeException("Unexpected node " + node + " |  " + nodes);
            }

            if (isOperator(lexem)) {
                OpExprNode node = new OpExprNode(lexem);

                Deque<Node> stack = new ArrayDeque<>();
                while (!nodes.isEmpty()) {
                    Node pop = nodes.pop();



//                    if (stack.isEmpty() && !(pop.is(LexemType.IntegerNumber) || pop.is(LexemType.DoubleNumber) || pop.is(LexemType.Identifier)))
//                        throw new IllegalStateException("Expected at least one id|double|int found " + pop + " | " + nodes);
                    stack.push(pop);
                }

//                Node pop = nodes.pop();
//                if (pop.is(LexemType.Var)) {
//                    stack.push(pop);
//                    pop = nodes.pop();
//                }
//
//                while (!nodes.isEmpty()) {
//
//                }
//
//                while (pop.is(LexemType.IntegerNumber) || pop.is(LexemType.DoubleNumber) || pop.is(LexemType.Identifier)) {
//                    stack.push(pop)
//                    node.add(pop);
//                }
//
//                if (x < 1) {
//                    throw new RuntimeException("Unexpected node " + node + " | " + nodes);
//                }


                while (!stack.isEmpty()) {
                    nodes.push(stack.pop());
                }

                throw new RuntimeException("Unexpected node " + node + " " + nodes);


            }

//            if (lexem.is())
        }

        return new ProgramNode(nodes);
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

    private boolean isHighPrecedence(LexemType a, LexemType b) {
        return precedenceOf(a) >= precedenceOf(b);
    }

    private boolean isOperator(Lexem lexem) {
        return isOperator(lexem.getType());
    }

    private boolean isOperator(LexemType lexem) {
        return  lexem == LexemType.Plus ||
                lexem == LexemType.Minus ||
                lexem == LexemType.Multiply ||
                lexem == LexemType.Divide ||
                lexem == LexemType.Power;
    }

    private boolean isFunction(Lexem lexem) {
        return isFunction(lexem.getType());
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

    public Deque<Node> getNodes() {
        return nodes;
    }
}
