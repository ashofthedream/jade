package ashes.of.jade.editor.frames;

import ashes.of.jade.editor.Listeners;
import ashes.of.jade.editor.VariablesTableModel;
import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.interpreter.Scope;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.LexemType;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.StringNode;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main frame for editor
 */
@Singleton
public class EditorFrame extends JFrame {
    private static final Logger log = LogManager.getLogger(EditorFrame.class);

    private final ForkJoinPool pool;
    private final Lexer lexer;
    private final Interpreter interpreter;
    private final SettingsFrame settings;

    /*
     * menu
     */
    private final JMenuBar menuBar = new JMenuBar();
    private final JMenu fileMenu = new JMenu("File");
    private final JMenuItem openMenuItem = new JMenuItem("Open...");
    private final JMenuItem exitMenuItem = new JMenuItem("Exit");


    private final JMenu settingsMenu = new JMenu("Settings");
    private final JMenuItem interpreterSettingsMenuItem = new JMenuItem("Interpreter Settings...");

    /**
     * Main text area
     */
    private final JTextArea sourceCodeTextArea = new JTextArea(20, 40);

    /**
     * Debug info text area (read only)
     */
    private final JTextArea debugTextArea = new JTextArea(5, 40);



    private final VariablesTableModel model = new VariablesTableModel();

    /**
     * Right variables table
     */
    private final JTable variablesTable = new JTable(model);

    /**
     * Run button
     */
    private final JButton runButton = new JButton("run");

    private final RunnerState runnerState = new RunnerState();


    private final Map<LexemType, DefaultHighlighter.DefaultHighlightPainter> highlighters = new HashMap<>();

    private final DefaultHighlighter.DefaultHighlightPainter valPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);
    private final DefaultHighlighter.DefaultHighlightPainter varPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);
    private final DefaultHighlighter.DefaultHighlightPainter functionPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.MAGENTA);
    private final DefaultHighlighter.DefaultHighlightPainter errorPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.RED);


    {
        String code =
                "var n = 500\n" +
                "var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))\n" +
                "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
                "print \"pi = \"\n" +
                "out pi" ;

        sourceCodeTextArea.setText(code);


        highlighters.put(LexemType.STRING, valPainter);
        highlighters.put(LexemType.INTEGER, valPainter);
        highlighters.put(LexemType.DOUBLE, valPainter);
        highlighters.put(LexemType.IDENTIFIER, valPainter);

        highlighters.put(LexemType.VAR, varPainter);

        highlighters.put(LexemType.MAP, functionPainter);
        highlighters.put(LexemType.REDUCE, functionPainter);
        highlighters.put(LexemType.OUT, functionPainter);
        highlighters.put(LexemType.PRINT, functionPainter);
    }

    @Inject
    public EditorFrame(@Named("editor-pool") ForkJoinPool pool, Lexer lexer, Interpreter interpreter, SettingsFrame settings) {
        this.pool = pool;
        this.lexer = lexer;
        this.interpreter = interpreter;
        this.settings = settings;

        setTitle("Jade Editor");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Container container = getContentPane();


        exitMenuItem.addActionListener(this::exitEditorAction);
        openMenuItem.addActionListener(this::openFileAction);
        fileMenu.add(openMenuItem);
        fileMenu.add(exitMenuItem);


        interpreterSettingsMenuItem.addActionListener(this::showSettingsFrameAction);
        settingsMenu.add(interpreterSettingsMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);


        Font font = new Font("Monospaced", Font.PLAIN, 13);
        // main text area
        sourceCodeTextArea.setFont(font);
        sourceCodeTextArea.addKeyListener(Listeners.onKeyReleased(this::textAreaKeyReleasedAction));

        // bottom text area
        debugTextArea.setBorder(new LineBorder(Color.black));
        debugTextArea.setSize(100, 0);
        debugTextArea.setEnabled(false);
        debugTextArea.setFont(font);

        variablesTable.setSize(120, 300);

        runButton.addActionListener(this::evalAction);

        container.add(variablesTable, BorderLayout.LINE_END);
        container.add(debugTextArea, BorderLayout.PAGE_END);
        container.add(sourceCodeTextArea, BorderLayout.CENTER);
        container.add(runButton, BorderLayout.PAGE_START);

        setJMenuBar(menuBar);
        setSize(640, 480);
        setVisible(true);


        Timer timer = new Timer(500, this::backgroundTimerAction);
        timer.start();
    }



    /*
     * Action handlers
     */

    private void textAreaKeyReleasedAction(KeyEvent event) {
        runnerState.updateTime();

        if (Character.isWhitespace(event.getKeyChar()))
            highlightCode();
    }

    private void highlightCode() {
        try {
            List<Lexem> lexems = lexer.parse(sourceCodeTextArea.getText());
            sourceCodeTextArea.getHighlighter().removeAllHighlights();
            lexems.forEach(lexem -> {
                Highlighter highlighter = sourceCodeTextArea.getHighlighter();
                Location location = lexem.getLocation();
                try {
                    DefaultHighlighter.DefaultHighlightPainter painter = highlighters.get(lexem.getType());
                    if (painter == null)
                        return;

                    highlighter.addHighlight(location.getStart(), location.getEnd(), painter);
                } catch (BadLocationException ex) {
                    log.error("Can't highlight", ex);
                }
            });

        } catch (ParseException e) {
            log.warn("Can't parse", e);
        }
    }


    /**
     * Timer event
     *
     * @param event action event
     */
    private void backgroundTimerAction(ActionEvent event) {
        if (!runnerState.canRunInBackground()) {
            log.trace("Already running or nothing changed, await");
            return;
        }

        runnerState.updateLastEvaluatedTime();
        evalAction(event);
    }

    /**
     * File for open selected
     *
     * @param event action event
     */
    private void openFileAction(ActionEvent event) {
        log.debug("openFileAction");
        JFileChooser fileChooser = new JFileChooser();
        int state = fileChooser.showOpenDialog(this);

        if (state != JFileChooser.APPROVE_OPTION) {
            log.info("showOpenDialog returns {}", state);
            return;
        }


        try {
            File file = fileChooser.getSelectedFile();
            Path path = Paths.get(file.getAbsolutePath());

            String read = new String(Files.readAllBytes(path));
            sourceCodeTextArea.setText(read);
            runnerState.updateTime();
        } catch (Exception e) {
            log.error("Can't read file");
        }
    }

    /**
     * Exit button
     *
     * @param event action event
     */
    private void exitEditorAction(ActionEvent event) {
        log.debug("exitEditorAction");
        System.exit(0);
    }

    /**
     * Show settings frame
     *
     * @param event action event
     */
    private void showSettingsFrameAction(ActionEvent event) {
        log.debug("showSettingsFrameAction");
        settings.setVisible(true);
    }


    /**
     * Run interpreter
     *
     * @param event action event
     */
    private void evalAction(ActionEvent event) {
        log.debug("evalAction invoked");

        String sourceCode = sourceCodeTextArea.getText();
        pool.submit(() -> eval(sourceCode));
    }


    private void eval(String sourceCode) {
        try {
            runnerState.setRunNow(true);
            long start = System.currentTimeMillis();

            highlightCode();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(baos);
            interpreter.getSettings().setOut(stream);

            Scope state = interpreter.eval(sourceCode);
            String output = baos.toString(Charset.defaultCharset().name());

            SwingUtilities.invokeLater(() -> {
                model.clear();
                state.getVars().forEach(model::add);
                model.add("_time", new StringNode(String.format("%.3f", (System.currentTimeMillis() - start) / 1000.0 )));
                model.fireTableStructureChanged();

                debugTextArea.setText(output);
            });

        } catch (ParseException ex) {
            log.warn("Can't parse", ex);
            Location location = ex.getLocation();
            String errorMessage = buildErrorMessage(sourceCode, ex);

            SwingUtilities.invokeLater(() -> {
                Highlighter highlighter = sourceCodeTextArea.getHighlighter();

                try {
                    Highlighter.Highlight[] highlights = highlighter.getHighlights();
                    for (Highlighter.Highlight highlight : highlights) {
                        if (highlight.getStartOffset() == location.getStart())
                            highlighter.removeHighlight(highlight);
                    }

                    highlighter.addHighlight(location.getStart(), location.getEnd(), errorPainter);
                } catch (BadLocationException e) {
                    log.error("Can't highlight", ex);
                }

                debugTextArea.setText(errorMessage);
            });

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> debugTextArea.setText("Unknown error: " + ex.getMessage()));
            log.error("Very bad thing happened", ex);
        }
        finally {
            runButton.setEnabled(true);
            runnerState.setRunNow(false);
        }
    }


    private String buildErrorMessage(String sourceCode, ParseException ex) {
        Location location = ex.getLocation();
        int startLine = location.getStart();
        for (; startLine > 0; startLine--) {
            if (sourceCode.charAt(startLine) == '\n')
                break;
        }

        int endLine = location.getStart();
        for (; endLine < sourceCode.length(); endLine++) {
            if (sourceCode.charAt(endLine) == '\n')
                break;
        }

        StringBuilder b = new StringBuilder()
                .append(sourceCode.substring(startLine, endLine))
                .append("\n");

        for (int i = 0; i < location.getOffset() - 1; i++)
            b.append(" ");

        b.append("^ ").append(ex.getMessage());
        return b.toString();
    }
}
