package ashes.of.jade.editor;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.StringNode;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;


public class Editor {
    private static final Logger log = LogManager.getLogger(Editor.class);

    private JFrame frame = new JFrame("Jade Editor");
    private JTextArea text = new JTextArea(20, 40);
    private JTextArea info = new JTextArea(5, 40);

    private VariablesTableModel model = new VariablesTableModel();

    private JMenuBar menuBar = new JMenuBar();
    private JMenu fileMenu = new JMenu("File");
    private JMenu runMenu = new JMenu("Run");

    {
        String code =
                "var seq = {0, 10000}\n" +
                "var sequence = map(seq, i -> i + i)\n" +
                "var pi = 1 * reduce(sequence, 1, acc y -> acc + y)\n" +
                "print \"pi = \"\n" +
                "out pi\n" ;

        text.setText(code);

    }

    private void start() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setLayout(new BorderLayout());

        Container container = frame.getContentPane();


        JMenuItem runItem = new JMenuItem("Run");
        runItem.addActionListener(e -> runInterpreter());

        runMenu.add(runItem);

        menuBar.add(fileMenu);
        menuBar.add(runMenu);

        frame.setJMenuBar(menuBar);


        Font font = new Font("Monospaced", Font.PLAIN, 13);
        // main text area
        text.setFont(font);

        // bottom text area
        info.setBorder(new LineBorder(Color.black));
        info.setSize(100, 0);
        info.setEnabled(false);
        info.setFont(font);

        JTable debug = new JTable(model);
        debug.setSize(120, 300);
        container.add(debug, BorderLayout.LINE_END);
        container.add(info, BorderLayout.PAGE_END);
        container.add(text, BorderLayout.CENTER);

        JButton button = new JButton("run");
        container.add(button, BorderLayout.PAGE_START);
        button.addActionListener(e -> runInterpreter());


        frame.setVisible(true);
    }

    private void runInterpreter() {
        log.debug("runInterpreter invoked");
        try {
            long start = System.currentTimeMillis();
            String sourceCode = text.getText();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(baos);

            Interpreter interpreter = new Interpreter();
            interpreter.setOut(stream);
            Interpreter.Scope state = interpreter.eval(sourceCode);

            info.setText(baos.toString(Charset.defaultCharset().name()));


            model.clear();
            state.vars.forEach(model::add);
            model.add("_time", new StringNode(String.format("%.3f", (System.currentTimeMillis() - start) / 1000.0 )));
            model.fireTableStructureChanged();
        } catch (ParseException ex) {

            StringBuilder b = new StringBuilder()
                    .append(ex.getLine())
                    .append("\n");

            Location location = ex.getLocation();
            for (int i = 0; i < location.offset - 1; i++)
                b.append(" ");

            b.append("^ ").append(ex.getMessage());


            DefaultHighlighter.DefaultHighlightPainter error = new DefaultHighlighter.DefaultHighlightPainter(Color.red);
            try {
                text.getHighlighter().addHighlight(location.index, location.index + 1, error);
            } catch (BadLocationException e) {
                log.error("Can't highlight", ex);
            }

            info.setText(b.toString());
        } catch (Exception ex) {
            info.setText("Oh shit!");
            log.error("Very bad thing happened", ex);
        }
    }



    public static class VariableTableRow  {
        public final String var;
        public final Node node;

        public VariableTableRow(String var, Node node) {
            this.var = var;
            this.node = node;
        }


    }

    public static class VariablesTableModel extends AbstractTableModel {

        private List<VariableTableRow> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            VariableTableRow row = rows.get(rowIndex);

            switch (columnIndex) {
                case 0: return row.var;
                case 1: return row.node;
                default:
                    return null;
            }
        }

        public void clear() {
            rows = new ArrayList<>();
        }

        public void add(String var, Node val) {
            rows.add(new VariableTableRow(var, val));
        }
    }


    public static void main(String... args) {
        Editor editor = new Editor();
        SwingUtilities.invokeLater(editor::start);
    }
}
