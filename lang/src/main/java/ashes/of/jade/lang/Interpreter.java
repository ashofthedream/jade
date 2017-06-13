package ashes.of.jade.lang;

import java.util.*;



public class Interpreter {


    public static void main(String... args) {
        String program =
                "var n = 500\n" +
                "var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))\n" +
                "var pi = 4 * reduce (sequence, 0, x y -> x + y)\n" +
                "print \"pi = \"\n" +
                "out pi\n";

        try {
            Lexer lexer = new Lexer();
            List<Lexem> lexems = lexer.parse(program);

            System.out.println(lexems);

        } catch (ParseException e) {

            StringBuilder b = new StringBuilder()
                    .append(e.getCode())
                    .append("\n");

            for (int i = 0; i < e.getPosition(); i++) {
                b.append(" ");
            }

            b.append("^ ").append(e.getMessage());

            System.err.println(b);
        }

    }
}



