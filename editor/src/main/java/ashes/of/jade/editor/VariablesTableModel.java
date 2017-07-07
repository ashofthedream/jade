package ashes.of.jade.editor;

import ashes.of.jade.lang.nodes.Node;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class VariablesTableModel extends AbstractTableModel {

    public static class VariableTableRow  {
        public final String var;
        public final Node node;

        public VariableTableRow(String var, Node node) {
            this.var = var;
            this.node = node;
        }
    }


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
