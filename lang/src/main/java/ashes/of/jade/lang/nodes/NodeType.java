package ashes.of.jade.lang.nodes;

public enum NodeType {


    COMMA,

    CurlyOpen,
    CurlyClose,
    ParentOpen,
    ParentClose,


    INTEGER,
    INTEGERSEQ,
    DOUBLE,
    DOUBLESEQ,
    STRING,

    SEQ,

    MAP,
    REDUCE,
    PRINT,
    OUT,

    NL, EOF,


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

    POWER
}
