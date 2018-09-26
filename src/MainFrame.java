
import com.dpex.erp.WsServer;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.io.IOException;

import javax.swing.*;
public class MainFrame extends JFrame implements ActionListener{

    private JLabel jl1,jl2,jt_status,jt_port;
    private JButton jb_enter,jb_exit;
    private TrayIcon trayicon;
    private void initCompoenent()
    {
        setSize(300,200);
        setLayout(new GridLayout(3,2));
        setTitle("DPEX-PrintTool");
        jl1=new JLabel("status：");
        jl2=new JLabel("port：");
        jt_status=new JLabel("ok");
        jt_port=new JLabel("9898");

        jb_enter=new JButton("ok");
        jb_exit=new JButton("cacel");
        jb_enter.addActionListener(this);
        jb_exit.addActionListener(this);
        add(jl1);
        add(jt_status);
        add(jl2);
        add(jt_port);
        add(jb_enter);
        add(jb_exit);
        setVisible(true);
    }
    @Override
    public void actionPerformed(ActionEvent ex) {

        //窗体只是提示作用，不给关闭程序的按钮，需要到系统托盘右击关闭
        if(ex.getSource().equals(jb_enter)) {
            setVisible(false);
        }
        else if(ex.getSource().equals(jb_exit)) {
//            System.exit(0);
            setVisible(false);
        }
        else if(ex.getSource().equals(trayicon)) {
            if(!isVisible()) {
                setVisible(true);
                toFront();
            }
        }
    }
    public MainFrame() {
        initCompoenent();
        if(!SystemTray.isSupported()) {
            return;
        }
        else {
            SystemTray systemTray=SystemTray.getSystemTray();
            String title="printServer";
            String company="DPEX";
            Image image=Toolkit.getDefaultToolkit().getImage(getClass().getResource("image/print.png"));
            trayicon=new TrayIcon(image,title+"-"+company,createMenu());
            trayicon.addActionListener(this);
            try {
                systemTray.add(trayicon);
                trayicon.displayMessage(title, company, MessageType.INFO);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }
    private PopupMenu createMenu() {
        PopupMenu menu=new PopupMenu();
        MenuItem exit=new MenuItem("close");
        exit.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ex) {
                System.exit(0);
            }
        });
        MenuItem open=new MenuItem("open");
        open.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ex) {
                if(!isVisible()) {
                    setVisible(true);
                    toFront();
                }
                else {
                    toFront();
                }
            }
        });
        menu.add(open);
        menu.addSeparator();
        menu.add(exit);
        return menu;
    }
    public static void main(String[] args) throws IOException {
        MainFrame mf=new MainFrame();
        WsServer.main(new String[]{});

    }
}
