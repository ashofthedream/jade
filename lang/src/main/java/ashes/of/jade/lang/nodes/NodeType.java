package ashes.of.jade.lang.nodes;


public enum NodeType {

    /**
     * Temporal nodes
     */
    COMMA,
    CURLY_OPEN,
    PARENT_OPEN,

    /**
     * Integer number
     */
    INTEGER,

    /**
     * Double number
     */
    DOUBLE,

    /**
     * String
     */
    STRING,

    /**
     * Sequence of integer or doubles
     */
    SEQUENCE,

    /**
     * Create a new sequence
     */
    NEWSEQUENCE,

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
     * Load a value onto stack from variable
     */
    LOAD,

    /**
     * Store a value to a local variable
     */
    STORE,

    /**
     *
     */
    LAMBDA,

    /**
     * Add two values from stack and place result to the stack
     */
    ADD,


    /**
     * Subtract two values from stack and place result to the stack
     */
    SUB,

    /**
     * Multiply two values from stack and place result to the stack
     */
    MUL,

    /**
     * Divide two values from stack and place result to the stack
     */
    DIV,

    /**
     * Raise first element from to the power of the second element from stack
     */
    POWER,

    EQUAL,

    VAR,

    /**
     * New line and EOF
     */
    NL, EOF,
}
