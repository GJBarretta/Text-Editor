package texteditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import views.TheFrame;

/**
 *
 * @author Giuseppe Barretta
 */

public class TextEditor {
    private final TheFrame frame; //for a JFrame object
    private File file;            //dedicated File member
    private Path path;            //dedicated Path member
    private boolean fileLoaded,   //to check if an already existing file is loaded
            newFile,      //to check if a file has never been saved
            modified;     //to check if a file has been modified

    private static JFileChooser getFileChooser() {
        JFileChooser chooser = new JFileChooser();

        // specify where chooser should open up
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        // define a set of "Editable Files" files by extension
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Editable Files", "txt", "java")
        );

        // do not accept "All Files"
        chooser.setAcceptAllFileFilterUsed(false);

        return chooser;
    }

    public TextEditor() {
        frame = new TheFrame();

        //set all of the boolean data members to false on initialization
        //so that the only options available are new and open
        fileLoaded = false;
        newFile = false;
        modified = false;

        frame.setTitle(getClass().getSimpleName());
        frame.setLocationRelativeTo(null);
        frame.setSize(600, 500);

        frame.getContentTextArea().setEditable(false);
        frame.getSplitPane().setEnabled(false);

        //event handlers
        frame.getOpenMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //exit out of method if JOptionPane does not return 1
                if (modified) {
                    String message = "Select an Option";
                    String title = "OK to discard changes?";
                    int option = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.NO_OPTION || option == JOptionPane.CANCEL_OPTION)
                        return;
                }

                JFileChooser chooser = TextEditor.getFileChooser();

                // invoke the chooser dialog for opening a file
                int status = chooser.showOpenDialog(frame);

                // test for approval
                if (status != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                file = chooser.getSelectedFile();
                path = file.toPath();

                try {
                    String content = new String(Files.readAllBytes(path));
                    frame.getContentTextArea().setText(content);
                    frame.getContentTextArea().setCaretPosition(0);

                    frame.getContentTextArea().setEditable(true);
                    frame.getFileNameTextField().setText(file.getName());
                    frame.getModifiedTextField().setText("");

                    fileLoaded = true;
                    newFile = false;
                    modified = false;
                }
                catch (IOException ex) {
                    ex.printStackTrace(System.err);
                    JOptionPane.showMessageDialog(frame, "Cannot open file " + file);
                }
            }
        });

        frame.getNewMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //exit out of method if JOptionPane doesn't return a 1
                if (modified) {
                    String message = "OK to discard changes?";
                    String title = "Select an Option";
                    int option = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.NO_OPTION || option == JOptionPane.CANCEL_OPTION)
                        return;
                }

                frame.getFileNameTextField().setText("<NEW FILE>");

                frame.getModifiedTextField().setText("");
                frame.getContentTextArea().setText("");
                frame.getContentTextArea().setEditable(true);

                newFile = true;
                fileLoaded = false;
                modified = false;
            }
        });

        frame.getSaveAsMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = TextEditor.getFileChooser();

                // invoke the chooser dialog for opening a file
                int status = chooser.showSaveDialog(frame);

                // test for approval
                if (status != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                path = chooser.getSelectedFile().toPath();
                file = path.toFile();

                //the goal for line 160 to line 179 is to make sure a file of the same
                //name doesn't already exist in the directory, but if the user types in
                //a file without an extension, then an exists() check will return false
                //and allow the method to continue. the method will then create the file
                //and add a .txt extension which will overwrite the already existing file.
                //so I am creating a temporary file with the .txt appended before the check
                String fileName = file.getName();
                boolean modName = false; //flag to know whether to add .txt later
                File temp;

                if (fileName.toLowerCase().endsWith(".txt") || fileName.toLowerCase().endsWith(".java")) {
                    temp = new File(file.getPath());
                }
                else {
                    modName = true;
                    fileName += ".txt";
                    temp = new File(file.getPath()+".txt");
                }

                if (temp.exists()) {
                    String message = "OK to overwrite existing file?";
                    String title = "Select an Option";
                    int option = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.NO_OPTION || option == JOptionPane.CANCEL_OPTION)
                        return;
                }

                try {
                    Path working = Paths.get(System.getProperty("user.dir","subdir"));
                    String content = frame.getContentTextArea().getText();

                    Files.write(path, content.getBytes());

                    if (modName) {
                        //add the .txt extension if necessary
                        File newFile = new File(file.toPath().toString() + ".txt");
                        if(file.renameTo(new File(newFile.toPath().toString())))
                            file = newFile;
                        path = file.toPath();
                    }

                    fileLoaded = true;  //the file is now created and loaded in memory
                    newFile = false;    //the file is no longer new
                    modified = false;
                    frame.getModifiedTextField().setText("");

                    String strWorking = working.toString() + File.separatorChar + fileName;

                    if (strWorking.equals(file.toPath().toString())) {
                        frame.getFileNameTextField().setText(fileName);
                    }
                    else {
                        //pathDifference method returns the difference between the working
                        //path and the file path, which will end up being the subdirectory
                        //that the file is located in
                        String diff = pathDifference(working.toString(),path.toString());
                        frame.getFileNameTextField().setText(diff);
                    }
                }
                catch (IOException ex) {
                    ex.printStackTrace(System.err);
                    JOptionPane.showMessageDialog(frame, "Cannot save file " + file);
                }
            }
        });

        frame.getSaveMenuItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String content = frame.getContentTextArea().getText();
                    Files.write(path, content.getBytes());

                    modified = false;
                    frame.getModifiedTextField().setText("");
                }
                catch (IOException ex) {
                    ex.printStackTrace(System.err);
                    JOptionPane.showMessageDialog(frame, "Cannot save file " + file);
                }
            }
        });

        frame.getFileMenu().addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                if (fileLoaded) {
                    frame.getSaveAsMenuItem().setEnabled(true);
                    frame.getSaveMenuItem().setEnabled(true);
                }
                else if (newFile) {
                    frame.getSaveAsMenuItem().setEnabled(true);
                    frame.getSaveMenuItem().setEnabled(false);
                }
                else {
                    frame.getSaveAsMenuItem().setEnabled(false);
                    frame.getSaveMenuItem().setEnabled(false);
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });

        frame.getContentTextArea().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent ke) {
                String memContent = frame.getContentTextArea().getText();

                if (fileLoaded) {
                    try {
                        //if fileLoaded is true then the path data member will point to an
                        //existing file. copy the text content of this file into fileContent
                        String fileContent = new String(Files.readAllBytes(path));

                        if (fileContent.equals(memContent)) {
                            //if fileContent is equal to the current content of the textField
                            //then it is no longer modified. doing it this way allows the
                            //modified flag to be set back to false if the content goes back
                            //to being unmodified from a modified state
                            frame.getModifiedTextField().setText("");
                            modified = false;
                        }
                        else {
                            frame.getModifiedTextField().setText("  *  ");
                            modified = true;
                        }
                    }
                    catch (IOException ex) {
                        ex.printStackTrace(System.err);
                    }
                }
                else if (newFile) {
                    //the only way that a newFile is modified is if it is not empty or null
                    if (memContent != null && !memContent.isEmpty()) {
                        frame.getModifiedTextField().setText("  *  ");
                        modified = true;
                    }
                    else {
                        //if it is both newFile and (null or empty)
                        //then it is necessarily unmodified
                        frame.getModifiedTextField().setText("");
                        modified = false;
                    }
                }
            }
        });

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                if (modified) {
                    //if the textarea is modified then make sure
                    //user is OK with discarding the changes
                    String message = "Select an Option";
                    String title = "OK to discard changes?";
                    int option = JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.YES_OPTION)
                        System.exit(0);
                }
                else
                    System.exit(0);
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TextEditor app = new TextEditor();
        app.frame.setVisible(true);
    }

    //method to return the subdirectory path from the working directory
    private String pathDifference(String working, String path) {
        if (working.length() > path.length())
            return working.substring(path.length()+1);
        else
            return path.substring(working.length()+1);
    }
}