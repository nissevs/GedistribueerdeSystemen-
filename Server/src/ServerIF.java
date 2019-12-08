import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;

public interface ServerIF extends Remote {

     void updateChat(String name, String nextPost) throws RemoteException;
     void passIdentity(RemoteRef ref) throws RemoteException;
     void registerListener(String [] details) throws RemoteException;
     void leaveChat(String username) throws RemoteException;
     void sendPM(int [] privateGroup, String message) throws RemoteException;

}
