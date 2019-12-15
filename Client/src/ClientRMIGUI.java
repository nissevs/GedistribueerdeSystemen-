import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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

    //Added for menu bar
    private JMenuBar menuBar;
    private JMenu menu, menuLogin;
    private JMenuItem menuItemSave, menuItemOpen, menuItemSaveToServer, menuItemLoginFromServer;

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
    private boolean passwordSave = false;
    private boolean passwordLogin = false;
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

        setJMenuBar();
        frame.setJMenuBar(menuBar);

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



    public void setJMenuBar(){

        menuBar = new JMenuBar();

        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_A);
        menu.getAccessibleContext().setAccessibleDescription("Menu to save or open status local");
        menuLogin = new JMenu("Login");
        menuLogin.setMnemonic(KeyEvent.VK_A);
        menuLogin.getAccessibleContext().setAccessibleDescription("Menu to save or open status from server");
        menuBar.add(menu);
        menuBar.add(menuLogin);

        menuItemSave = new JMenuItem("Save", KeyEvent.VK_T);
        menuItemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.ALT_MASK));
        menuItemSave.getAccessibleContext().setAccessibleDescription("Saves all chats in encrypted file");
        menuItemSave.addActionListener(this);
        menu.add(menuItemSave);

        menuItemOpen = new JMenuItem("Open", KeyEvent.VK_T);
        menuItemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
        menuItemOpen.getAccessibleContext().setAccessibleDescription("Opens all chats in encrypted file");
        menuItemOpen.addActionListener(this);
        menu.add(menuItemOpen);
        menu.setEnabled(false);

        menuItemSaveToServer = new JMenuItem("Save to server", KeyEvent.VK_T);
        menuItemSaveToServer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        menuItemSaveToServer.getAccessibleContext().setAccessibleDescription("Saves all chats on server");
        menuItemSaveToServer.addActionListener(this);
        menuLogin.add(menuItemSaveToServer);

        menuItemLoginFromServer = new JMenuItem("Login from server", KeyEvent.VK_T);
        menuItemLoginFromServer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
        menuItemLoginFromServer.getAccessibleContext().setAccessibleDescription("Logs the user in from the server");
        menuItemLoginFromServer.addActionListener(this);
        menuLogin.add(menuItemLoginFromServer);
        menuLogin.setEnabled(false);
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
                        menu.setEnabled(true);
                        menuLogin.setEnabled(true);
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
                if(passwordLogin && textField.getText().length()!=0){
                    String pass = textField.getText();
                    textField.setText("");

                    int boxNr = (pass.length()*27 + this.name.length()*6) % chatClient.serverIF.getGrootteVanBoard();
                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    String ongehashteTag = pass+"_"+boxNr+"_"+this.name;
                    byte[] tag = sha.digest(ongehashteTag.getBytes());

                    byte[] salt= new byte[16];
                    for(int i=0; i<16; i++){
                        salt[i] = tag[i+2];
                    }

                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec spec = new PBEKeySpec(pass.toCharArray(), salt,65536, 256);
                    SecretKey tmp = factory.generateSecret(spec);
                    SecretKey key = new SecretKeySpec(tmp.getEncoded(), "AES");

                    byte[] encryptedFile = this.chatClient.serverIF.getMessageFromBoard(boxNr, tag);

                    Cipher c = Cipher.getInstance("AES");
                    c.init(Cipher.DECRYPT_MODE, key);

                    byte[] encodedFile = c.doFinal(encryptedFile);

                    File file2 = new File("temp.txt");
                    OutputStream os = new FileOutputStream(file2);
                    os.write(encodedFile);
                    os.close();

                    this.chatClient.importFile(file2);

                    Files.delete(file2.toPath());

                    this.textArea.setText("[System]: I succesfully add all your chats to the this client!");
                    this.passwordLogin = false;
                }else if(passwordSave && textField.getText().length()!=0){
                    String pass = textField.getText();
                    textField.setText("");

                    int boxNr = (pass.length()*27 + this.name.length()*6) % chatClient.serverIF.getGrootteVanBoard();
                    MessageDigest sha = MessageDigest.getInstance("SHA-256");
                    String ongehashteTag = pass+"_"+boxNr+"_"+this.name;
                    byte[] tag = sha.digest(ongehashteTag.getBytes());

                    byte[] salt= new byte[16];
                    for(int i=0; i<16; i++){
                        salt[i] = tag[i+2];
                    }

                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec spec = new PBEKeySpec(pass.toCharArray(), salt,65536, 256);
                    SecretKey tmp = factory.generateSecret(spec);
                    SecretKey key = new SecretKeySpec(tmp.getEncoded(), "AES");

                    File file = new File("temp2.txt");
                    this.chatClient.exportFile(file);
                    FileInputStream fis = new FileInputStream(file);
                    byte[] encodedFile = new byte[(int) file.length()];
                    fis.read(encodedFile);

                    Cipher c = Cipher.getInstance("AES");
                    c.init(Cipher.ENCRYPT_MODE, key);

                    byte[] encryptedFile = c.doFinal(encodedFile);
                    chatClient.serverIF.sendMessageIntoBoard(boxNr, encryptedFile, sha.digest(tag));

                    Files.delete(file.toPath());

                    this.textArea.setText("[System]: I succesfully saved your chats to the server!");
                    this.passwordSave = false;
                }else if(this.addedFriend && textField.getText().length()!=0){
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
                    if(!chatClient.isGroup(listModel.getElementAt(index))){
                        sendMessage(listModel.getElementAt(index), message);
                    }else{
                        System.out.println("Trying to send something into a group!");
                        List<String> users = chatClient.getUsersFromGroup(listModel.getElementAt(index));
                        sendMessageToGroup(listModel.getElementAt(index), users, message);
                    }
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
                    initializeGUIForGroup(groupname, chatClient.getUsersFromGroup(groupname));


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

            if(e.getSource() == menuItemSave){
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] hashedName = sha.digest(this.name.getBytes());

                byte[] salt = new byte[16];
                for(int i=0; i<16; i++){
                    salt[i] = hashedName[i+2];
                }

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec spec = new PBEKeySpec(name.toCharArray(), salt,65536, 256);
                SecretKey tmp = factory.generateSecret(spec);
                SecretKey fileKey = new SecretKeySpec(tmp.getEncoded(), "AES");

                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.ENCRYPT_MODE, fileKey);

                JFileChooser fileChooser = new JFileChooser();
                int retval = fileChooser.showSaveDialog(menuItemSave);
                if (retval == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    if (file == null) {
                        return;
                    }
                    if (!file.getName().toLowerCase().endsWith(".txt")) {
                        file = new File(file.getParentFile(), file.getName() + ".txt");
                    }
                    try {

                        chatClient.exportFile(file);

                        FileInputStream inputStream = new FileInputStream(file);
                        byte[] inputBytes = new byte[(int) file.length()];
                        inputStream.read(inputBytes);

                        byte[] outputBytes = c.doFinal(inputBytes);

                        FileOutputStream outputStream = new FileOutputStream(file);
                        outputStream.write(outputBytes);

                        inputStream.close();
                        outputStream.close();

                    } catch (Exception er) {
                        er.printStackTrace();
                    }
                }
            }

            if(e.getSource() == menuItemOpen) {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] hashedName = sha.digest(this.name.getBytes());

                byte[] salt = new byte[16];
                for(int i=0; i<16; i++){
                    salt[i] = hashedName[i+2];
                }

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec spec = new PBEKeySpec(name.toCharArray(), salt,65536, 256);
                SecretKey tmp = factory.generateSecret(spec);
                SecretKey fileKey = new SecretKeySpec(tmp.getEncoded(), "AES");

                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, fileKey);

                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(menuItemOpen);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();

                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] inputBytes = new byte[(int) file.length()];
                    inputStream.read(inputBytes);

                    byte[] outputBytes = c.doFinal(inputBytes);

                    FileOutputStream outputStream = new FileOutputStream(file);
                    outputStream.write(outputBytes);

                    chatClient.importFile(file);

                    inputStream.close();
                    outputStream.close();
                    Files.delete(file.toPath());

                }

            }

            if(e.getSource() == menuItemSaveToServer){
                list.clearSelection();
                textArea.setText("");
                textArea.append("[System]: You are about to save all these chats (on a safe way) to the server. Please enter a safe password and press SEND.\n");
                this.passwordSave = true;
            }

            if(e.getSource() == menuItemLoginFromServer){
                list.clearSelection();
                textArea.setText("");
                textArea.append("[System]: You are about to log into the server. Please enter your safe password and press SEND.\n");
                this.passwordLogin = true;
            }

        }
        catch (RemoteException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException | InvalidKeySpecException | FileNotFoundException remoteExc) {
            remoteExc.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NoSuchPaddingException ex) {
            ex.printStackTrace();
        }

    }//end actionPerformed

    // --------------------------------------------------------------------

    public void addUserToList(String user){
        listModel.removeElement("No other users");
        listModel.addElement(user);
    }

    public void initializeGUIForGroup(String groupname, List<String> users){
        //Initializing the GUI to the chat of the added friend
        listModel.addElement(groupname);
        int index = listModel.indexOf(groupname);
        list.setSelectedIndex(index);
        listModel.removeElement("No other users");
        textArea.setText("");
        textArea.append("[System]: Legen... wait for it!\n");
        textArea.append("[System]: Awesome because you are in a chatgroup with ");

        String enumerationOfUsers = "";

        for(int i=0; i<users.size(); i++){
            if(!users.get(i).equals(this.name)){
                enumerationOfUsers += users.get(i);
                if(users.indexOf(name) == users.size()-1){
                    if(i!=users.size()-2) enumerationOfUsers += ", ";
                } else if(i != users.size()-1){
                    enumerationOfUsers += ", ";
                }
            }
        }

        String hulp = enumerationOfUsers;
        if(users.size()>2){
            int indexOfLastComma = enumerationOfUsers.lastIndexOf(",");
            hulp = enumerationOfUsers.substring(0,indexOfLastComma) + " and" + enumerationOfUsers.substring(indexOfLastComma+1);
        }

        textArea.append(hulp+"\n");
        textArea.append("[System]: ... dary!\n");
        textArea.append("\n");
        textField.setText("");
    }

    /**
     * Send a message, to be relayed to all chatters
     * @param chatMessage
     * @throws RemoteException
     */
    private void sendMessage(String friendname, String chatMessage) throws RemoteException, BadPaddingException, InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException {
        chatClient.sendMessageTo(friendname, chatMessage);
    }

    private void sendMessageToGroup(String groupname, List<String> users, String chatMessage) throws RemoteException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException {
        long millis = -1;

        for(String user: users){
            millis = chatClient.sendMessageWithoutPrinting(user+"_"+groupname, chatMessage);
        }

        String uur =  String.format("%02d", new Date(millis).getHours());
        String minuten = String.format("%02d", new Date(millis).getMinutes());

        textArea.append(uur+":"+minuten+" - "+"[You]: "+message+"\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());

        chatClient.addMessageToHistory(groupname, uur+":"+minuten+" - "+"[You]: "+message+"\n");

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