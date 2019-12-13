import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
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

        //Sending special message to everyone you chose
        for(String receiver: groupMembers){
            StoreAttribute sender = this.storeAsSender.get(receiver);
            SecretKey key = sender.getKey();
            String keyToString = Base64.getEncoder().encodeToString(key.getEncoded());
            String appendedMessage = keyToString+"]GROUP["+groupMembers.size()+"]BEGIN_DATA_INPUT[";

            for(int i=0; i<groupMembers.size(); i++){

                //First key -> this one is the sender
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                SecretKey symmetricKey1 = kg.generateKey();
                String newKeyToString1 = Base64.getEncoder().encodeToString(symmetricKey1.getEncoded());
                appendedMessage += newKeyToString1 + "|";

                //Second key -> this one is the receiver
                SecretKey symmetricKey2 = kg.generateKey();
                String newKeyToString2 = Base64.getEncoder().encodeToString(symmetricKey2.getEncoded());
                appendedMessage += newKeyToString2 + "|";

                //Create random tags
                String tag1 = "";
                String tag2 = "";
                for(int j=0; j<32; i++){
                    tag1+=generateRandomChar();
                    tag2+=generateRandomChar();
                }
                byte[] newTag1 = tag1.getBytes();
                byte[] newTag2 = tag2.getBytes();
                appendedMessage += tag1 +"|";
                appendedMessage += tag2 +"|";

                //Generate random box numbers
                int newBoxNr1 = (int) (Math.random()*this.serverIF.getGrootteVanBoard());
                int newBoxNr2 = (int) (Math.random()*this.serverIF.getGrootteVanBoard());
                appendedMessage += newBoxNr1 + "|";
                appendedMessage += newBoxNr2;

                //Create StoreAttribute objects
                StoreAttribute senderAttributes = new StoreAttribute(symmetricKey1, newBoxNr1, newTag1);
                StoreAttribute receiverAttributes = new StoreAttribute(symmetricKey2, newBoxNr2, newTag2);

                this.storeAsSender.put(receiver+"_"+groupname, senderAttributes);
                this.storeAsReceiver.put(receiver+"_"+groupname, receiverAttributes);

                if(i != groupMembers.size()-1){
                    appendedMessage += "]NEXT_USER[";
                }

                System.out.println("Message to "+receiver+": "+appendedMessage);
            }

            this.messageHistory.put(groupname, new ArrayList<String>());
            //this.sendMessageTo(receiver, appendedMessage);
        }

        System.out.println("DONE!");

    }

    private static char generateRandomChar () {
        int rnd = (int) (Math.random() * 52); // or use Random or whatever
        char base = (rnd < 26) ? 'A' : 'a';
        return (char) (base + rnd % 26);

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
        StoreAttribute receiver = this.storeAsReceiver.get(friendname);

        byte[] thisMessage = serverIF.getMessageFromBoard(receiver.getBoxNummer(), receiver.getTag());
        while (thisMessage != null) {
            if (thisMessage.length != 0){
                c.init(Cipher.DECRYPT_MODE, receiver.getKey());
                byte[] messageArray = c.doFinal(thisMessage);
                String newMessage = new String(messageArray);

                byte[] tag = newMessage.substring(0, 32).getBytes();
                int boxNr = Integer.parseInt(newMessage.substring(32, 34));
                String message = newMessage.substring(34);
                long timestamp = Long.parseLong(message.substring(0,20));
                String uur =  String.format("%02d", new Date(timestamp).getHours());
                String minuten = String.format("%02d", new Date(timestamp).getMinutes());
                message = message.substring(20);

                receiver.setTag(tag);
                receiver.setBoxNummer(boxNr);

                //Key derivation function
                String wachtwoord = Base64.getEncoder().encodeToString(receiver.getKey().getEncoded());
                char[] chars = wachtwoord.toCharArray();
                byte[] salt = new byte[16];
                for(int i=0; i<16; i++){
                    salt[i]=receiver.getTag()[i*2];
                }
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), salt,65536, 256);
                SecretKey tmp = factory.generateSecret(spec);
                SecretKey newKey = new SecretKeySpec(tmp.getEncoded(), "AES");
                receiver.setKey(newKey);

                chatGUI.textArea.append(uur+":"+minuten+" - "+"["+friendname+"]: " + message + "\n");
                chatGUI.textArea.setCaretPosition(chatGUI.textArea.getDocument().getLength());
                this.messageHistory.get(friendname).add(uur+":"+minuten+" - "+"["+friendname+"]: " + message + "\n");
            }

            thisMessage = serverIF.getMessageFromBoard(receiver.getBoxNummer(), receiver.getTag());
        }
    }

    public void sendMessageTo(String friendname, String message) throws RemoteException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException {
        if(message.length()!=0){
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
            chatGUI.textArea.append(uur+":"+minuten+" - "+"[You]: "+oldMessage+"\n");
            chatGUI.textArea.setCaretPosition(chatGUI.textArea.getDocument().getLength());
            this.messageHistory.get(friendname).add(uur+":"+minuten+" - "+"[You]: "+oldMessage+"\n");
        }

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

}
