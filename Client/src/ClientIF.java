import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientIF extends Remote {
     String getName() throws RemoteException;
}
