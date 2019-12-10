import org.w3c.dom.events.Event;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Timer;

public class ClientRMIGUI extends JFrame implements ActionListener, Serializable {

    private static final long serialVersionUID = 1L;
    private JPanel textPanel, inputPanel;
    private JTextField textField;
    private String name, message;
    private Font meiryoFont = new Font("Meiryo", Font.PLAIN, 14);
    private Border blankBorder = BorderFactory.createEmptyBorder(10,10,20,10);//top,r,b,l
    private ChatClient chatClient;
    private JList<String> list;
    private DefaultListModel<String> listModel;
    private TimerThread timerThread;

    protected JTextArea textArea, userArea;
    protected JFrame frame;
    protected JButton privateMsgButton, startButton, sendButton,getButton;
    protected JPanel clientPanel, userPanel;

    /**
     * Main method to start client GUI app.
     * @param args
     */
    public static void main(String args[]){
        //set the look and feel to 'Nimbus'
        try{
            for(UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()){
                if("Nimbus".equals(info.getName())){
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch(Exception e){
        }
        new ClientRMIGUI();
    }//end main


    /**
     * GUI Constructor
     */
    public ClientRMIGUI(){

        frame = new JFrame("Client Chat Console");

        //-----------------------------------------
        /*
         * intercept close method, inform server we are leaving
         * then let the system exit.
         */
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {

                if(chatClient != null){
                    try {
                        sendMessage("Bye all, I am leaving");
                        chatClient.serverIF.leaveChat(name);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                System.exit(0);
            }
        });
        //-----------------------------------------
        //remove window buttons and border frame
        //to force user to exit on a button
        //- one way to control the exit behaviour
        //frame.setUndecorated(true);
        //frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        Container c = getContentPane();
        JPanel outerPanel = new JPanel(new BorderLayout());

        outerPanel.add(getInputPanel(), BorderLayout.CENTER);
        outerPanel.add(getTextPanel(), BorderLayout.NORTH);

        c.setLayout(new BorderLayout());
        c.add(outerPanel, BorderLayout.CENTER);
        c.add(getUsersPanel(), BorderLayout.WEST);

        frame.add(c);
        frame.pack();
        frame.setAlwaysOnTop(true);
        frame.setLocation(150, 150);
        textField.requestFocus();

        frame.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e){
                closeAll();
            }
        });

        frame.setVisible(true);
    }

    /**
     * action to close window and close the timerthread
     */
    public void closeAll(){
        timerThread.stopGettingMessages();
        System.exit(0);
    }


    /**
     * Method to set up the JPanel to display the chat text
     * @return
     */
    public JPanel getTextPanel(){
        String welcome = "Welcome enter your name and press Start to begin\n";
        textArea = new JTextArea(welcome, 14, 34);
        textArea.setMargin(new Insets(10, 10, 10, 10));
        textArea.setFont(meiryoFont);

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textPanel = new JPanel();
        textPanel.add(scrollPane);

        textPanel.setFont(new Font("Meiryo", Font.PLAIN, 14));
        return textPanel;
    }

    /**
     * Method to build the panel with input field
     * @return inputPanel
     */
    public JPanel getInputPanel(){
        inputPanel = new JPanel(new GridLayout(1, 1, 5, 5));
        inputPanel.setBorder(blankBorder);
        textField = new JTextField();
        textField.setFont(meiryoFont);
        inputPanel.add(textField);
        return inputPanel;
    }

    /**
     * Method to build the panel displaying currently connected users
     * with a call to the button panel building method
     * @return
     */
    public JPanel getUsersPanel(){

        userPanel = new JPanel(new BorderLayout());
        String  userStr = " Current Users      ";

        JLabel userLabel = new JLabel(userStr, JLabel.CENTER);
        userPanel.add(userLabel, BorderLayout.NORTH);
        userLabel.setFont(new Font("Meiryo", Font.PLAIN, 16));

        String[] noClientsYet = {"No other users"};
        setClientPanel(noClientsYet);

        clientPanel.setFont(meiryoFont);
        userPanel.add(makeButtonPanel(), BorderLayout.SOUTH);
        userPanel.setBorder(blankBorder);

        return userPanel;
    }

    /**
     * Populate current user panel with a
     * selectable list of currently connected users
     * @param currClients
     */
    public void setClientPanel(String[] currClients) {
        clientPanel = new JPanel(new BorderLayout());
        listModel = new DefaultListModel<String>();

        for(String s : currClients){
            listModel.addElement(s);
        }
        if(currClients.length > 1){
            privateMsgButton.setEnabled(true);
        }

        //Create the list and put it in a scroll pane.
        list = new JList<String>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(8);
        list.setFont(meiryoFont);
        JScrollPane listScrollPane = new JScrollPane(list);

        clientPanel.add(listScrollPane, BorderLayout.CENTER);
        userPanel.add(clientPanel, BorderLayout.CENTER);
    }

    /**
     * Make the buttons and add the listener
     * @return
     */
    public JPanel makeButtonPanel() {
        sendButton = new JButton("Send ");
        sendButton.addActionListener(this);
        sendButton.setEnabled(false);

        privateMsgButton = new JButton("Send PM");
        privateMsgButton.addActionListener(this);
        privateMsgButton.setEnabled(false);

        startButton = new JButton("Start ");
        startButton.addActionListener(this);

        /*getButton= new JButton("get messages");
        getButton.addActionListener(this);
        getButton.setEnabled(true);*/

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1));
        buttonPanel.add(privateMsgButton);
        buttonPanel.add(new JLabel(""));
        buttonPanel.add(startButton);
        buttonPanel.add(sendButton);
        //buttonPanel.add(getButton);

        return buttonPanel;
    }

    /**
     * Action handling on the buttons
     */
    @Override
    public void actionPerformed(ActionEvent e){

        try {
            //get connected to chat service
            if(e.getSource() == startButton){
                name = textField.getText();
                if(name.length() != 0){
                    frame.setTitle(name + "'s console ");
                    textField.setText("");
                    textArea.append("username : " + name + " connecting to chat...\n");
                    getConnected(name);
                    if(!chatClient.connectionProblem){
                        startButton.setEnabled(false);
                        sendButton.setEnabled(true);
                    }
                    timerThread= new TimerThread(chatClient);
                    timerThread.start();
                }
                else{
                    JOptionPane.showMessageDialog(frame, "Enter your name to Start");
                }
            }

            //get text and clear textField
            if(e.getSource() == sendButton){
                message = textField.getText();
                textField.setText("");
                sendMessage(message);
                System.out.println("Sending message : " + message);
            }

            //send a private message, to selected users
            if(e.getSource() == privateMsgButton){
                int[] privateList = list.getSelectedIndices();

                for(int i=0; i<privateList.length; i++){
                    System.out.println("selected index :" + privateList[i]);
                }
                message = textField.getText();
                textField.setText("");
                //sendPrivate(privateList);
            }

            /*if(e.getSource()== getButton){
                String mes=chatClient.serverIF.getMessageFrom(name);
                textArea.append("New message for me: "+mes);
            }*/

        }
        catch (RemoteException remoteExc) {
            remoteExc.printStackTrace();
        }

    }//end actionPerformed

    // --------------------------------------------------------------------

    /**
     * Send a message, to be relayed to all chatters
     * @param chatMessage
     * @throws RemoteException
     */
    private void sendMessage(String chatMessage) throws RemoteException {
        //chatClient.serverIF.sendMessageTo("Robbe", chatMessage);
        chatClient.sendMessage(chatMessage);
        System.out.println("succeeded to send to server:" + chatMessage);
    }

    /**
     * Send a message, to be relayed, only to selected chatters

     * @throws RemoteException
     */
   /* private void sendPrivate(int[] privateList) throws RemoteException {
        String privateMessage = "[PM from " + name + "] :" + message + "\n";
        chatClient.serverIF.sendPM(privateList, privateMessage);
    }*/

    /**
     * Make the connection to the chat server
     * @param userName
     * @throws RemoteException
     */
    private void getConnected(String userName) throws RemoteException{
        //remove whitespace and non word characters to avoid malformed url
        String cleanedUserName = userName.replaceAll("\\s+","_");
        cleanedUserName = userName.replaceAll("\\W+","_");
        try {
            chatClient = new ChatClient(this, cleanedUserName);
            chatClient.startClient();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}