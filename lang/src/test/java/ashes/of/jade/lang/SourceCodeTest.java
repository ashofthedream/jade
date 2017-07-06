package ashes.of.jade.lang;

import org.junit.Test;

import static org.junit.Assert.*;


public class SourceCodeTest {

    @Test
    public void getCharShouldReturnCurrentChar() throws Exception {
        SourceCode code = new SourceCode("hello world");

        assertEquals('h', code.getChar());
    }

    @Test
    public void getStringShouldReturnSubstringFromIndexAndWithLenght() throws Exception {
        SourceCode code = new SourceCode("hello world");

        assertEquals("hello", code.getString(5));
    }

    @Test
    public void stepShouldMoveIteratorForward() throws Exception {
        SourceCode code = new SourceCode("hello world");
        code.step();
        assertEquals('e', code.getChar());

        code.step(5);
        assertEquals('w', code.getChar());
    }

    @Test
    public void getIndexShouldReturnCurrentIndex() throws Exception {
        SourceCode code = new SourceCode("hello world");
        code.step();
        assertEquals(1, code.getIndex());

        code.step(5);
        assertEquals(6, code.getIndex());
    }

    @Test
    public void remainsShouldReturnHowMuchCharactersIsRemains() throws Exception {
        SourceCode code = new SourceCode("hello world");
        code.step(3);
        assertEquals(8, code.remains());
    }



    @Test
    public void isWhitespaceShouldReturnTrueIfCurrentSymbolIsWhitespace() throws Exception {
        SourceCode code = new SourceCode(" \t\n");

        while (!code.isEOF()) {
            assertTrue(code.isWhitespace());
            code.step();
        }
    }

    @Test
    public void isDigitShouldReturnTrueIfCurrentSymbolIsWhitespace() throws Exception {
        SourceCode code = new SourceCode("0123456789");

        while (!code.isEOF()) {
            assertTrue(code.isDigit());
            code.step();
        }
    }


    @Test
    public void getLineToIndexShouldReturnPartOfCurrentLineFromStartToIndex() throws Exception {
        SourceCode code = new SourceCode("var a = 10\nvar b = a + 0");

        code.step(9);
        assertEquals("var a = 1", code.getLineToIndex());
    }


    @Test
    public void getLineToEndShouldReturnPartOfCurrentLineFromIndexToEnd() throws Exception {
        SourceCode code = new SourceCode("var a = 10\nvar b = a + 0");

        code.step(2);
        assertEquals("r a = 10", code.getLineToEnd());
    }
}