package ashes.of.jade.editor.frames;

import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.interpreter.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;


/**
 * Interpreter settings frame
 */
@Singleton
public class SettingsFrame extends JFrame {
    private static final Logger log = LogManager.getLogger(SettingsFrame.class);

    private final Interpreter interpreter;

    private final JPanel panel = new JPanel();
    private final JPanel settingsPane = new JPanel(new GridLayout(0,2));
    private final JPanel buttonPane = new JPanel(new GridLayout(0,2));


    private final JButton save = new JButton("Save");
    private final JButton close = new JButton("Close");


    private int mapParallelismSize;
    private int reduceParallelismSize;

    @Inject
    public SettingsFrame(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.mapParallelismSize = interpreter.getSettings().getMapParallelismSize();
        this.reduceParallelismSize = interpreter.getSettings().getReduceParallelismSize();

        setTitle("Interpeter Settings");

        panel.setLayout(new BorderLayout());
        panel.add(settingsPane, BorderLayout.CENTER);
        panel.add(buttonPane, BorderLayout.PAGE_END);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setTitle("Interpeter Settings");
        add(panel);


        createInputWithLabel(settingsPane, "map() min parallel size:", evt -> mapParallelismSize = ((Number) evt.getNewValue()).intValue());
        createInputWithLabel(settingsPane, "reduce() min parallel size:", evt -> mapParallelismSize = ((Number) evt.getNewValue()).intValue());


        save.addActionListener(e -> saveAndHide());


        close.addActionListener(e -> donNotSaveAndHide());


        add(settingsPane, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.PAGE_END);

        buttonPane.add(close);
        buttonPane.add(save);

        pack();
    }


    private void createInputWithLabel(JPanel pane, String labelText, PropertyChangeListener listener) {
        NumberFormat format = NumberFormat.getNumberInstance();

        JFormattedTextField input = new JFormattedTextField(format);
        input.setValue(mapParallelismSize);
        input.setColumns(20);
        input.addPropertyChangeListener("value", listener);

        JLabel label = new JLabel(labelText);
        label.setLabelFor(input);

        pane.add(label);
        pane.add(input);
    }


    /*
     * Event listeners
     */

    /**
     * Hides this window without saving changes
     */
    private void donNotSaveAndHide() {
        setVisible(false);
    }

    /**
     * Saves changes and hides this window
     */
    private void saveAndHide() {
        log.info("Save mapParallelismSize={} reduceParallelismSize={}",
                mapParallelismSize, reduceParallelismSize);
        Settings settings = interpreter.getSettings();
        settings.setMapParallelismSize(mapParallelismSize);
        settings.setReduceParallelismSize(reduceParallelismSize);

        setVisible(false);
    }
}


