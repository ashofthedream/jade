package ashes.of.jade.editor.frames;

import ashes.of.jade.editor.Listeners;
import ashes.of.jade.editor.VariablesTableModel;
import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.interpreter.Scope;
import ashes.of.jade.lang.nodes.StringNode;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.ComponentListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.concurrent.ForkJoinPool;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ForkJoinPool;

/**
 * Main frame for editor
 */
@Singleton
public class EditorFrame extends JFrame {
    private static final Logger log = LogManager.getLogger(EditorFrame.class);

    private final ForkJoinPool pool;
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


    {
        String code =
                "var n = 500\n" +
                "var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))\n" +
                "var pi = 4 * reduce(sequence, 0, x y -> x + y)\n" +
                "print \"pi = \"\n" +
                "out pi" ;

        sourceCodeTextArea.setText(code);
    }

    @Inject
    public EditorFrame(@Named("editor-pool") ForkJoinPool pool, Interpreter interpreter, SettingsFrame settings) {
        this.pool = pool;
        this.interpreter = interpreter;
        this.settings = settings;

        setTitle("Jade Editor");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Container container = getContentPane();


        exitMenuItem.addActionListener(e -> exitEditorAction());
        openMenuItem.addActionListener(e -> openFileAction());
        fileMenu.add(openMenuItem);
        fileMenu.add(exitMenuItem);


        interpreterSettingsMenuItem.addActionListener(e -> showSettingsFrameAction());
        settingsMenu.add(interpreterSettingsMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);


        Font font = new Font("Monospaced", Font.PLAIN, 13);
        // main text area
        sourceCodeTextArea.setFont(font);
        sourceCodeTextArea.addKeyListener(Listeners.onKeyReleased(e -> runnerState.updateTime()));

        // bottom text area
        debugTextArea.setBorder(new LineBorder(Color.black));
        debugTextArea.setSize(100, 0);
        debugTextArea.setEnabled(false);
        debugTextArea.setFont(font);

        variablesTable.setSize(120, 300);

        runButton.addActionListener(e -> evalAction());

        container.add(variablesTable, BorderLayout.LINE_END);
        container.add(debugTextArea, BorderLayout.PAGE_END);
        container.add(sourceCodeTextArea, BorderLayout.CENTER);
        container.add(runButton, BorderLayout.PAGE_START);

        setJMenuBar(menuBar);
        setSize(640, 480);
        setVisible(true);



        Timer timer = new Timer(2000, e -> {
            long time = System.currentTimeMillis();
            if (runnerState.canRunInBackground(time)) {
                log.trace("Nothing changed, await");
                return;
            }

            runnerState.setLastEvaluatedTime(time);
            evalAction();
        });
        timer.start();
    }




    /*
     * Action handlers
     */

    private void openFileAction() {
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

    private void exitEditorAction() {
        log.debug("exitEditorAction");
        System.exit(0);
    }

    /**
     * Show settings frame
     */
    private void showSettingsFrameAction() {
        log.debug("showSettingsFrameAction");
        settings.setVisible(true);
    }


    /**
     * Run interpreter
     */
    private void evalAction() {
        log.debug("evalAction invoked");
        sourceCodeTextArea.getHighlighter().removeAllHighlights();

        String sourceCode = sourceCodeTextArea.getText();
        pool.submit(() -> eval(sourceCode));
    }


    private void eval(String sourceCode) {
        try {
            runnerState.setRunNow(true);
            long start = System.currentTimeMillis();

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
            Location location = ex.getLocation();
            String errorMessage = buildErrorMessage(sourceCode, ex);

            SwingUtilities.invokeLater(() -> {
                DefaultHighlighter.DefaultHighlightPainter error = new DefaultHighlighter.DefaultHighlightPainter(Color.red);
                try {
                    sourceCodeTextArea.getHighlighter().addHighlight(location.getIndex(), location.getIndex() + 1, error);
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
        int startLine = location.getIndex();
        for (; startLine > 0; startLine--) {
            if (sourceCode.charAt(startLine) == '\n')
                break;
        }

        int endLine = location.getIndex();
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
