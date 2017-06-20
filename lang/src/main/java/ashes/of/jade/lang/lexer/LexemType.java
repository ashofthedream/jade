package ashes.of.jade.lang.lexer;


public enum LexemType {
    NewLine,
    Var,

    Print,
    Out,

    Map, Reduce,

    Comma,
    Arrow,

    Plus, Minus, Multiply, Divide, Remainder, Power,

    Store,
    Load,

    IntegerNumber, DoubleNumber,
    String,

    CurlyOpen,
    CurlyClose,
    ParentOpen,
    ParentClose,

    EOF
}
