import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ChatClient extends UnicastRemoteObject implements ClientIF {

    private String name;
    //public ClientIF user;
    private ServerIF serverIF;


    /*@Override
    public ClientIF getClient() {
        return this;
    }*/

    @Override
    public void messageFromServer(String message){
        System.out.println(message);

    }

    @Override
    public String getName() {
        return this.name;
    }

    public ChatClient(String name) throws RemoteException {
        super();
        this.name=name;
        //this.user=chatClient;

    }

    @Override
    public void updateUserList(String[] currentUsers){
        //if(currentUsers.length<2)
    }

    public void startClient(){
        try {
            Naming.rebind("rmi://localhost/GroupChatService", this);

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void registerWithServer(String[] details){
        try {
            serverIF.passIdentity(this.ref);
            serverIF.registerListener(details);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
