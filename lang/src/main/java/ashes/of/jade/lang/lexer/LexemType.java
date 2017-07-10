package ashes.of.jade.lang.lexer;


public enum LexemType {

    /**
     * Call map method
     */
    MAP,

    /**
     * Call reduce method
     */
    REDUCE,

    /**
     * Print string to output stream
     */
    PRINT,

    /**
     * Print integer, double or sequence to output stream
     */
    OUT,


    /**
     * var keyword
     */
    VAR,

    /**
     * Comma
     */
    COMMA,

    /**
     * -> keyword
     */
    ARROW,

    /**
     * Load a value onto stack from local variable
     */
    LOAD,

    /**
     * Store a value to a local variable
     */
    STORE,

    /**
     * Data types
     */
    INTEGER, DOUBLE, STRING,

    /**
     * Keywords
     */
    CURLY_OPEN,
    CURLY_CLOSE,
    PARENT_OPEN,
    PARENT_CLOSE,

    /**
     * Operators
     */
    PLUS, MINUS, MULTIPLY, DIVIDE, REMAINDER, POWER,

    /**
     * New line and EOF
     */
    NL, EOF
}
