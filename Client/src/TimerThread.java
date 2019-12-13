import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Timer;
import java.util.TimerTask;

public class TimerThread extends Thread {
    private volatile boolean getMessages;
    ChatClient chatClient;

    @Override
    public void run() {
        while (getMessages) {
            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                int [] selectedIndexes = chatClient.chatGUI.getSelectedIndices();
                int index = -1;

                for(int i=0; i<selectedIndexes.length; i++){
                    index = selectedIndexes[i];
                }

                if(index != -1) chatClient.getMessagesFrom(chatClient.chatGUI.getUserAtIndex(index));
            } catch (RemoteException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopGettingMessages(){
        this.getMessages=false;
    }

    public TimerThread(ChatClient cC){
        chatClient=cC;
        getMessages=true;
    }


}
