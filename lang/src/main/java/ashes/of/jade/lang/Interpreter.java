package ashes.of.jade.lang;

import java.util.*;
import java.util.stream.Collectors;

import static ashes.of.jade.lang.LexemType.NewLine;
import static ashes.of.jade.lang.LexemType.Out;


public class Interpreter {


    public static void main(String... args) {
        String expr =
//                "3 + out((1 + 7) * 4)\n" +
//                "var a = 0\n" +
//                "var b = (a * 2) + 1\n" +
//                "out(7)\n" +
                "var fiveHundred = 400 + 50 * (3 - 1) / 2\n" +
                "var n = 0 + fiveHundred\n" +
//                "var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))\n" +
//                "var pi = 3.1415 * reduce (sequence, 0, x y -> x + y)\n" +
//                "print \"pi = \"\n" +
                "out fiveHundred\n" +
                "" ;

        try {
            Lexer lexer = new Lexer();
            List<Lexem> lexems = lexer.parse(expr);

            System.out.println(expr);
            System.out.println();
            System.out.println(lexems);
            System.out.println();

//            Parser parser = new Parser(lexems);
//            Node node = parser.parse();

//            System.out.println(node);
            Interpreter interpreter = new Interpreter();
            System.out.println(interpreter.eval(lexems));
        } catch (ParseException e) {

            StringBuilder b = new StringBuilder()
                    .append(e.getCode())
                    .append("\n");

            Location location = e.getLocation();
            for (int i = 0; i < location.offset; i++) {
                b.append(" ");
            }

            b.append("^ ").append(e.getMessage());

            System.err.println(b);
        }
    }

    private Deque<Lexem> rpn(List<Lexem> lexems) {
        Deque<Lexem> stack = new ArrayDeque<>();
        Deque<Lexem> out = new ArrayDeque<>();

        for (Lexem lexem : lexems) {
            if (lexem.is(LexemType.IntegerNumber) || lexem.is(LexemType.DoubleNumber)) {
                out.push(lexem);
            }

            if (lexem.is(LexemType.Map) || lexem.is(LexemType.Reduce) || lexem.is(Out)) {
                stack.push(lexem);
            }


            if (lexem.is(LexemType.Comma)) {
                while (!stack.isEmpty() && !stack.peek().is(LexemType.ParentOpen))
                    out.push(stack.pop());
            }

            if (isOperator(lexem)) {
                while (!stack.isEmpty() && isOperator(stack.peek()) && isHighPrecedence(stack.peek(), lexem))
                    out.push(stack.pop());

                stack.push(lexem);
            }


            if (lexem.is(LexemType.ParentOpen)) {
                stack.push(lexem);
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
                    Lexem pop = stack.pop();
                    out.push(pop);
                }

                out.push(lexem);
            }

            if (lexem.is(LexemType.Var)) {
                stack.push(lexem);
            }

            if (lexem.is(LexemType.Identifier)) {
                if (!stack.isEmpty() && stack.peek().is(LexemType.Var)) {
                    stack.pop();
                    stack.push(new Lexem(LexemType.STORE, lexem.getLocation(), lexem.getContent()));
                    continue;
                }

                out.push(new Lexem(LexemType.LOAD, lexem.getLocation(), lexem.getContent()));
            }
        }

        while (!stack.isEmpty())
            out.push(stack.pop());

        return out;
    }

    private double eval(List<Lexem> lexems) {
        Deque<Lexem> rpn = rpn(lexems);
        System.out.println("byLineStack:");
        rpn.stream()
                .map(x -> x.getType() == NewLine ? "\n" : x.toString())
                .forEach(System.out::println);

        System.out.println();
        Deque<Node> stack = new ArrayDeque<>();
        List<Map<String, Node>> scopes = new ArrayList<>();
        scopes.add(new HashMap<>());
        int scope = 0;
        while (!rpn.isEmpty()) {
            Lexem lexem = rpn.removeLast();
            System.out.println("exec: " + lexem + "    scope: " + scopes.get(scope) + "    stack: " + stack );
            if (lexem.is(LexemType.IntegerNumber)) {
                stack.push(new DoubleNode(Double.parseDouble(lexem.getContent())));
                continue;
            }

            if (lexem.is(LexemType.STORE)) {
                Node a = stack.pop();
                scopes.get(0).put(lexem.getContent(), new DoubleNode(a.asDouble()));
            }

            if (lexem.is(LexemType.LOAD)) {
                Node val = scopes.get(0).get(lexem.getContent());
                stack.push(new DoubleNode(val.asDouble()));
            }

//            if (lexem.is(Identifier)) {
//                stack.p
////                Double val = scope.get(lexem.getContent());
////                if (val == null)
////                stack.pop()
//            }

            if (isOperator(lexem)) {
                Node a = stack.pop();
                Node b = stack.pop();
                switch (lexem.getType()) {
                    case Plus:      stack.push(new DoubleNode(b.asDouble() + a.asDouble())); break;
                    case Minus:     stack.push(new DoubleNode(b.asDouble() - a.asDouble())); break;
                    case Multiply:  stack.push(new DoubleNode(b.asDouble() * a.asDouble())); break;
                    case Divide:    stack.push(new DoubleNode(b.asDouble() / a.asDouble())); break;
                    case Power:     stack.push(new DoubleNode( Math.pow(b.asDouble(), a.asDouble()))); break;

                }
            }
        }


        System.out.println("scope: " + scopes.get(scope) + "    stack: " + stack );
        return stack.pop().asDouble();
    }



    private boolean isHighPrecedence(Lexem a, Lexem b) {
        return precedenceOf(a) >= precedenceOf(b);
    }


    private int precedenceOf(Lexem lexem) {
        switch (lexem.getType()) {
            case Plus:      return 1;
            case Minus:     return 1;
            case Multiply:  return 2;
            case Divide:    return 2;
            case Power:     return 3;
            default:        return 0;
        }
    }

    private boolean isFunction(Lexem lexem) {
        return lexem.is(LexemType.Map) || lexem.is(LexemType.Reduce) || lexem.is(Out);
    }

    private boolean isOperator(Lexem lexem) {
        return  lexem.is(LexemType.Plus) ||
                lexem.is(LexemType.Minus) ||
                lexem.is(LexemType.Multiply) ||
                lexem.is(LexemType.Divide) ||
                lexem.is(LexemType.Power);
    }
}


abstract class StmtNode implements Node {
}


class OutNode extends StmtNode {
    private Node expr;

    public OutNode(Node expr) {
        this.expr = expr;
    }

    @Override
    public LexemType getType() {
        return LexemType.Out;
    }

    @Override
    public Node eval() {
        System.out.println(expr.asString());
        return null;
    }
}



class PrintNode extends StmtNode {
    private Node expr;

    public PrintNode(Node expr) {
        this.expr = expr;
    }

    @Override
    public LexemType getType() {
        return LexemType.Print;
    }

    @Override
    public Node eval() {
        System.out.println(expr.asString());
        return null;
    }
}


class VarNode extends StmtNode {
    private String identifier;
    private Node expr;

    public VarNode(String identifier) {
        this.identifier = identifier;
    }

    public void add(Node expr) {
        this.expr = expr;
    }

    @Override
    public LexemType getType() {
        return LexemType.Var;
    }

    @Override
    public String toString() {
        return "VarNode{" + identifier + " = " + expr + "}";
    }
}



class OpExprNode implements Node {
    private Lexem op;
    private Node l, r;

    public OpExprNode(Lexem op) {
        this.op = op;
//        this.l = left;
//        this.r = right;
    }

    public void addLeft(Node node) {
        this.l = node;
    }

    public void addRight(Node node) {
        this.r = node;
    }

    @Override
    public LexemType getType() {
        return op.getType();
    }

    public Node eval() {
        switch (op.getType()) {
            case Plus:
                if (l.isDouble() || r.isDouble())
                    return new DoubleNode(l.asDouble() + l.asDouble());

                if (l.isInteger() && r.isInteger())
                    return new IntNode(l.asInteger() + l.asInteger());

                throw new RuntimeException("Can't " + l + " + " + r);

            case Minus:
                if (l.isDouble() || r.isDouble())
                    return new DoubleNode(l.asDouble() - l.asDouble());

                if (l.isInteger() && r.isInteger())
                    return new IntNode(l.asInteger() - l.asInteger());

                throw new RuntimeException("Can't " + l + " - " + r);

            case Multiply:
                if (l.isDouble() || r.isDouble())
                    return new DoubleNode(l.asDouble() * l.asDouble());

                if (l.isInteger() && r.isInteger())
                    return new IntNode(l.asInteger() * l.asInteger());

                throw new RuntimeException("Can't " + l + " * " + r);

            case Divide:
                if (l.isDouble() || r.isDouble())
                    return new DoubleNode(l.asDouble() / l.asDouble());

                if (l.isInteger() && r.isInteger())
                    return new IntNode(l.asInteger() / l.asInteger());

                throw new RuntimeException("Can't " + l + " / " + r);
        }

        throw new RuntimeException("Unknown operator: " + op);
    }

    @Override
    public String toString() {
        return "OpExprNode{" + l + " " + op + " " + r + "}";
    }
}


class ParentnessNode implements Node {
    private Node[] nodes;


}


class ExprNode implements Node {

    private Lexem lexem;
    private Node node;

    public ExprNode(Lexem lexem) {
        this.lexem = lexem;
    }


    @Override
    public boolean is(LexemType type) {
        return lexem.is(type);
    }

    public void add(ExprNode expr) {
        this.node = expr;
    }

    @Override
    public String toString() {
        return "ExprNode{" + lexem + " " + node + '}';
    }
}


class ProgramNode implements Node {
    private final Collection<Node> nodes;

    public ProgramNode(Collection<Node> nodes) {
        this.nodes = nodes;
    }

    @Override
    public LexemType getType() {
        return LexemType.Program;
    }

    @Override
    public Node eval() {
        for (Node node : nodes)
            node.eval();

        return null;
    }

    @Override
    public String toString() {
        return nodes.stream()
                .map(n -> "\t" + n)
                .collect(Collectors.joining("\n", "ProgramNode{\n", "\n}"));
    }
}


