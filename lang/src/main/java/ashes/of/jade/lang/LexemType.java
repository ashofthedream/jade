package ashes.of.jade.lang;



public enum LexemType {
    Whitespace(1),
    NewLine(1),
    Var(3),
    Print(5),
    Out(3),

    Map(3), Reduce(6),

    Comma(1),
    Arrow(2),

    Plus(1), Minus(1), Multiply(1), Divide(1), Remainder(1), Power(1),

    Assign(1),

    IntegerNumber(0), DoubleNumber(0),
    String(0), Seq(0),

    Identifier(0),

    LOAD(0),
    STORE(0),

    CurlyOpen(1),
    CurlyClose(1),
    ParentOpen(1),
    ParentClose(1),


    Indent(1),
    Dedent(1),

    Eof(0),

    Program(0);




    public int len;

    LexemType(int len) {
        this.len = len;
    }

    public boolean isOperator() {
        return  this == Plus ||
                this == Minus ||
                this == Multiply ||
                this == Divide ||
                this == Power;
    }
}
