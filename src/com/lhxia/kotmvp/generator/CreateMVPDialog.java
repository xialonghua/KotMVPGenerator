package com.lhxia.kotmvp.generator;

import javafx.scene.control.cell.ComboBoxListCell;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Vector;

public class CreateMVPDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textField1;
    private JTextField textField2;
    private JComboBox comboBox1;

    public CreateMVPDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        Vector<String> types = new Vector<>();
        types.add("无");
        types.add("Activity");
        types.add("Fragment");
        DefaultComboBoxModel<String> spinnerListModel = new DefaultComboBoxModel<>(types);
        comboBox1.setModel(spinnerListModel);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        // add your code here
        onOK(textField1.getText(), textField2.getText(), comboBox1.getSelectedIndex());
        dispose();

    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    protected void onOK(String contractName, String packageName, int typeIndex){

    }
}
