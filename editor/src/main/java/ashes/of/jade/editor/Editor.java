package ashes.of.jade.editor;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;


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
                "var seq = {4, 6}\n" +
                "var sequence = map(seq, i -> i * i)\n" +
//                "var pi = 3.1415 * reduce (sequence, 0, x y -> x + y)\n" +
//                "var pi = 1 * reduce(sequence, 1000, acc y -> acc + y)\n" +
                "var pi = 1 * reduce(sequence, 1, acc y -> acc * y)\n" +
                "print \"pi = \"\n" +
                "out pi\n" +
                "" ;

        text.setText(code);
    }

    private void start() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(640, 480);
        frame.setLayout(new BorderLayout());

        Container container = frame.getContentPane();

//        JLabel left = new JLabel("left");
//        left.setBorder(new LineBorder(Color.black));
//        container.add(left, BorderLayout.LINE_START);


        JMenuItem runItem = new JMenuItem("Run");
        runItem.addActionListener(e -> runInterpreter());

        runMenu.add(runItem);

        menuBar.add(fileMenu);
        menuBar.add(runMenu);

        frame.setJMenuBar(menuBar);


        JTable debug = new JTable(model);


        info.setBorder(new LineBorder(Color.black));
        info.setSize(100, 0);
        info.setEnabled(false);

        container.add(debug, BorderLayout.LINE_END);
        container.add(info, BorderLayout.PAGE_END);
        container.add(text, BorderLayout.CENTER);


//        JLabel top = new JLabel("top");
//        top.setBorder(new LineBorder(Color.black));
//        container.add(top, BorderLayout.PAGE_START);

        JButton button = new JButton("run");
        container.add(button, BorderLayout.PAGE_START);
        button.addActionListener(e -> runInterpreter());


        frame.setVisible(true);
    }

    private void runInterpreter() {
        log.debug("runInterpreter invoked");
        try {
            String sourceCode = text.getText();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(baos);

            Interpreter interpreter = new Interpreter();
            interpreter.setOut(stream);
            Interpreter.Scope state = interpreter.eval(sourceCode);

            info.setText(baos.toString(Charset.defaultCharset().name()));


            model.clear();
            state.vars.forEach(model::add);
            model.fireTableStructureChanged();
        } catch (ParseException ex) {

            StringBuilder b = new StringBuilder()
                    .append(ex.getLine())
                    .append("\n");

            Location location = ex.getLocation();
            for (int i = 0; i < location.offset; i++) {
                b.append(" ");
            }

            b.append("^ ").append(ex.getMessage());

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
