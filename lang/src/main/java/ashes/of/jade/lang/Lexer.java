package ashes.of.jade.lang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class Lexer {

    private final Map<LexemType, Pattern> keywords = new LinkedHashMap<>();

    {
        keywords.put(LexemType.NewLine, Pattern.compile("\\n"));
        keywords.put(LexemType.Whitespace, Pattern.compile("\\s"));

        keywords.put(LexemType.Var,                 Pattern.compile("var"));
        keywords.put(LexemType.Print,               Pattern.compile("print"));
        keywords.put(LexemType.Out,                 Pattern.compile("out"));
        keywords.put(LexemType.Map,                 Pattern.compile("map"));
        keywords.put(LexemType.Reduce,              Pattern.compile("reduce"));

        keywords.put(LexemType.Arrow,               Pattern.compile("->"));

        keywords.put(LexemType.CurlyOpen,           Pattern.compile("\\{"));
        keywords.put(LexemType.CurlyClose,          Pattern.compile("\\}"));
        keywords.put(LexemType.ParentOpen,          Pattern.compile("\\("));
        keywords.put(LexemType.ParentClose,         Pattern.compile("\\)"));
        keywords.put(LexemType.Plus,                Pattern.compile("\\+"));
        keywords.put(LexemType.Minus,               Pattern.compile("\\-"));
        keywords.put(LexemType.Multiply,            Pattern.compile("\\*"));
        keywords.put(LexemType.Divide,              Pattern.compile("\\/"));
        keywords.put(LexemType.Power,               Pattern.compile("\\^"));

        keywords.put(LexemType.Assign,              Pattern.compile("\\="));
        keywords.put(LexemType.Comma,               Pattern.compile(","));

        keywords.put(LexemType.DoubleNumber,        Pattern.compile("[0-9]+\\.[0-9]*"));
        keywords.put(LexemType.IntegerNumber,       Pattern.compile("[0-9]+"));
        keywords.put(LexemType.Identifier,          Pattern.compile("[A-Za-z][A-Za-z0-9_]*"));
        keywords.put(LexemType.String,              Pattern.compile("\".*\""));
    }


    public List<Lexem> parse(String source) {
        return parse(new SourceCode(source));
    }

    /**
     * expr ::= expr op expr | (expr) | identifier | { expr, expr } | number | map(expr, identifier -> expr) | reduce(expr, expr, identifier identifier -> expr)
     * op ::= + | - | * | / | ^
     * stmt ::= var identifier = expr | out expr | print "string"
     * program ::= stmt | program stmt
     * <p>
     * <p>
     * <p>
     * var n = 500
     * var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))
     * var pi = 4 * reduce (sequence, 0, x y -> x + y)
     * print "pi = "
     * out pi
     */
    public List<Lexem> parse(SourceCode it) {
        List<Lexem> lexems = new ArrayList<>();

        while (it.isEnough(1)) {
            Lexem lexem = tryParseNextLexem(it);
            if (lexem == null)
                throw new ParseException("Can't parse symbol", it);

            lexems.add(lexem);
        }

        lexems.add(new Lexem(LexemType.EOF, it.getLocation()));
        return lexems;
    }

    private Lexem tryParseNextLexem(SourceCode it) {
        for (Map.Entry<LexemType, Pattern> e : keywords.entrySet()) {
            LexemType type = e.getKey();
            Pattern pattern = e.getValue();

            if (type.len > 0 && it.isEnough(type.len)) {
                String content = it.getString(type.len);
                if (pattern.matcher(content).matches()) {
                    it.step(type.len);

                    if (type != LexemType.Whitespace)
                        return new Lexem(type, it.getLocation(), "");
                }
            } else {
//                Matcher matcher = pattern.matcher(it.getSource())
//                        .region(it.getIndex(), it.getSource().length());
//
//                if (!matcher.find() || matcher.start() != it.getIndex())
//                    continue;
//
//                String found = it.getString(matcher.start(), matcher.end());
//                Location start = it.getLocation();
//                it.step(found.length());
//                return new Lexem(type, start, found);

                char ch = it.getChar();

                if (Character.isDigit(ch)) {
                    int start = it.getIndex();
                    while (!it.isEOF() && Character.isDigit(it.getChar()) || it.getChar() == '.')
                        it.step(1);

                    String content = it.getString(start, it.getIndex() - start);

                    Pattern number = keywords.get(LexemType.IntegerNumber);
                    if (number.matcher(content).matches())
                        return new Lexem(LexemType.IntegerNumber, it.getLocation(), content);

                    Pattern fp = keywords.get(LexemType.DoubleNumber);
                    if (fp.matcher(content).matches())
                        return new Lexem(LexemType.DoubleNumber, it.getLocation(), content);

                    return null;
                }


                if (Character.isLetter(ch)) {
                    int start = it.getIndex();
                    while (!it.isEOF() && Character.isLetter(it.getChar()) || Character.isDigit(it.getChar()) || it.getChar() == '_') {
                        it.step(1);
                    }

                    String content = it.getString(start, it.getIndex() - start);

                    Pattern id = keywords.get(LexemType.Identifier);
                    if (id.matcher(content).matches())
                        return new Lexem(LexemType.Identifier, it.getLocation(), content);

                    return null;
                }


                if (ch == '"') {
                    it.step(1);
                    int start = it.getIndex();

                    while (!it.isEOF() && it.getChar() != '"' && it.getChar() != '\n') {
                        it.step(1);
                    }

                    String content = it.getString(start, it.getIndex() - start);
                    it.step(1);
                    return new Lexem(LexemType.String, it.getLocation(), content);
                }
            }
        }

        return null;
    }
}
