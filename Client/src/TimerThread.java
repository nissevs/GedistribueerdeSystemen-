import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;

public class TimerThread extends Thread {
    private volatile boolean getMessages;
    ChatClient chatClient;

    @Override
    public void run() {
        while (getMessages) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            chatClient.getMessages();
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
