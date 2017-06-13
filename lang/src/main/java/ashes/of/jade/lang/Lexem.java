package ashes.of.jade.lang;

class Lexem {
    private final LexemType type;
    private final String content;

    public Lexem(LexemType type, String content) {
        this.type = type;
        this.content = content;
        System.out.println(this);
    }

    @Override
    public String toString() {
        return type + "{" + content + "}";
    }
}
