import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class ClientRMIGUI extends JFrame implements ActionListener, Serializable {

    private static final long serialVersionUID = 1L;
    private JPanel textPanel, inputPanel;
    private JTextField textField;
    private String name, message, wachtwoord, uniekeCode;
    private Font meiryoFont = new Font("Meiryo", Font.PLAIN, 14);
    private Border blankBorder = BorderFactory.createEmptyBorder(10,10,20,10);//top,r,b,l
    private ChatClient chatClient;
    private JList<String> list;
    private DefaultListModel<String> listModel;
    private TimerThread timerThread;

    protected JTextArea textArea, userArea;
    protected JFrame frame;
    protected JButton privateMsgButton, startButton, sendButton, addFriendButton, pasteButton, acceptFriendButton, addGroupButton;
    protected JPanel clientPanel, userPanel;

    private boolean inFriendAddModus = false;
    private boolean addedGroupMembers = false;
    private boolean keyAsSenderIngegeven = false;
    private boolean acceptUniekeCode = false;
    private boolean acceptWachtwoord = false;
    private boolean addedFriend = false;
    private String friendName = null;
    private byte[] tagAsSender = null;
    private byte[] tagAsReceiver = null;
    private SecretKey keyAsSender = null;
    private SecretKey keyAsReceiver = null;
    private int boxNummerAsSender;
    private int boxNummerAsReceiver;
    private boolean inGroupModus = false;
    private List<String> groupMembers = null;


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

    public int[] getSelectedIndices(){
        return list.getSelectedIndices();
    }

    public String getUserAtIndex(int index){
        return this.listModel.elementAt(index);
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
        //textField.setTransferHandler();
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
        String  userStr = " Contacts      ";

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

        list.addListSelectionListener(this::valueChanged);

        /*
        privateMsgButton = new JButton("Send PM");
        privateMsgButton.addActionListener(this);
        privateMsgButton.setEnabled(false);
         */

        startButton = new JButton("Start ");
        startButton.addActionListener(this);

        addFriendButton = new JButton("Add friend");
        addFriendButton.addActionListener(this);
        addFriendButton.setEnabled(false);

        acceptFriendButton = new JButton("Accept friend");
        acceptFriendButton.addActionListener(this);
        acceptFriendButton.setEnabled(false);

        addGroupButton = new JButton("Add Group");
        addGroupButton.addActionListener(this);
        addGroupButton.setEnabled(false);

        pasteButton = new JButton("Paste");
        pasteButton.addActionListener(this);
        pasteButton.setEnabled(false);

        frame.getRootPane().setDefaultButton(startButton);

        JPanel buttonPanel = new JPanel(new GridLayout(7, 1));
        //buttonPanel.add(privateMsgButton);
        buttonPanel.add(startButton);
        buttonPanel.add(new JLabel(""));
        buttonPanel.add(sendButton);
        buttonPanel.add(addFriendButton);
        buttonPanel.add(acceptFriendButton);
        buttonPanel.add(addGroupButton);
        buttonPanel.add(pasteButton);

        return buttonPanel;
    }


    /**
     * Action handling change of the list
     */
    public void valueChanged(ListSelectionEvent e){
        if(list.getValueIsAdjusting()){
            this.textArea.setText("");
            int[] selectedIndices = this.list.getSelectedIndices();
            int index = -1;

            for(int i=0; i<selectedIndices.length; i++){
                index = selectedIndices[i];
            }

            if(index != -1){
                List<String> messages = chatClient.getListMessagesFrom(listModel.getElementAt(index));
                for(String message: messages){
                    this.textArea.append(message);
                }
            }
        }
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
                    textArea.append("[System]: " + name + " ready to chat but first... SUIT UP!\n");
                    getConnected(name);
                    if(!chatClient.connectionProblem){
                        startButton.setEnabled(false);
                        sendButton.setEnabled(true);
                        addFriendButton.setEnabled(true);
                        pasteButton.setEnabled(true);
                        acceptFriendButton.setEnabled(true);
                        addGroupButton.setEnabled(true);
                        frame.getRootPane().setDefaultButton(sendButton);
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
                if(this.addedFriend && textField.getText().length()!=0){
                    this.friendName = textField.getText();
                    textField.setText("");

                    //Initializing the GUI to the chat of the added friend
                    listModel.addElement(friendName);
                    int index = listModel.indexOf(friendName);
                    list.setSelectedIndex(index);
                    listModel.removeElement("No other users");
                    textArea.setText("");
                    textArea.append("[System]: Haaaaave you met "+friendName+"?\n");
                    textArea.append("\n");
                    textField.setText("");

                    //Setting the attributes to the client
                    chatClient.addFriend(friendName, keyAsSender, keyAsReceiver, boxNummerAsSender, boxNummerAsReceiver, tagAsSender, tagAsReceiver);

                    //Resetting everything out of "adding friends modus"
                    this.boxNummerAsSender = -1;
                    this.boxNummerAsReceiver = -1;
                    this.keyAsReceiver = null;
                    this.keyAsSender = null;
                    this.tagAsSender = null;
                    this.tagAsReceiver = null;
                    this.addedFriend= false;
                    this.acceptWachtwoord = false;
                    this.acceptUniekeCode = false;

                }else if(this.acceptWachtwoord && textField.getText().length()!=0){
                    this.wachtwoord = textField.getText();
                    String appending = wachtwoord + uniekeCode;
                    textField.setText("");

                    boxNummerAsReceiver = Integer.parseInt(this.uniekeCode) % chatClient.serverIF.getGrootteVanBoard();
                    boxNummerAsSender = Integer.parseInt(this.uniekeCode.substring(2,5)) % chatClient.serverIF.getGrootteVanBoard();

                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    byte[] hashedWachtwoord = sha.digest(wachtwoord.getBytes());
                    byte[] hashedAppending = sha.digest(appending.getBytes());

                    tagAsReceiver = Arrays.copyOf(hashedWachtwoord, hashedWachtwoord.length);
                    tagAsSender = Arrays.copyOf(hashedAppending, hashedAppending.length);

                    byte[] saltSender = new byte[16];
                    byte[] saltReceiver = new byte[16];
                    for(int i=0; i<16; i++){
                        saltSender[i] = hashedAppending[i];
                        saltReceiver[i] = hashedAppending[i+10];
                    }

                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), saltSender,65536, 256);
                    SecretKey tmp = factory.generateSecret(spec);
                    this.keyAsReceiver = new SecretKeySpec(tmp.getEncoded(), "AES");

                    spec = new PBEKeySpec(wachtwoord.toCharArray(), saltReceiver,65536, 256);
                    SecretKey tmp2 = factory.generateSecret(spec);
                    this.keyAsSender = new SecretKeySpec(tmp2.getEncoded(), "AES");

                    this.addedFriend = true;

                    textArea.append("[System]: Enter the name of your friend and press SEND.\n");

                }else if(this.acceptUniekeCode && textField.getText().length()!=0){
                    this.uniekeCode = textField.getText();
                    textField.setText("");
                    this.acceptWachtwoord = true;
                    textArea.append("[System]: Please enter the chosen password to initialize friendship and press SEND.\n");

                }else if(this.keyAsSenderIngegeven && textField.getText().length()!=0){
                    friendName = textField.getText();
                    textField.setText("");

                    //Initializing the GUI to the chat of the added friend
                    listModel.addElement(friendName);
                    int index = listModel.indexOf(friendName);
                    list.setSelectedIndex(index);
                    listModel.removeElement("No other users");
                    textArea.setText("");
                    textArea.append("[System]: Haaaaave you met "+friendName+"?\n");
                    textArea.append("\n");
                    textField.setText("");

                    //Setting the attributes to the client
                    chatClient.addFriend(friendName, keyAsSender, keyAsReceiver, boxNummerAsSender, boxNummerAsReceiver, tagAsSender, tagAsReceiver);

                    //Resetting everything out of "adding friends modus"
                    this.boxNummerAsSender = -1;
                    this.boxNummerAsReceiver = -1;
                    this.keyAsReceiver = null;
                    this.keyAsSender = null;
                    this.tagAsSender = null;
                    this.tagAsReceiver = null;
                    this.inFriendAddModus = false;
                    this.keyAsSenderIngegeven = false;

                }else if(this.inFriendAddModus && textField.getText().length()!=0){
                    wachtwoord = textField.getText();
                    String appending = wachtwoord + uniekeCode;
                    textField.setText("");

                    boxNummerAsSender = Integer.parseInt(this.uniekeCode) % chatClient.serverIF.getGrootteVanBoard();
                    boxNummerAsReceiver = Integer.parseInt(this.uniekeCode.substring(2,5)) % chatClient.serverIF.getGrootteVanBoard();

                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    byte[] hashedWachtwoord = sha.digest(wachtwoord.getBytes());
                    byte[] hashedAppending = sha.digest(appending.getBytes());

                    tagAsSender = Arrays.copyOf(hashedWachtwoord, hashedWachtwoord.length);
                    tagAsReceiver = Arrays.copyOf(hashedAppending, hashedAppending.length);

                    byte[] saltSender = new byte[16];
                    byte[] saltReceiver = new byte[16];
                    for(int i=0; i<16; i++){
                        saltSender[i] = hashedAppending[i];
                        saltReceiver[i] = hashedAppending[i+10];
                    }

                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), saltSender,65536, 256);
                    SecretKey tmp = factory.generateSecret(spec);
                    this.keyAsSender = new SecretKeySpec(tmp.getEncoded(), "AES");

                    spec = new PBEKeySpec(wachtwoord.toCharArray(), saltReceiver,65536, 256);
                    SecretKey tmp2 = factory.generateSecret(spec);
                    this.keyAsReceiver = new SecretKeySpec(tmp2.getEncoded(), "AES");

                    textArea.append("[System]: Enter the name of your friend and press SEND.\n");

                    this.keyAsSenderIngegeven = true;
                }else{
                    int[] selectedIndexes = list.getSelectedIndices();
                    int index = -1;

                    for(int i=0; i<selectedIndexes.length; i++){
                        index = selectedIndexes[i];
                    }

                    message = textField.getText();
                    textField.setText("");
                    System.out.print("Sending a message to "+listModel.getElementAt(index));
                    System.out.println("  -  Sended message: " + message);
                    sendMessage(listModel.getElementAt(index), message);
                }
            }

            //send a private message, to selected users
            if(e.getSource() == privateMsgButton){
                int[] privateList = list.getSelectedIndices();

                for(int i=0; i<privateList.length; i++){
                    System.out.println("selected index :" + privateList[i]);
                }
                message = textField.getText();
                textField.setText("");
            }

            if(e.getSource() == addFriendButton){
                if(inGroupModus){
                    String member = textField.getText();
                    textField.setText("");

                    if(chatClient.hasFriend(member)){
                        groupMembers.add(member);
                        textArea.append("[System]: I succesfully added "+member+" to the group!\n");
                    }else{
                        textArea.append("[System]: I didn't found "+member+" in your friendlist. Please try again.\n");
                    }

                }else {
                    this.inFriendAddModus = true;
                    list.clearSelection();
                    textArea.setText(null);
                    uniekeCode = "";

                    for (int i = 0; i < 6; i++) {
                        uniekeCode += (int) (Math.random() * 10);
                    }

                    textArea.append("[System]: This is the (secret) 6 digit code from you and your friend: " + uniekeCode + "\n");
                    textArea.append("[System]: Please enter your chosen password to initialize friendship and press SEND.\n");
                }
            }

            if(e.getSource() == acceptFriendButton){
                list.clearSelection();
                textArea.setText(null);
                this.acceptUniekeCode = true;
                textArea.append("[System]: Please enter the unique 6 digit code and press SEND.\n");
            }

            if(e.getSource() == addGroupButton){
                if(this.addedGroupMembers){
                    String groupname = textField.getText();
                    textField.setText("");

                    chatClient.addGroup(groupname, this.groupMembers);

                    //Initializing the GUI to the chat of the added group
                    listModel.addElement(groupname);
                    int index = listModel.indexOf(groupname);
                    list.setSelectedIndex(index);
                    listModel.removeElement("No other users");
                    textArea.setText("");
                    textArea.append("[System]: Haaaaave you met "+groupname+"?\n");
                    textArea.append("\n");
                    textField.setText("");


                    this.addedGroupMembers = false;
                    this.inGroupModus = false;
                    groupMembers = null;
                }else if(inGroupModus){
                    textArea.append("[System]: "+chatClient.getName()+", you're one step away from adding this group succesfully. Will you just enter the name of the group and press ADD GROUP again?\n");
                    this.addedGroupMembers = true;
                }else{
                    list.clearSelection();
                    textArea.setText(null);
                    this.inGroupModus = true;
                    this.groupMembers = new ArrayList<String>();
                    textArea.append("[System]: Welcome at the group functionality of this chat application, you can add a friend to " +
                            "the group by simply entering his/her name and pressing ADD FRIEND. When you are ready you press ADD GROUP again.\n");
                }
            }

            if(e.getSource() == pasteButton){
                textField.paste();
            }

        }
        catch (RemoteException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidKeySpecException remoteExc) {
            remoteExc.printStackTrace();
        }

    }//end actionPerformed

    // --------------------------------------------------------------------

    /**
     * Send a message, to be relayed to all chatters
     * @param chatMessage
     * @throws RemoteException
     */
    private void sendMessage(String friendname, String chatMessage) throws RemoteException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException {
        chatClient.sendMessageTo(friendname, chatMessage);
    }

    /**
     * Send a message, to be relayed, only to selected chatters
     *
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
        } catch (RemoteException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}