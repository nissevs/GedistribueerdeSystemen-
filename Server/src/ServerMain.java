import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ServerMain extends UnicastRemoteObject implements ServerIF{

    private Vector<ClientIF> chatClients;
    Map<String, String> bulletinBoard;

    protected ServerMain() throws RemoteException {
        super();
        chatClients= new Vector<>();
        bulletinBoard = new HashMap<String, String>();
    }

    public static void main(String[] args) {
        startServer();
        String hostName = "192.168.0.119";
        String serviceName = "groupChat";

      /*  if(args.length==2){
            hostName= args[0];
            serviceName=args[1];
        }*/

        try {
            ServerIF hellooooo = new ServerMain();
            Naming.rebind("rmi://" + hostName + "/" + serviceName, hellooooo);
            System.out.println("Daddy's home");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public void sendMessageTo(String receiver, String message){
        bulletinBoard.put(receiver, message);
    }

    public String getMessageFrom(String sender){
        return bulletinBoard.get(sender);
    }

    public static void startServer(){
        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
            System.out.println("The server is back, babydoll!!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String greet(String username)throws RemoteException{
        System.out.println(username+" sldi into your DM's");
        return "Hi, have you met "+ username;
    }

    @Override
    public void updateChat(String name, String nextPost)throws RemoteException{
        String message= name+":"+nextPost+"\n";
        for(ClientIF chatClient: chatClients) {
            sendTo(chatClient, message);
        }
    }

    public void sendTo(ClientIF chatClient, String message){
        try {
            chatClient.messageFromServer(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void passIdentity(RemoteRef ref)throws RemoteException{
        try {
            System.out.println(ref.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerListener(String [] details)throws RemoteException{
        System.out.println(new Date(System.currentTimeMillis()));
        System.out.println(details[0]+" has accepted the challenge");
        System.out.println(details[0]+"'s RMI service"+details[2]);
        //registerChat(details);
    }

    public String getValueToClient() throws RemoteException{
        System.out.println("De client probeert de waarde te getten");
        return "Het is gelukt!";
    }

    private void registerChat(String[] details){
        try{
            ClientIF nextClient = ( ClientIF )Naming.lookup("rmi://" + details[1] + "/" + details[2]);

            chatClients.addElement(nextClient);

            nextClient.messageFromServer("[Server] : Hello " + details[0] + " you are now free to chat.\n");

            sendToAll("[Server] : " + details[0] + " has joined the group.\n");

            updateUserList();
        }
        catch(RemoteException | MalformedURLException | NotBoundException e){
            e.printStackTrace();
        }
    }

    private void updateUserList() throws RemoteException {
        String[] currentUsers= getUserList();
        for(ClientIF c: chatClients){
            try {
                c.updateUserList(currentUsers);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String[] getUserList() throws RemoteException {
        // generate an array of current users
        String[] allUsers = new String[chatClients.size()];
        for(int i = 0; i< allUsers.length; i++){
            allUsers[i] = chatClients.elementAt(i).getName();
        }
        return allUsers;
    }

    public void sendToAll(String message){
        for(ClientIF c: chatClients){
            try {
                c.messageFromServer(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void leaveChat(String username)throws RemoteException{
        for(ClientIF c: chatClients){
            if(c.getName().equals(username)){
                System.out.println(username+": FRIENDSHIP OVER!!");
                chatClients.remove(c);
                break;
            }
        }
        if(!chatClients.isEmpty()){
            updateUserList();
        }
    }

    @Override
    public void sendPM(int [] privateGroup, String message)throws RemoteException{
        for(int i: privateGroup){
            ClientIF c= chatClients.elementAt(i);
            c.messageFromServer(message);
        }
    }


}
