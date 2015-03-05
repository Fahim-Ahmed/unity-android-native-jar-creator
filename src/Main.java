import javax.swing.*;

public class Main {

    static void setTheme(){
        try {
            UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    static void initView(){
        MainInterface mi = new MainInterface();
        mi.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mi.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setTheme();
                initView();
            }
        });
    }
}