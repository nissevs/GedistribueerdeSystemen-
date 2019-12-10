import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ChatClient extends UnicastRemoteObject implements ClientIF {

    ClientRMIGUI chatGUI;
    private String name;

    //public ClientIF user;
    protected ServerIF serverIF;
    protected boolean connectionProblem;


    /*@Override
    public ClientIF getClient() {
        return this;
    }*/

   /* @Override
    public void messageFromServer(String message)throws RemoteException{
        System.out.println(message);
        chatGUI.textArea.append( message );
        //make the gui display the last appended text, ie scroll to bottom
        chatGUI.textArea.setCaretPosition(chatGUI.textArea.getDocument().getLength());

    }*/


    @Override
    public String getName() {
        return this.name;
    }

    public ChatClient(ClientRMIGUI chatGUI,String name) throws RemoteException {
        super();
        this.name=name;
        //this.user=chatClient;
        this.chatGUI=chatGUI;

    }

   /* @Override
    public void updateUserList(String[] currentUsers)throws RemoteException{
        //if(currentUsers.length<2)
    }*/

    public void startClient()throws RemoteException{
        String [] details= {name, "192.168.0.130", "GroupChatService"};
        try {
            //Naming.rebind("rmi://192.168.0.119/GroupChatService", this);
            serverIF= (ServerIF) Naming.lookup("rmi://192.168.0.119/groupChat");
            /*String iets=serverIF.getValueToClient();
            System.out.println(iets);*/
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
        /*if(!connectionProblem){
            registerWithServer(details);
        }*/
        System.out.println("Client Listen RMI Server is running...\n");

    }



   /* public void registerWithServer(String[] details){
        try {
            serverIF.passIdentity(this.ref);
            serverIF.registerListener(details);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


}
