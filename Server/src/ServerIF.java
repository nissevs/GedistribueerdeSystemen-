import java.rmi.Remote;
import java.rmi.server.RemoteRef;

public interface ServerIF extends Remote {

     void updateChat(String name, String nextPost, ChatClient chatClient);
     void passIdentity(RemoteRef ref);
     void registerListener(String [] details);
     void leaveChat(String username);
     void sendPM(int [] privateGroup, String message);

}
