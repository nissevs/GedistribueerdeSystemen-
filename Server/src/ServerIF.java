import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;
import java.security.NoSuchAlgorithmException;

public interface ServerIF extends Remote {

     void registerListener(String [] details) throws RemoteException;
     void leaveChat(String username) throws RemoteException;
     void sendMessageIntoBoard(int boxNr, byte[] message, byte[] gehashteTag) throws RemoteException;
     int getGrootteVanBoard() throws RemoteException;
     byte[] getMessageFromBoard(int boxNr, byte[] tag) throws RemoteException, NoSuchAlgorithmException;

}
