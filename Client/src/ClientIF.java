import java.rmi.Remote;

public interface ClientIF extends Remote {
     void messageFromServer(String message);
     String getName();
      void updateUserList(String [] currentUsers);
}
