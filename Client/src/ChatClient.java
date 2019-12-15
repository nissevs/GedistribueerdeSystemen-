import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;

public class ChatClient extends UnicastRemoteObject implements ClientIF {

    ClientRMIGUI chatGUI;
    private String name;
    private List<String> groups;
    private Map<String, List<String>> groupMembersList;
    private Map<String, StoreAttribute> storeAsSender;
    private Map<String, StoreAttribute> storeAsReceiver;
    private Map<String, List<String>> messageHistory;
    private Cipher c;

    //public ClientIF user;
    protected ServerIF serverIF;
    protected boolean connectionProblem;


    @Override
    public String getName() {
        return this.name;
    }
    public List<String> getListMessagesFrom(String friendname){ return this.messageHistory.get(friendname); }

    public ChatClient(ClientRMIGUI chatGUI,String name) throws RemoteException, NoSuchPaddingException, NoSuchAlgorithmException {
        super();
        this.name=name;
        //this.user=chatClient;
        this.chatGUI=chatGUI;
        this.storeAsSender = new HashMap<String, StoreAttribute>();
        this.storeAsReceiver = new HashMap<String, StoreAttribute>();
        this.messageHistory = new HashMap<String, List<String>>();
        this.groupMembersList = new HashMap<String, List<String>>();
        this.groups = new ArrayList<String>();
        c = Cipher.getInstance("AES");
    }


    public void addFriend(String name, SecretKey keyAsSender, SecretKey keyAsReceiver, int boxNummerAsSender, int boxNummerAsReceiver, byte[] tagAsSender, byte[] tagAsReceiver){
        StoreAttribute senderAttributes = new StoreAttribute(keyAsSender, boxNummerAsSender, tagAsSender);
        StoreAttribute receiverAttributes = new StoreAttribute(keyAsReceiver, boxNummerAsReceiver, tagAsReceiver);
        this.storeAsSender.put(name, senderAttributes);
        this.storeAsReceiver.put(name, receiverAttributes);
        this.messageHistory.put(name, new ArrayList<String>());
        System.out.println("Succesfully added friend "+name);
    }

    public void addGroup(String groupname, List<String> groupMembers) throws RemoteException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        this.groups.add(groupname);
        this.groupMembersList.put(groupname, new ArrayList<String>());
        groupMembers.add(this.name);

        Map<String, Map<String, String>> initializingUsers = new HashMap<String, Map<String, String>>();

        for(String receiver: groupMembers){
            initializingUsers.put(receiver, new HashMap<String, String>());
        }

        //Sending special message to everyone you chose
        for(String receiver: groupMembers){

            for(int i=0; i<groupMembers.size(); i++) {

                if (!initializingUsers.get(receiver).containsKey(groupMembers.get(i)) && !groupMembers.get(i).equals(receiver)) {
                    System.out.println("Receiver: "+receiver);

                    String appendedMessage1 = "";
                    String appendedMessage2 = "";

                    //Begin client info with name
                    appendedMessage2 += groupMembers.get(i) + "|";
                    appendedMessage1 += receiver + "|";

                    //First key -> this one is the sender
                    KeyGenerator kg = KeyGenerator.getInstance("AES");
                    SecretKey symmetricKey1 = kg.generateKey();
                    String newKeyToString1 = Base64.getEncoder().encodeToString(symmetricKey1.getEncoded());
                    appendedMessage1 += newKeyToString1 + "|";

                    //Second key -> this one is the receiver
                    SecretKey symmetricKey2 = kg.generateKey();
                    String newKeyToString2 = Base64.getEncoder().encodeToString(symmetricKey2.getEncoded());
                    appendedMessage1 += newKeyToString2 + "|";

                    appendedMessage2 += newKeyToString2 + "|";
                    appendedMessage2 += newKeyToString1 + "|";

                    //Create random tags
                    String tag1 = "";
                    String tag2 = "";
                    for (int j = 0; j < 32; j++) {
                        tag1 += generateRandomChar();
                        tag2 += generateRandomChar();
                    }
                    byte[] newTag1 = tag1.getBytes();
                    byte[] newTag2 = tag2.getBytes();
                    appendedMessage1 += tag1 + "|";
                    appendedMessage1 += tag2 + "|";
                    appendedMessage2 += tag2 + "|";
                    appendedMessage2 += tag1 + "|";

                    //Generate random box numbers
                    int newBoxNr1 = (int) (Math.random() * this.serverIF.getGrootteVanBoard());
                    int newBoxNr2 = (int) (Math.random() * this.serverIF.getGrootteVanBoard());
                    appendedMessage1 += newBoxNr1 + "|";
                    appendedMessage1 += newBoxNr2;

                    appendedMessage2 += newBoxNr2 + "|";
                    appendedMessage2 += newBoxNr1;

                    if(groupMembers.get(i).equals(this.name)){
                        //Create StoreAttribute objects
                        StoreAttribute senderAttributes = new StoreAttribute(symmetricKey1, newBoxNr1, newTag1);
                        StoreAttribute receiverAttributes = new StoreAttribute(symmetricKey2, newBoxNr2, newTag2);

                        System.out.println("Coordinator: add to list: "+groupMembers.get(i)+"_"+groupname);

                        System.out.println();
                        System.out.println("-----------------------------");
                        System.out.println("    User: "+receiver);
                        System.out.println("    SenderKey to this user: "+newKeyToString1);
                        System.out.println("    ReceiverKey for this user: "+newKeyToString2);
                        System.out.println("    SenderTag to this user: "+tag1);
                        System.out.println("    ReceiverKey to this user: "+tag2);
                        System.out.println("    Boxnumber used to send to this user: "+newBoxNr1);
                        System.out.println("    Boxnumber user to receive from this user: "+newBoxNr2);
                        System.out.println("-----------------------------");
                        System.out.println("");

                        this.storeAsSender.put(receiver + "_" + groupname, senderAttributes);
                        this.storeAsReceiver.put(receiver + "_" + groupname, receiverAttributes);

                        this.messageHistory.put(receiver+"_"+groupname, new ArrayList<String>());
                    }

                    initializingUsers.get(receiver).put(groupMembers.get(i),appendedMessage2);
                    initializingUsers.get(groupMembers.get(i)).put(receiver,appendedMessage1);

                    System.out.println("Message from " + receiver + " to "+groupMembers.get(i)+": " + appendedMessage1);
                    System.out.println("Message from " + groupMembers.get(i) + " to "+receiver+": " + appendedMessage2);

                }
            }
        }

        for(String receiver: groupMembers){

            if(!receiver.equals(this.name)) {
                StoreAttribute sender = this.storeAsSender.get(receiver);
                SecretKey key = sender.getKey();
                String keyToString = Base64.getEncoder().encodeToString(key.getEncoded());
                String appendedMessage = keyToString + "]GROUP[" +groupname +"|" + groupMembers.size() + "]BEGIN_DATA_INPUT[";
                int length = 0;

                for(String receiver2 :initializingUsers.get(receiver).keySet()){
                    appendedMessage += initializingUsers.get(receiver).get(receiver2);

                    if(length != initializingUsers.get(receiver).keySet().size() - 1){
                        appendedMessage += "]NEXT_USER[";
                    }
                    length++;
                }

                groupMembersList.get(groupname).add(receiver);
                this.sendMessageTo(receiver, appendedMessage);

            }

        }
        this.messageHistory.put(groupname, new ArrayList<String>());
    }

    private static char generateRandomChar () {
        int rnd = (int) (Math.random() * 52); // or use Random or whatever
        char base = (rnd < 26) ? 'A' : 'a';
        return (char) (base + rnd % 26);

    }

    public List<String> getUsersFromGroup(String groupname){
        this.groupMembersList.remove(this.name);
        return this.groupMembersList.get(groupname);
    }

    public boolean isGroup(String groupname){
        return this.groups.contains(groupname);
    }

    public boolean hasFriend(String friendname){
        return this.messageHistory.containsKey(friendname);
    }

    public String getTeVersturenFormaat(String friendname, String message) throws RemoteException {

        String tag = "";
        for(int i=0; i<32; i++){
            tag+=generateRandomChar();
        }
        byte[] newTag = tag.getBytes();

        int newBoxNr = (int) (Math.random()*this.serverIF.getGrootteVanBoard());
        String numberAsPaddedString = String.format("%02d", newBoxNr);

        StoreAttribute attributes = this.storeAsSender.get(friendname);
        attributes.setBoxNummer(newBoxNr);
        attributes.setTag(newTag);

        String oplossing = new String(newTag);
        oplossing += numberAsPaddedString;
        oplossing += message;

        return oplossing;
    }


    public void getMessagesFrom(String friendname) throws RemoteException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(!this.isGroup(friendname)){
            StoreAttribute receiver = this.storeAsReceiver.get(friendname);

            byte[] thisMessage = serverIF.getMessageFromBoard(receiver.getBoxNummer(), receiver.getTag());
            while (thisMessage != null) {
                if (thisMessage.length != 0) {
                    c.init(Cipher.DECRYPT_MODE, receiver.getKey());
                    byte[] messageArray = c.doFinal(thisMessage);
                    String newMessage = new String(messageArray);

                    byte[] tag = newMessage.substring(0, 32).getBytes();
                    int boxNr = Integer.parseInt(newMessage.substring(32, 34));
                    String message = newMessage.substring(34);
                    long timestamp = Long.parseLong(message.substring(0, 20));
                    String uur = String.format("%02d", new Date(timestamp).getHours());
                    String minuten = String.format("%02d", new Date(timestamp).getMinutes());
                    message = message.substring(20);

                    receiver.setTag(tag);
                    receiver.setBoxNummer(boxNr);

                    SecretKey key = receiver.getKey();
                    String keyToString = Base64.getEncoder().encodeToString(key.getEncoded());

                    //Key derivation function
                    String wachtwoord = Base64.getEncoder().encodeToString(receiver.getKey().getEncoded());
                    char[] chars = wachtwoord.toCharArray();
                    byte[] salt = new byte[16];
                    for (int i = 0; i < 16; i++) {
                        salt[i] = receiver.getTag()[i * 2];
                    }
                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), salt, 65536, 256);
                    SecretKey tmp = factory.generateSecret(spec);
                    SecretKey newKey = new SecretKeySpec(tmp.getEncoded(), "AES");
                    receiver.setKey(newKey);


                    if (message.contains(keyToString + "]GROUP[")) {

                        //chatGUI.textArea.append("Group chat request detected!\n");
                        //chatGUI.textArea.append(message);

                        String[] firstSplit = message.split("]GROUP\\[");
                        String[] secondSplit = firstSplit[1].split("]BEGIN_DATA_INPUT\\[");
                        String[] thirdSplit = secondSplit[0].split("\\|");

                        String groupName = thirdSplit[0];
                        int amountOfUsers = Integer.parseInt(thirdSplit[1]);
                        this.messageHistory.put(groupName, new ArrayList<String>());

                        String[] fourthSplit = secondSplit[1].split("]NEXT_USER\\[");
                        List<String> users = new ArrayList<String>();

                        System.out.println();
                        System.out.println("----------------------------------------");
                        System.out.println("               GROUPCHAT");
                        System.out.println("----------------------------------------");
                        System.out.println("    Group: "+groupName);
                        System.out.println("    Amount of users: "+amountOfUsers);
                        System.out.println("    On client of: "+this.name);

                        for (int i = 0; i < fourthSplit.length; i++) {
                            System.out.println();
                            String appendedMessage = fourthSplit[i];
                            String[] parts = appendedMessage.split("\\|");

                            String userName = parts[0];
                            String keySencderInString = parts[1];
                            String keyReceiverInString = parts[2];
                            String tagSenderInString = parts[3];
                            String tagReceiverInString = parts[4];
                            int boxNrSender = Integer.parseInt(parts[5]);
                            int boxNrReceiver = Integer.parseInt(parts[6]);

                            //Debug reseasons
                            System.out.println("    User: "+userName);
                            System.out.println("    SenderKey to this user: "+keySencderInString);
                            System.out.println("    ReceiverKey for this user: "+keyReceiverInString);
                            System.out.println("    SenderTag to this user: "+tagSenderInString);
                            System.out.println("    ReceiverKey to this user: "+tagReceiverInString);
                            System.out.println("    Boxnumber used to send to this user: "+boxNrSender);
                            System.out.println("    Boxnumber user to receive from this user: "+boxNrReceiver);

                            users.add(userName);

                            byte[] decodedKeySender = Base64.getDecoder().decode(keySencderInString);
                            byte[] decodedKeyReceiver = Base64.getDecoder().decode(keyReceiverInString);
                            SecretKey keySender = new SecretKeySpec(decodedKeySender, 0, decodedKeySender.length, "AES");
                            SecretKey keyReceiver = new SecretKeySpec(decodedKeyReceiver, 0, decodedKeyReceiver.length, "AES");

                            byte[] tagSender = tagSenderInString.getBytes();
                            byte[] tagReceiver = tagReceiverInString.getBytes();

                            //Create StoreAttribute objects
                            StoreAttribute senderAttributes = new StoreAttribute(keySender, boxNrSender, tagSender);
                            StoreAttribute receiverAttributes = new StoreAttribute(keyReceiver, boxNrReceiver, tagReceiver);

                            System.out.println(userName+"_"+groupName);
                            this.storeAsSender.put(userName + "_" + groupName, senderAttributes);
                            this.storeAsReceiver.put(userName + "_" + groupName, receiverAttributes);

                            this.messageHistory.put(userName+"_"+groupName, new ArrayList<String>());

                        }

                        System.out.println("----------------------------------------");

                        this.groups.add(groupName);
                        users.remove(this.name);
                        this.groupMembersList.put(groupName, users);
                        chatGUI.initializeGUIForGroup(groupName, users);

                    } else {
                        chatGUI.textArea.append(uur + ":" + minuten + " - " + "[" + friendname + "]: " + message + "\n");
                        chatGUI.textArea.setCaretPosition(chatGUI.textArea.getDocument().getLength());
                        this.messageHistory.get(friendname).add(uur + ":" + minuten + " - " + "[" + friendname + "]: " + message + "\n");
                    }
                }

                thisMessage = serverIF.getMessageFromBoard(receiver.getBoxNummer(), receiver.getTag());

            }

        }else{

            List<String> users = this.getUsersFromGroup(friendname);
            users.remove(this.name);
            List<String> allMessages = new ArrayList<>();

            for(String user: users){
                allMessages.addAll(this.getListMessagesFromSender(user, friendname));
            }

            Collections.sort(allMessages);

            for(String message: allMessages){
                chatGUI.textArea.append(message.substring(20));
                chatGUI.textArea.setCaretPosition(chatGUI.textArea.getDocument().getLength());
                this.messageHistory.get(friendname).add(message.substring(20));
            }

        }
    }


    public List<String> getListMessagesFromSender(String friendname, String groupname) throws RemoteException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException, InvalidKeyException {

        List<String> messages = new ArrayList<String>();

        if (!this.isGroup(friendname)) {
            System.out.println("Trying to get data from "+friendname+"_"+groupname);
            StoreAttribute receiver = this.storeAsReceiver.get(friendname+"_"+groupname);

            byte[] thisMessage = serverIF.getMessageFromBoard(receiver.getBoxNummer(), receiver.getTag());
            while (thisMessage != null) {
                if (thisMessage.length != 0) {
                    c.init(Cipher.DECRYPT_MODE, receiver.getKey());
                    byte[] messageArray = c.doFinal(thisMessage);
                    String newMessage = new String(messageArray);

                    byte[] tag = newMessage.substring(0, 32).getBytes();
                    int boxNr = Integer.parseInt(newMessage.substring(32, 34));
                    String message = newMessage.substring(34);
                    //messages.add(message);
                    long timestamp = Long.parseLong(message.substring(0, 20));
                    String uur = String.format("%02d", new Date(timestamp).getHours());
                    String minuten = String.format("%02d", new Date(timestamp).getMinutes());
                    message = message.substring(20);
                    messages.add(String.format("%020d", timestamp)+uur+":"+minuten+" - "+"["+friendname+"]: "+message+"\n");

                    receiver.setTag(tag);
                    receiver.setBoxNummer(boxNr);

                    SecretKey key = receiver.getKey();
                    String keyToString = Base64.getEncoder().encodeToString(key.getEncoded());

                    //Key derivation function
                    String wachtwoord = Base64.getEncoder().encodeToString(receiver.getKey().getEncoded());
                    char[] chars = wachtwoord.toCharArray();
                    byte[] salt = new byte[16];
                    for (int i = 0; i < 16; i++) {
                        salt[i] = receiver.getTag()[i * 2];
                    }
                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    KeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), salt, 65536, 256);
                    SecretKey tmp = factory.generateSecret(spec);
                    SecretKey newKey = new SecretKeySpec(tmp.getEncoded(), "AES");
                    receiver.setKey(newKey);
                }

                thisMessage = serverIF.getMessageFromBoard(receiver.getBoxNummer(), receiver.getTag());
            }
        }

        return messages;
    }


    public void sendMessageTo(String friendname, String message) throws RemoteException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(message.length()!=0 && !friendname.equals(name)){
            StoreAttribute sender = this.storeAsSender.get(friendname);
            long millis = new Date().getTime();
            String timeAsPaddedString = String.format("%020d", millis);
            String uur =  String.format("%02d", new Date(millis).getHours());
            String minuten = String.format("%02d", new Date(millis).getMinutes());
            String oldMessage = message;
            message = timeAsPaddedString+message;

            int boxNr = sender.getBoxNummer();
            byte[] tag = Arrays.copyOf(sender.getTag(), sender.getTag().length);
            String newMessage = this.getTeVersturenFormaat(friendname, message);

            c.init(Cipher.ENCRYPT_MODE, sender.getKey());
            byte[] hashedMessage = c.doFinal(newMessage.getBytes());

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashedTag = sha.digest(tag);

            //Key derivation function
            String wachtwoord = Base64.getEncoder().encodeToString(sender.getKey().getEncoded());
            char[] chars = wachtwoord.toCharArray();
            byte[] salt = new byte[16];
            for(int i=0; i<16; i++){
                salt[i]=sender.getTag()[i*2];
            }
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), salt,65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey newKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            sender.setKey(newKey);

            serverIF.sendMessageIntoBoard(boxNr, hashedMessage ,hashedTag);
            if(!oldMessage.contains("]BEGIN_DATA_INPUT[") && !oldMessage.contains("]BEGIN_DATA_INPUT[")){
                chatGUI.textArea.append(uur+":"+minuten+" - "+"[You]: "+oldMessage+"\n");
                chatGUI.textArea.setCaretPosition(chatGUI.textArea.getDocument().getLength());

                String[] groupArray = friendname.split("_");
                if(groupArray.length>1){
                    if(this.isGroup(groupArray[groupArray.length-1])){
                        this.messageHistory.get(groupArray[groupArray.length-1]).add(uur+":"+minuten+" - "+"[You]: "+oldMessage+"\n");
                    }else this.messageHistory.get(friendname).add(uur+":"+minuten+" - "+"[You]: "+oldMessage+"\n");
                }else this.messageHistory.get(friendname).add(uur+":"+minuten+" - "+"[You]: "+oldMessage+"\n");
            }
        }

    }

    public long sendMessageWithoutPrinting (String friendname, String message) throws RemoteException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(message.length()!=0 && !friendname.equals(name)){
            StoreAttribute sender = this.storeAsSender.get(friendname);
            long millis = new Date().getTime();
            String timeAsPaddedString = String.format("%020d", millis);
            String uur =  String.format("%02d", new Date(millis).getHours());
            String minuten = String.format("%02d", new Date(millis).getMinutes());
            String oldMessage = message;
            message = timeAsPaddedString+message;

            int boxNr = sender.getBoxNummer();
            byte[] tag = Arrays.copyOf(sender.getTag(), sender.getTag().length);
            String newMessage = this.getTeVersturenFormaat(friendname, message);

            c.init(Cipher.ENCRYPT_MODE, sender.getKey());
            byte[] hashedMessage = c.doFinal(newMessage.getBytes());

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashedTag = sha.digest(tag);

            //Key derivation function
            String wachtwoord = Base64.getEncoder().encodeToString(sender.getKey().getEncoded());
            char[] chars = wachtwoord.toCharArray();
            byte[] salt = new byte[16];
            for(int i=0; i<16; i++){
                salt[i]=sender.getTag()[i*2];
            }
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), salt,65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey newKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            sender.setKey(newKey);

            serverIF.sendMessageIntoBoard(boxNr, hashedMessage ,hashedTag);

            return millis;
        }

        return -1;

    }

    public void addMessageToHistory(String friendname, String message){
        this.messageHistory.get(friendname).add(message);
    }

    public void startClient()throws RemoteException{
        String [] details= {name, "localhost", "GroupChatService"};
        try {
            //Naming.rebind("rmi://192.168.0.119/GroupChatService", this);
            serverIF= (ServerIF) Naming.lookup("rmi://localhost/groupChat");
            System.out.println("Challenge accepted");
        } catch (RemoteException e) {
            System.out.println("remote");
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.out.println("url");
            connectionProblem = true;
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("not bound");
            e.printStackTrace();
        }

        System.out.println("Client Listen RMI Server is running...\n");

    }

    public void exportFile(File file) throws IOException {

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        //Groups of the user
        writer.append("# groups\n");
        for(String group : groups){
            writer.append(group+"\n");
        }

        writer.append("# group_members\n");
        for(String group: this.groupMembersList.keySet()){
            writer.append("% "+group+"\n");
            for(String member: this.groupMembersList.get(group)){
                writer.append(member+"\n");
            }
        }

        writer.append("# storeAsSender\n");
        for(String friend: this.storeAsSender.keySet()){
            writer.append("% "+friend+"\n");
            writer.append(this.storeAsSender.get(friend).toString()+"\n");
        }

        writer.append("# storeAsReceiver\n");
        for(String friend: this.storeAsReceiver.keySet()){
            writer.append("% "+friend+"\n");
            writer.append(this.storeAsReceiver.get(friend).toString()+"\n");
        }

        writer.append("# messageHistory\n");
        for(String friend: this.messageHistory.keySet()){
            writer.append("% "+friend+"\n");
            for(String message: this.messageHistory.get(friend)){
                writer.append(message);
            }
        }

        writer.close();
    }

    public void importFile(File file) throws FileNotFoundException {

        try(Scanner sc = new Scanner(file)){

            boolean stop = false;
            String line = sc.nextLine();
            String inputLine[] = line.split(" ");
            String attributen = inputLine[1];
            String friend = null;
            String friend2 = null;
            String friend3 = null;
            String groupname = null;
            line = sc.nextLine();

            for(int i=0; i<5; i++){

                while(!stop && line.indexOf('#')==-1){

                    System.out.println(line);

                    switch(attributen){

                        case("groups"):
                            this.groups.add(line);
                            System.out.println("Added group: "+line);
                            chatGUI.addUserToList(line);
                            break;
                        case("group_members"):
                            if(line.indexOf('%') == -1){
                                this.groupMembersList.get(groupname).add(line);
                                System.out.println("Added member "+line+" to group "+groupname);
                            }else{
                                String[] split = line.split(" ");
                                List<String> members = new ArrayList<String>();
                                groupname = split[1];
                                this.groupMembersList.put(groupname, members);
                            }
                            break;
                        case("storeAsSender"):
                            if(line.indexOf('%') == -1){
                                this.storeAsSender.put(friend, new StoreAttribute(line));
                                System.out.println("Store as sender - friend "+friend+":"+line);
                                String[] split = friend.split("_");
                                if(split.length<1 || !this.isGroup(split[split.length-1])) chatGUI.addUserToList(friend);
                            }else{
                                String[] split = line.split(" ");
                                friend = split[1];
                            }
                            break;
                        case("storeAsReceiver"):
                            if(line.indexOf('%') == -1){
                                this.storeAsReceiver.put(friend2, new StoreAttribute(line));
                                System.out.println("Store as receiver - friend "+friend2+":"+line);
                            }else{
                                String[] split = line.split(" ");
                                friend2 = split[1];
                            }
                            break;
                        case("messageHistory"):
                            if(line.indexOf('%') == -1){
                                this.messageHistory.get(friend3).add(line+"\n");
                                System.out.println("Added message "+ line+" to history of friend "+friend3);
                            }else{
                                String[] split = line.split(" ");
                                List<String> messages = new ArrayList<String>();
                                friend3 = split[1];
                                this.messageHistory.put(friend3, messages);
                                System.out.println("Added friend "+friend3+" to message history");
                            }
                            break;
                    }

                    if(sc.hasNextLine()) line = sc.nextLine();
                    else stop = true;
                }

                if(i!=4){
                    System.out.println(inputLine);
                    inputLine = line.split(" ");
                    attributen = inputLine[1];
                    if(sc.hasNextLine()) line = sc.nextLine();
                    else stop = true;
                }
            }

        }

    }

}
