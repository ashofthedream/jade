package ashes.of.jade.editor.frames;

import ashes.of.jade.editor.VariablesTableModel;
import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.interpreter.Scope;
import ashes.of.jade.lang.nodes.StringNode;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;


/**
 * Main frame for editor
 */
@Singleton
public class EditorFrame extends JFrame {
    private static final Logger log = LogManager.getLogger(EditorFrame.class);

    private final Interpreter interpreter;
    private final SettingsFrame settings;

    /*
     * menu
     */
    private final JMenuBar menuBar = new JMenuBar();
    private final JMenu fileMenu = new JMenu("File");
    private final JMenu settingsMenu = new JMenu("Settings");
    private final JMenuItem interpreterSettingsMenuItem = new JMenuItem("Interpreter Settings...");

    /**
     * Main text area
     */
    private final JTextArea text = new JTextArea(20, 40);

    /**
     * Debug info text area (read only)
     */
    private final JTextArea info = new JTextArea(5, 40);



    private final VariablesTableModel model = new VariablesTableModel();

    /**
     * Right variables table
     */
    private final JTable variablesTable = new JTable(model);


    {
        String code =
                "var seq = {0, 10000}\n" +
                        "var sequence = map(seq, i -> i + i)\n" +
                        "var pi = 1 * reduce(sequence, 1, acc y -> acc + y)\n" +
                        "print \"pi = \"\n" +
                        "out pi\n" ;

        text.setText(code);
    }


    @Inject
    public EditorFrame(Interpreter interpreter, SettingsFrame settings) {
        this.interpreter = interpreter;
        this.settings = settings;

        setTitle("Jade Editor");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Container container = getContentPane();


        interpreterSettingsMenuItem.addActionListener(e -> showSettingsFrame());

        settingsMenu.add(interpreterSettingsMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);


        Font font = new Font("Monospaced", Font.PLAIN, 13);
        // main text area
        text.setFont(font);

        // bottom text area
        info.setBorder(new LineBorder(Color.black));
        info.setSize(100, 0);
        info.setEnabled(false);
        info.setFont(font);


        variablesTable.setSize(120, 300);
        container.add(variablesTable, BorderLayout.LINE_END);
        container.add(info, BorderLayout.PAGE_END);
        container.add(text, BorderLayout.CENTER);

        JButton button = new JButton("run");
        button.addActionListener(e -> runInterpreter());

        container.add(button, BorderLayout.PAGE_START);


        setJMenuBar(menuBar);
        setSize(640, 480);
        setVisible(true);
    }


    /*
     * Event listeners
     */

    /**
     * Show settings frame
     */
    private void showSettingsFrame() {
        settings.setVisible(true);
    }


    /**
     * Run interpreter
     */
    private void runInterpreter() {
        log.debug("runInterpreter invoked");
        String sourceCode = text.getText();
        try {
            text.getHighlighter().removeAllHighlights();
            long start = System.currentTimeMillis();


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(baos);
            interpreter.setOut(stream);

            Scope state = interpreter.eval(sourceCode);

            info.setText(baos.toString(Charset.defaultCharset().name()));

            model.clear();
            state.getVars().forEach(model::add);
            model.add("_time", new StringNode(String.format("%.3f", (System.currentTimeMillis() - start) / 1000.0 )));
            model.fireTableStructureChanged();
        } catch (ParseException ex) {

            Location location = ex.getLocation();
            String errorMessage = buildErrorMessage(sourceCode, ex);

            DefaultHighlighter.DefaultHighlightPainter error = new DefaultHighlighter.DefaultHighlightPainter(Color.red);
            try {
                text.getHighlighter().addHighlight(location.getIndex(), location.getIndex() + 1, error);
            } catch (BadLocationException e) {
                log.error("Can't highlight", ex);
            }


            info.setText(errorMessage);
        } catch (Exception ex) {
            info.setText("Oh shit!");
            log.error("Very bad thing happened", ex);
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
