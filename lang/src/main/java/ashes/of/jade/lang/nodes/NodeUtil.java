package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.LexemType;
import ashes.of.jade.lang.parser.ParseException;

public class NodeUtil {

    public static NodeType nodeTypeByLexem(LexemType lexem) {
        switch (lexem) {
            case PRINT:         return NodeType.PRINT;
            case OUT:           return NodeType.OUT;
            case MAP:           return NodeType.MAP;
            case REDUCE:        return NodeType.REDUCE;
            case COMMA:         return NodeType.COMMA;
            case ARROW:         return null;
            case PLUS:          return NodeType.ADD;
            case MINUS:         return NodeType.SUB;
            case MULTIPLY:      return NodeType.MUL;
            case DIVIDE:        return NodeType.DIV;
            case POWER:         return NodeType.POWER;
            case REMAINDER:     return null;
            case IDENTIFIER:    return NodeType.LOAD;
            case VAR:           return NodeType.VAR;
            case EQUAL:         return NodeType.EQUAL;
            case INTEGER:       return NodeType.INTEGER;
            case DOUBLE:        return NodeType.DOUBLE;
            case STRING:        return NodeType.STRING;
            case CURLY_OPEN:    return NodeType.CURLY_OPEN;
            case CURLY_CLOSE:   return null;
            case PARENT_OPEN:   return NodeType.PARENT_OPEN;
            case PARENT_CLOSE:  return null;
            case EOF:           return NodeType.EOF;
            case NL:            return NodeType.NL;

            default:
                return null;
        }
    }
    

    public static Node createNodeFromLexem(Lexem lexem) {
        NodeType type = nodeTypeByLexem(lexem.getType());
        if (type == null)
            throw new ParseException(lexem.getLocation(), "Can't create node from lexem");

        return createNode(type, lexem.getLocation(), lexem.getContent());
    }

    public static Node createNode(NodeType type, Location location, String content) {
        switch (type) {
            case INTEGER:   return createIntNode(location, content);
            case DOUBLE:    return createDoubleNode(location, content);
            case STRING:    return createStringNode(location, content);
            default:        return new Node(type, location, content);
        }
    }

    private static StringNode createStringNode(Location location, String content) {
        return new StringNode(location, content);
    }

    private static DoubleNode createDoubleNode(Location location, String content) {

        try {
            return new DoubleNode(location, Double.parseDouble(content));
        } catch (Exception e) {
            throw new ParseException(location, "Invalid double: %s", content);
        }
    }

    private static IntNode createIntNode(Location location, String content) {

        try {
            return new IntNode(location, Integer.parseInt(content));
        } catch (Exception e) {
            throw new ParseException(location, "Invalid integer: %s", content);
        }
    }

    
    private static int precedenceOf(Node node) {
        switch (node.getType()) {
            case ADD:   return 1;
            case SUB:   return 1;
            case MUL:   return 2;
            case DIV:   return 2;
            case POWER: return 3;
            default:    return 0;
        }
    }

    public static boolean isHighPrecedence(Node a, Node b) {
        return precedenceOf(a) >= precedenceOf(b);
    }

    public static boolean isOperator(Node node) {
        return precedenceOf(node) > 0;
    }

    public static boolean isFunction(Node node) {
        return isFunction(node.getType());
    }

    public static boolean isFunction(NodeType type) {
        return type == NodeType.MAP || type == NodeType.REDUCE;
    }    
}
