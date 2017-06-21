package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;
import ashes.of.jade.lang.SourceCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;


public class Lexer {
    private static final Logger log = LogManager.getLogger(Lexer.class);

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
        log.debug("input:\n{}", it.getSource());

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

            log.trace("state: {} \u2192{}", it.getLineToIndex(), it.getLineToEnd());

            if (it.isLetter()) {
                log.debug("Found letter: {}, try parse as a Identifier, Call", it.getChar());
                Location loc = it.getLocation();
                int letters = 0;
                while (!it.isEOF() && it.isLetter()) {
                    it.step();
                    letters++;
                }

                String token = it.getString(it.getIndex() - letters, letters);
                log.debug("Found letters: '{}' at ", token, loc);
                switch (token) {
                    case "map":
                        Lexem map = new Lexem(LexemType.Map, loc);
                        add(lexems, map);
                        break;

                    case "reduce":
                        Lexem reduce = new Lexem(LexemType.Reduce, loc);
                        add(lexems, reduce);
                        break;

                    case "var":
                        Lexem var = new Lexem(LexemType.Var, loc);
                        add(lexems, var);
                        break;

                    case "print":
                        Lexem print = new Lexem(LexemType.Print, loc);
                        add(lexems, print);
                        break;

                    case "out":
                        Lexem out = new Lexem(LexemType.Out, loc);
                        add(lexems, out);
                        break;

                    default:
                        if (isNewLine(lexems))
                            throw new ParseException("Identifier isn't allowed as first token", it.getLineToEnd(), loc);

                        Lexem load = new Lexem(LexemType.Load, loc, token);
                        add(lexems, load);
                }

                continue;
            }

            if (it.getChar() == '"') {
                log.debug("Found letter: \", try parse as a String", it.getChar());
                checkNewLine(it, lexems, "String isn't allowed as first token");

                Location loc = it.getLocation();
                int len = 0;
                it.step(1);
                while (!it.isEOF() && it.getChar() != '"' && it.getChar() != '\n') {
                    len++;
                    it.step(1);
                }

                String token = it.getString(it.getIndex() - len, len);
                it.step(1);
                Lexem string = new Lexem(LexemType.String, loc, token);
                add(lexems, string);
                continue;
            }

            if (it.isDigit()) {
                log.debug("Digit found: {}, try parse as a Integer/Double", it.getChar());
                checkNewLine(it, lexems, "Number isn't allowed as first token");

                Location loc = it.getLocation();
                int start = it.getIndex();
                int len = 0;

                Lexem back1 = lexems.get(lexems.size() - 1);
                if (back1.is(LexemType.Minus) || back1.is(LexemType.Plus)) {
                    log.trace("previous lexem is +/-, check that it isn't expr");
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

                Lexem number = new Lexem(type, loc, token);
                add(lexems, number);
                continue;
            }


            if (it.getChar() == '>') {
                log.debug("Found symbol: {}, try parse as a Arrow", it.getChar());

                Lexem minus = removeLast(lexems);
                if (minus.is(LexemType.Minus)) {
                    add(lexems, new Lexem(LexemType.Arrow, minus.getLocation()));
                    it.step();
                    continue;
                }

                throw new ParseException("Expected -> but first char is ", minus.toString(), minus.getLocation());
            }


            if (it.isOperator()) {
                checkNewLine(it, lexems, "Operator isn't allowed as first token");

                Location loc = it.getLocation();
                switch (it.getChar()) {
                    case '+': lexems.add(new Lexem(LexemType.Plus, loc)); break;
                    case '-': lexems.add(new Lexem(LexemType.Minus, loc)); break;
                    case '/': lexems.add(new Lexem(LexemType.Divide, loc)); break;
                    case '*': lexems.add(new Lexem(LexemType.Multiply, loc)); break;
                }

                log.info("add Operator {} at {}", it.getChar(), loc);
                it.step();
                continue;
            }

            if (it.isParentOpen()) {
                checkNewLine(it, lexems, "Symbol ( isn't allowed as first token");

                Location loc = it.getLocation();
                add(lexems, new Lexem(LexemType.ParentOpen, loc));
                it.step();
                continue;
            }

            if (it.isParentClose()) {
                checkNewLine(it, lexems, "Symbol ) isn't allowed as first token");

                Location loc = it.getLocation();
                add(lexems, new Lexem(LexemType.ParentClose, loc));
                it.step();
                continue;
            }

            if (it.isCurlyOpen()) {
                checkNewLine(it, lexems, "Symbol { isn't allowed as first token");

                Location loc = it.getLocation();
                add(lexems, new Lexem(LexemType.CurlyOpen, loc));
                it.step();
                continue;
            }

            if (it.isCurlyClose()) {
                checkNewLine(it, lexems, "Symbol } isn't allowed as first token");

                Location loc = it.getLocation();
                add(lexems, new Lexem(LexemType.CurlyClose, loc));
                it.step();
                continue;
            }

            if (it.isComma()) {
                checkNewLine(it, lexems, "Comma isn't allowed as first token");

                Location loc = it.getLocation();
                Lexem lexem = new Lexem(LexemType.Comma, loc);
                add(lexems, lexem);
                it.step();
                continue;
            }


            if (it.isAssign()) {
                log.debug("assign, try reduce [var, load{id}, Assign] to one Store{id}");
                checkNewLine(it, lexems, "Assign isn't allowed as first token");

                Lexem id = removeLast(lexems);
                if (!id.is(LexemType.Load))
                    throw new ParseException("Load expected", it);

                Lexem var = removeLast(lexems);
                if (!var.is(LexemType.Var))
                    throw new ParseException("Load expected", it);

                Lexem lexem = new Lexem(LexemType.Store, var.getLocation(), id.getContent());
                lexems.add(lexem);
                log.info("add {},   reduce [{}, {}]", lexem, var, id);
                it.step();
                continue;
            }

            throw new ParseException("Unexpected symbol '" + it.getChar() + "'", it);
        }


        add(lexems, new Lexem(LexemType.EOF, it.getLocation()));


        System.out.println();
        for (Lexem lexem : lexems) {
            System.out.print(lexem + " ");
            if (lexem.is(LexemType.NewLine) || lexem.is(LexemType.EOF))
                System.out.println();

        }

        System.out.println();


        return lexems;
    }

    private void add(List<Lexem> lexems, Lexem eof) {
        log.info("add {}", eof);
        lexems.add(eof);
    }

    private void checkNewLine(SourceCode it, List<Lexem> lexems, String message) {
        if (isNewLine(lexems))
            throw new ParseException(message, it);
    }

    private boolean isNewLine(List<Lexem> lexems) {
        return lexems.isEmpty() || getLast(lexems).is(LexemType.NewLine);
    }

    private Lexem removeLast(List<Lexem> lexems) {
        return lexems.remove(lexems.size() - 1);
    }

    private Lexem getLast(List<Lexem> lexems) {
        return lexems.get(lexems.size() - 1);
    }
}
