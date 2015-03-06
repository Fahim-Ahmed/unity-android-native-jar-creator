import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.*;
/*
 * Created by JFormDesigner on Thu Mar 05 00:12:42 BDT 2015
 */



/**
 * @author unknown
 */
public class MainInterface extends JFrame {
    private JFileChooser fc;

    public MainInterface() {
        initComponents();

        BufferedImage image = null;

        try {
            image = ImageIO.read(new File("icon"));
            setIconImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }


        super.setIconImage(image);
    }

    private void btnSourceActionPerformed(ActionEvent e) {
        String path = showDirChooser();
        if(path != null) {
            tfSource.setText(path);
            System.out.println("-----> " + path);
        }
    }

    private void BtnDestActionPerformed(ActionEvent e) {
        String path = showDirChooser();
        if(path != null) {
            tfDest.setText(path);
            System.out.println("-----> " + path);
        }
    }

    private String showDirChooser(){
        if(fc == null) {
            fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }

        if(fc.showOpenDialog(MainInterface.this) == JFileChooser.APPROVE_OPTION){
            return fc.getSelectedFile().getAbsolutePath();
        }

        return null;
    }

    private void button3ActionPerformed(ActionEvent e) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doTheDirtyWorks();
            }
        }).start();
    }

    private void doTheDirtyWorks() {

        //tfPackage.setText("com\\ice9apps\\AndroidDiplomat");
        //tfSource.setText("C:\\Users\\Fahim\\Desktop\\Android Diplomat\\src");
        //tfDest.setText("C:\\Users\\Fahim\\Desktop");

        String packageName = tfPackage.getText();
        String src = tfSource.getText();
        String dest = tfDest.getText();

        if(packageName.length() == 0){
            showWarning("invalid package name.");
            return;
        }else if(src.length() == 0){
            showWarning("invalid source name.");
            return;
        }else if(dest.length() == 0){
            showWarning("invalid destination name.");
            return;
        }

        packageName = packageName.replace(".", "\\");

        String srcPath = (src.contains(packageName)) ? src : src + "\\" + packageName;
        String classpath = "libs\\classes.jar";
        String bootclasspath = "libs\\android.jar";
        String fileName = "files.dat";
        String tmpDest = "compiled";

        File tmpDir = new File(tmpDest);
        if(!tmpDir.exists())
            if(!tmpDir.mkdir()) {
                showWarning("permission error. sigh.");
                return;
            }

        try {
            writeFileList(tmpDest + "\\" + fileName, srcPath);
        } catch (IOException e) {
            e.printStackTrace();
            showWarning("epic fail: IO error. please die.");
            return;
        }

        String tmp = packageName.replace("\\", "/");  //String split don't support stupid backslash -_-
        String[] ss = tmp.split("/");
        String jName = ss[ss.length-1];

        //set ingredients
        String[] options = new String[]{
                "-bootclasspath", bootclasspath,
                "-classpath", classpath,
                "-source", "1.6",
                "-target", "1.6",
                "-d", new File("").getAbsolutePath(),
                "@" + tmpDest + "\\" + fileName,
               // srcPath
        };

        showStatus("evolving...");
        //start compile
        com.sun.tools.javac.Main.compile(options);

        //jar generation start ----------------------------------------------------------------------------------------------------
        showStatus("class file generated.");

        System.out.println("-----> " +jName);

        showStatus("creating jar...");
        try {
            String destPath = dest + "\\" + jName + ".jar";
            createJar(destPath, ss[0]);

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("epic fail: IO error. please die. probably classpath files are missing. sigh.");
            return;
        }

        showStatus("temp file delete success: " + deleteDirectory(new File(ss[0])));
        showStatus("jar created. you can now break it and pat yourself.");

        System.out.println("-----> " + srcPath);
    }

    boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if(file.getName().endsWith(".class"))
                            file.delete();
                    }
                }
            }
        }
        return (path.delete());
    }

    private void writeFileList(String path, String srcPath) throws IOException {
        showStatus("populating swarm...");
        File file = new File(path);

        FileWriter fw = new FileWriter(file);
        File srcFolder = new File(srcPath);

        writeListRecursively(srcFolder, fw);

        fw.close();
        showStatus("population completed.");
    }

    void writeListRecursively(final File src, final FileWriter fw) throws IOException {
        for (final File fileEntry : src.listFiles(new FileFilter() {
                                                      @Override
                                                      public boolean accept(File f) {
                                                          String n = f.getName();
                                                          return n.endsWith(".java") || f.isDirectory();
                                                      }
                                                  }
        )) {
            if (fileEntry.isDirectory()) {
                writeListRecursively(fileEntry, fw);
            } else {
                String fp = fileEntry.getAbsolutePath().replace("\\", "/");
                fw.write("\"");
                fw.write(fp);
                fw.write("\"");
                fw.write(System.getProperty("line.separator"));
            }
        }
    }

    void showWarning(String s){
        lStat.setForeground(new Color(255, 102, 102));
        lStat.setText(s);
    }

    void showStatus(String s){
        lStat.setForeground(new Color(0, 255, 153));
        lStat.setText(s);
    }

    public void createJar(String out, String input) throws IOException
    {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        JarOutputStream target = new JarOutputStream(new FileOutputStream(out), manifest);

//        File file = new File(input);
//        if(file.isDirectory() && !file.exists())
//            showStatus("creating dir: " + file.mkdir());

        add(new File(input), target);

        target.close();
    }

    private void add(File source, JarOutputStream target) throws IOException
    {
        BufferedInputStream in = null;
        try
        {
            if (source.isDirectory())
            {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty())
                {
                    if (!name.endsWith("/")) name += "/";

                    JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    target.putNextEntry(entry);
                    target.closeEntry();
                }
                for (File nestedFile: source.listFiles()) {
                    if(nestedFile.getName().endsWith(".class") || nestedFile.isDirectory())
                        add(nestedFile, target);
                }
                return;
            }

            JarEntry entry = new JarEntry(source.getPath().replace("\\", "/"));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        }
        finally
        {
            if (in != null)
                in.close();
        }
      }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - fahim ahmed
        label1 = new JLabel();
        tfPackage = new JTextField();
        tfSource = new JTextField();
        label2 = new JLabel();
        tfDest = new JTextField();
        label3 = new JLabel();
        btnSource = new JButton();
        BtnDest = new JButton();
        button3 = new JButton();
        lStat = new JLabel();

        //======== this ========
        setMinimumSize(new Dimension(520, 380));
        setResizable(false);
        setTitle("Unity Android Plugin Generator");
        Container contentPane = getContentPane();

        //---- label1 ----
        label1.setText("Package Name");
        label1.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        label1.setToolTipText("eg. com.company.app");

        //---- tfPackage ----
        tfPackage.setToolTipText("eg. com.company.app");
        tfPackage.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));

        //---- tfSource ----
        tfSource.setToolTipText("src directory path");
        tfSource.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));

        //---- label2 ----
        label2.setText("Source Path");
        label2.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        label2.setToolTipText("src directory path");

        //---- tfDest ----
        tfDest.setToolTipText("Unity asset/plugins/android directory path");
        tfDest.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));

        //---- label3 ----
        label3.setText("Destination Path");
        label3.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        label3.setToolTipText("Unity asset/plugins/android directory path");

        //---- btnSource ----
        btnSource.setText("O ");
        btnSource.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnSourceActionPerformed(e);
            }
        });

        //---- BtnDest ----
        BtnDest.setText("O ");
        BtnDest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BtnDestActionPerformed(e);
            }
        });

        //---- button3 ----
        button3.setText("Convert");
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button3ActionPerformed(e);
            }
        });

        //---- lStat ----
        lStat.setText("Zzz...");
        lStat.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        lStat.setForeground(new Color(0, 255, 153));

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addGap(16, 16, 16)
                    .addGroup(contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                            .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addGroup(contentPaneLayout.createSequentialGroup()
                                    .addComponent(label3, GroupLayout.PREFERRED_SIZE, 96, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(tfDest, GroupLayout.PREFERRED_SIZE, 310, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(BtnDest, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE))
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(contentPaneLayout.createSequentialGroup()
                                        .addComponent(label2, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(tfSource, GroupLayout.PREFERRED_SIZE, 310, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnSource, GroupLayout.PREFERRED_SIZE, 48, GroupLayout.PREFERRED_SIZE))
                                    .addGroup(contentPaneLayout.createSequentialGroup()
                                        .addComponent(label1, GroupLayout.PREFERRED_SIZE, 78, GroupLayout.PREFERRED_SIZE)
                                        .addGap(24, 24, 24)
                                        .addComponent(tfPackage, GroupLayout.PREFERRED_SIZE, 364, GroupLayout.PREFERRED_SIZE))))
                            .addContainerGap(22, Short.MAX_VALUE))
                        .addGroup(contentPaneLayout.createSequentialGroup()
                            .addComponent(button3, GroupLayout.DEFAULT_SIZE, 476, Short.MAX_VALUE)
                            .addGap(12, 12, 12))))
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addGap(20, 20, 20)
                    .addComponent(lStat, GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
        );
        contentPaneLayout.setVerticalGroup(
            contentPaneLayout.createParallelGroup()
                .addGroup(contentPaneLayout.createSequentialGroup()
                    .addGap(20, 20, 20)
                    .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(label1)
                        .addComponent(tfPackage, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnSource)
                        .addComponent(tfSource, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(label2))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(BtnDest)
                        .addComponent(tfDest, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(label3))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 110, Short.MAX_VALUE)
                    .addComponent(lStat)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(button3, GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        setSize(520, 380);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - fahim ahmed
    private JLabel label1;
    private JTextField tfPackage;
    private JTextField tfSource;
    private JLabel label2;
    private JTextField tfDest;
    private JLabel label3;
    private JButton btnSource;
    private JButton BtnDest;
    private JButton button3;
    private JLabel lStat;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
