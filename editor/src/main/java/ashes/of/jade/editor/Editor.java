package ashes.of.jade.editor;

import ashes.of.jade.editor.frames.EditorFrame;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.interpreter.Settings;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.parser.Parser;
import com.google.inject.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Named;
import javax.swing.*;
import java.util.concurrent.ForkJoinPool;


@Singleton
public class Editor {
    private static final Logger log = LogManager.getLogger(Editor.class);


    public static class EditorModule extends AbstractModule {
        @Override
        protected void configure() {

        }

        @Provides
        @Singleton
        @Named("editor-pool")
        public ForkJoinPool editorPool() {
            return ForkJoinPool.commonPool();
        }

        @Provides
        @Singleton
        @Named("interpreter-pool")
        public ForkJoinPool interpreterPool() {
            return ForkJoinPool.commonPool();
        }

        @Provides
        @Singleton
        public Lexer lexer() {
            return new Lexer();
        }

        @Provides
        @Singleton
        public Interpreter interpreter(Lexer lexer, @Named("interpreter-pool") ForkJoinPool pool) {
            Settings settings = new Settings();
            Interpreter interpreter = new Interpreter(pool, settings, lexer, new Parser());
            return interpreter;
        }
    }


    private final EditorFrame editorFrame;


    @Inject
    public Editor(EditorFrame editorFrame) {
        this.editorFrame = editorFrame;
    }

    /**
     * Start editor
     */
    public void start() {
        editorFrame.setVisible(true);
    }



    public static void main(String... args) {
        Injector injector = Guice.createInjector(new EditorModule());
        Editor editor = injector.getInstance(Editor.class);
        SwingUtilities.invokeLater(editor::start);
    }
}