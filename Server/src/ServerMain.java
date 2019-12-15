import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class ServerMain extends UnicastRemoteObject implements ServerIF{

    private static MessageBox[] bulletinBoard;

    protected ServerMain() throws RemoteException {
        super();
        bulletinBoard = new MessageBox[15];
    }

    public int getGrootteVanBoard(){
        return this.bulletinBoard.length;
    }

    public static void main(String[] args) {
        startServer();
        String hostName = "localhost";
        String serviceName = "groupChat";

        try {
            ServerIF hellooooo = new ServerMain();
            Naming.rebind("rmi://" + hostName + "/" + serviceName, hellooooo);
            System.out.println("Daddy's home");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < bulletinBoard.length; i++) {
            bulletinBoard[i] = new MessageBox();
        }
    }

    @Override
    public void sendMessageIntoBoard(int boxNr, byte[] message, byte[] gehashteTag) throws RemoteException{
        System.out.println();
        System.out.println("Ontvangen message voor boxnummer "+boxNr+": "+new String(message));
        System.out.println("Tag: "+new String(gehashteTag));
        System.out.println();
        this.bulletinBoard[boxNr].setTag(gehashteTag, message);
    }

    @Override
    public byte[] getMessageFromBoard(int boxNr, byte[] tag) throws RemoteException, NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hashedTag = sha.digest(tag);

        byte[] message = this.bulletinBoard[boxNr].getMessage(hashedTag);

        System.out.println();
        System.out.println("Proberen om een bericht te krijgen uit boxnummer "+boxNr);
        System.out.println("Tag: "+new String(hashedTag));
        System.out.println();

        return message;
    }

    public static void startServer(){
        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
            System.out.println("The server is back, babydoll!!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerListener(String [] details)throws RemoteException{
        System.out.println(new Date(System.currentTimeMillis()));
        System.out.println(details[0]+" has accepted the challenge");
        System.out.println(details[0]+"'s RMI service"+details[2]);
    }

    @Override
    public void leaveChat(String username)throws RemoteException{
        System.out.println(username+" left the chat application...");
    }
}
