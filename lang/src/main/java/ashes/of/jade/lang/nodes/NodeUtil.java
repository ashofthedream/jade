package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.LexemType;
import ashes.of.jade.lang.parser.ParseException;

public class NodeUtil {

    public static NodeType nodeTypeByLexem(LexemType lexem) {
        switch (lexem) {
            case VAR:           return null;
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
            case STORE:         return NodeType.STORE;
            case LOAD:          return NodeType.LOAD;
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

        switch (type) {
            case INTEGER:   return createIntNode(lexem);
            case DOUBLE:    return createDoubleNode(lexem);
            case STRING:    return createStringNode(lexem);
            default:        return new Node(type, lexem.getLocation(), lexem.getContent());
        }
    }

    public static StringNode createStringNode(Lexem lexem) {
        return new StringNode(lexem.getLocation(), lexem.getContent());
    }

    public static DoubleNode createDoubleNode(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new DoubleNode(lexem.getLocation(), Double.parseDouble(value));
        } catch (Exception e) {
            throw new ParseException(lexem.getLocation(), "Invalid double: %s", value);
        }
    }

    public static IntNode createIntNode(Lexem lexem) {
        String value = lexem.getContent();
        try {
            return new IntNode(lexem.getLocation(), Integer.parseInt(value));
        } catch (Exception e) {
            throw new ParseException(lexem.getLocation(), "Invalid integer: %s", value);
        }
    }

    
    public static int precedenceOf(Node node) {
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
