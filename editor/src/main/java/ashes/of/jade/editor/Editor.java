package ashes.of.jade.editor;

import ashes.of.jade.lang.interpreter.Interpreter;


public class Editor {


    private final Interpreter interpreter = new Interpreter();


    private void start() {

    }




    public static void main(String... args) {

        Editor editor = new Editor();
        editor.start();

    }
}
