package ashes.of.jade.lang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class Lexer {

    private final Map<Pattern, LexemType> keywords = new LinkedHashMap<>();

    {
        keywords.put(Pattern.compile("\\s"), LexemType.Whitespace);

        keywords.put(Pattern.compile("var"), LexemType.Var);
        keywords.put(Pattern.compile("print"), LexemType.Print);
        keywords.put(Pattern.compile("out"), LexemType.Out);
        keywords.put(Pattern.compile("map"), LexemType.CallMap);
        keywords.put(Pattern.compile("reduce"), LexemType.CallReduce);


        keywords.put(Pattern.compile("->"), LexemType.Arrow);

        keywords.put(Pattern.compile("\\{"), LexemType.LeftBrace);
        keywords.put(Pattern.compile("\\}"), LexemType.RightBrace);
        keywords.put(Pattern.compile("\\("), LexemType.LeftParenthesis);
        keywords.put(Pattern.compile("\\)"), LexemType.RightParenthesis);
        keywords.put(Pattern.compile("\\+"), LexemType.Plus);
        keywords.put(Pattern.compile("\\-"), LexemType.Minus);
        keywords.put(Pattern.compile("\\*"), LexemType.Multiply);
        keywords.put(Pattern.compile("\\/"), LexemType.Divide);
        keywords.put(Pattern.compile("\\^"), LexemType.Caret);

        keywords.put(Pattern.compile("\\="), LexemType.Assign);
        keywords.put(Pattern.compile(","), LexemType.Comma);
        keywords.put(Pattern.compile("\""), LexemType.QuotationMark);


        keywords.put(Pattern.compile("[0-9]\\.[0-9]+"), LexemType.FloatPointNumber);
        keywords.put(Pattern.compile("[0-9]+"), LexemType.Number);
        keywords.put(Pattern.compile("[A-Za-z][A-Za-z0-9_]*"), LexemType.Identifier);
    }



    public List<Lexem> parse(String source) {
        return parse(new SourceCode(source));
    }

    /**
     *
     * expr ::= expr op expr | (expr) | identifier | { expr, expr } | number | map(expr, identifier -> expr) | reduce(expr, expr, identifier identifier -> expr)
     * op ::= + | - | * | / | ^
     * stmt ::= var identifier = expr | out expr | print "string"
     * program ::= stmt | program stmt
     *
     *
     *
     * var n = 500
     * var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))
     * var pi = 4 * reduce (sequence, 0, x y -> x + y)
     * print "pi = "
     * out pi
     */
    public List<Lexem> parse(SourceCode it) {
        System.out.println(it);
        List<Lexem> lexems = new ArrayList<>();

        while (!it.isEOF()) {
            Lexem lexem = tryParseNextLexem(it);
            if (lexem == null)
                throw new ParseException("Can't parse symbol", it);

        }


        lexems.forEach(System.out::println);
        return lexems;
    }

    private Lexem tryParseNextLexem(SourceCode it) {
        for (Map.Entry<Pattern, LexemType> e : keywords.entrySet()) {
            Pattern pattern = e.getKey();
            LexemType type = e.getValue();

            if (type.len > 0 && it.isEnough(type.len)) {
                String content = it.getString(type.len);
                if (pattern.matcher(content).matches()) {
                    it.step(type.len);

                    if (type != LexemType.Whitespace)
                        return new Lexem(type, content);
                }
            } else {
                String matched = "";
                int len = 0;
                while (++len < it.remains()) {
                    String content = it.getString(len);
                    if (!pattern.matcher(content).matches())
                        break;

                    matched = content;
                }

                if (!matched.isEmpty()) {
                    it.step(matched.length());
                    return new Lexem(type, matched);
                }
            }
        }

        return null;
    }
}
