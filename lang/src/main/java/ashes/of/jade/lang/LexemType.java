package ashes.of.jade.lang;

enum LexemType {
    Whitespace(1),
    Var(3),
    Print(5),
    Out(3),

    CallMap(3), CallReduce(6),

    Comma(1),
    Arrow(2),

    Plus(1), Minus(1), Multiply(1), Divide(1), Caret(1),

    Assign(1),
    QuotationMark(1),

    Number(0), FloatPointNumber(0),
    Identifier(0),

    LeftBrace(1),
    RightBrace(1),
    LeftParenthesis(1),
    RightParenthesis(1);


    public int len;

    LexemType(int len) {
        this.len = len;
    }
}
