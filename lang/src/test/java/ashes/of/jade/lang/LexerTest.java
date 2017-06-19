package ashes.of.jade.lang;

import org.junit.ComparisonFailure;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

public class LexerTest {


//    public <T> void assertEquals(List<T> expected, List<T> actual) {
//
//        if (equalsRegardingNull(expected, actual)) {
//            return;
//        } else {
//            failNotEquals(message, expected, actual);
//        }
//    }
    @Test
    public void testMultilineAssign() {
        String source = "var first = 1\n" +
                        "var second = 2\n" +
                        "var third = first + second\n";

        Lexer lexer = new Lexer();

        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Var, new Location(1, 1)),
                new Lexem(LexemType.Identifier, new Location(1, 5), "first"),
                new Lexem(LexemType.Assign, new Location(1, 11)),
                new Lexem(LexemType.IntegerNumber, new Location(1, 13), "1"),
                new Lexem(LexemType.NewLine, new Location(1, 14)),

                new Lexem(LexemType.Var, new Location(2, 1)),
                new Lexem(LexemType.Identifier, new Location(2, 5), "second"),
                new Lexem(LexemType.Assign, new Location(2, 12)),
                new Lexem(LexemType.IntegerNumber, new Location(2, 14), "2"),
                new Lexem(LexemType.NewLine, new Location(2, 15)),

                new Lexem(LexemType.Var, new Location(3, 1)),
                new Lexem(LexemType.Identifier, new Location(3, 5), "third"),
                new Lexem(LexemType.Assign, new Location(3, 11)),
                new Lexem(LexemType.Identifier, new Location(3, 13), "first"),
                new Lexem(LexemType.Plus, new Location(3, 19)),
                new Lexem(LexemType.Identifier, new Location(3, 21), "second"),
                new Lexem(LexemType.NewLine, new Location(2, 21)),
                new Lexem(LexemType.EOF, new Location(4, 1))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void testUnknownSymbolInInput() {
        String source = "var first = 1 & 7\n";

        Lexer lexer = new Lexer();

        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Var, new Location(1, 1)),
                new Lexem(LexemType.Identifier, new Location(1, 5), "first"),
                new Lexem(LexemType.Assign, new Location(1, 11)),
                new Lexem(LexemType.IntegerNumber, new Location(1, 13), "1"),
                new Lexem(LexemType.NewLine, new Location(1, 14)),

                new Lexem(LexemType.Var, new Location(2, 1)),
                new Lexem(LexemType.Identifier, new Location(2, 5), "second"),
                new Lexem(LexemType.Assign, new Location(2, 12)),
                new Lexem(LexemType.IntegerNumber, new Location(2, 14), "2"),
                new Lexem(LexemType.NewLine, new Location(2, 15)),

                new Lexem(LexemType.Var, new Location(3, 1)),
                new Lexem(LexemType.Identifier, new Location(3, 5), "third"),
                new Lexem(LexemType.Assign, new Location(3, 11)),
                new Lexem(LexemType.Identifier, new Location(3, 13), "first"),
                new Lexem(LexemType.Plus, new Location(3, 15)),
                new Lexem(LexemType.Identifier, new Location(3, 21), "second"),
                new Lexem(LexemType.NewLine, new Location(2, 18))
                );

        assertEquals(expected, lexems);
    }

    @Test
    public void testSimpleAssignToVarA() {
        String source = "var a = 1";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Var, new Location(1, 1)),
                new Lexem(LexemType.Identifier, new Location(1, 5), "a"),
                new Lexem(LexemType.Assign, new Location(1, 7)),
                new Lexem(LexemType.IntegerNumber, new Location(1, 9), "1"),
                new Lexem(LexemType.EOF, new Location(1, 10))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void testParseIdentifierWhichStartsFromMap() throws Exception {
        String source = "var mapped = map({0, 1}, e -> e)\n";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);


        System.out.println(lexems);

        Lexem var = lexems.get(0);
        assertEquals("First should be VAR", LexemType.Var, var.getType());

        Lexem id = lexems.get(1);
        assertEquals("ID should be second", LexemType.Identifier, id.getType());
        assertEquals("mapped", id.getContent());
    }
}