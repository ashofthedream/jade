package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;
import ashes.of.jade.lang.SourceCode;

import java.util.*;
import java.util.regex.Pattern;


public class Lexer {


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

        while (!it.isEOF()) {
            if (it.isNewLine()) {
                Location loc = it.getLocation();
                lexems.add(new Lexem(LexemType.NewLine, loc));
                it.step();
                it.newLine();
                continue;
            }


            if (it.isWhitespace()) {
                it.step();
                continue;
            }


            if (it.isLetter()) {
                Location loc = it.getLocation();
                int letters = 0;
                while (!it.isEOF() && it.isLetter()) {
                    it.step();
                    letters++;
                }

                String token = it.getString(it.getIndex() - letters, letters);
                switch (token) {
                    case "map":
                        lexems.add(new Lexem(LexemType.Map, loc));
                        break;

                    case "reduce":
                        lexems.add(new Lexem(LexemType.Reduce, loc));
                        break;

                    case "var":
                        lexems.add(new Lexem(LexemType.Var, loc));
                        break;

                    case "print":
                        lexems.add(new Lexem(LexemType.Print, loc));
                        break;

                    case "out":
                        lexems.add(new Lexem(LexemType.Out, loc));
                        break;

                    default:
                        lexems.add(new Lexem(LexemType.Load, loc, token));
                }

                continue;
            }

            if (it.getChar() == '"') {
                Location loc = it.getLocation();
                int len = 0;
                it.step(1);
                while (!it.isEOF() && it.getChar() != '"' && it.getChar() != '\n') {
                    len++;
                    it.step(1);
                }

                String content = it.getString(it.getIndex() - len, len);
                it.step(1);
                lexems.add(new Lexem(LexemType.String, loc, content));
                continue;
            }

            if (it.isDigit()) {
                Location loc = it.getLocation();
                int start = it.getIndex();
                int len = 0;

                Lexem back1 = lexems.get(lexems.size() - 1);
                if (back1.is(LexemType.Minus) || back1.is(LexemType.Plus)) {
                    Lexem back2 = lexems.get(lexems.size() - 2);
                    if (back2.is(LexemType.Store) ||
                        back2.is(LexemType.Arrow) ||
                        back2.is(LexemType.ParentOpen) ||
                        back2.is(LexemType.Plus) ||
                        back2.is(LexemType.Minus) ||
                        back2.is(LexemType.Multiply) ||
                        back2.is(LexemType.Divide) ||
                        back2.is(LexemType.Power)) {
                        loc = back1.getLocation();
                        start--;
                        len++;
                        lexems.remove(lexems.get(lexems.size() - 1));
                    }

                }

                while (!it.isEOF() && (it.isDigit() || it.isDot())) {
                    it.step(1);
                    len++;
                }

                String token = it.getString(start, len);

                Pattern doublePattern = Pattern.compile("[+|-]?[0-9]+\\.[0-9]+");
                Pattern integerPattern = Pattern.compile("[+|-]?[0-9]+");

                LexemType type = null;
                if (doublePattern.matcher(token).matches()) {
                    type = LexemType.DoubleNumber;
                } else if (integerPattern.matcher(token).matches()) {
                    type = LexemType.IntegerNumber;
                }

                if (type == null) {
                    throw new ParseException("Unknown token: " + token, token, loc);
                }

                lexems.add(new Lexem(type, loc, token));
                continue;
            }


            if (it.getChar() == '>') {
                Lexem minus = removeLast(lexems);
                if (minus.is(LexemType.Minus)) {
                    lexems.add(new Lexem(LexemType.Arrow, minus.getLocation()));
                    it.step();
                    continue;
                }

                throw new ParseException("Expected -> but first char is ", minus.toString(), minus.getLocation());
            }


            if (it.isOperator()) {
                Location loc = it.getLocation();
                switch (it.getChar()) {
                    case '+': lexems.add(new Lexem(LexemType.Plus, loc)); break;
                    case '-': lexems.add(new Lexem(LexemType.Minus, loc)); break;
                    case '/': lexems.add(new Lexem(LexemType.Divide, loc)); break;
                    case '*': lexems.add(new Lexem(LexemType.Multiply, loc)); break;
                }
            }

            if (it.isParentOpen()) {
                Location loc = it.getLocation();
                lexems.add(new Lexem(LexemType.ParentOpen, loc));
            }

            if (it.isParentClose()) {
                Location loc = it.getLocation();
                lexems.add(new Lexem(LexemType.ParentClose, loc));
            }

            if (it.isCurlyOpen()) {
                Location loc = it.getLocation();
                lexems.add(new Lexem(LexemType.CurlyOpen, loc));
            }

            if (it.isCurlyClose()) {
                Location loc = it.getLocation();
                lexems.add(new Lexem(LexemType.CurlyClose, loc));
            }

            if (it.isComma()) {
                Location loc = it.getLocation();
                lexems.add(new Lexem(LexemType.Comma, loc));
            }


            if (it.isAssign()) {
                Lexem id = removeLast(lexems);
                if (!id.is(LexemType.Load))
                    throw new ParseException("Load expected", it);

                Lexem var = removeLast(lexems);
                if (!var.is(LexemType.Var))
                    throw new ParseException("Load expected", it);

                lexems.add(new Lexem(LexemType.Store, var.getLocation(), id.getContent()));
            }

            it.step();
        }


        lexems.add(new Lexem(LexemType.EOF, it.getLocation()));

        System.out.println();
        for (Lexem lexem : lexems) {
            System.out.print(lexem + " ");
            if (lexem.is(LexemType.NewLine) || lexem.is(LexemType.EOF))
                System.out.println();

        }

        System.out.println();


        return lexems;
    }

    private Lexem removeLast(List<Lexem> lexems) {
        return lexems.remove(lexems.size() - 1);
    }
}
