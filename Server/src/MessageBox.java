import java.util.HashMap;
import java.util.Map;

public class MessageBox {

    //De key in deze map is de gehashte tag
    private Map<byte[], String> messages;

    public MessageBox(){
        this.messages = new HashMap<byte[], String>();
    }

    public String getMessage(byte[] tag){
        return this.messages.get(tag);
    }

    public void setTag(byte[] tag, String message){
        System.out.println("ADDED MESSAGE - "+tag+": "+message);
        this.messages.put(tag, message);
    }

}
