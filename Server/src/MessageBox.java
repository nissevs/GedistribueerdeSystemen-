import java.util.HashMap;
import java.util.Map;

public class MessageBox {

    //De key in deze map is de gehashte tag
    private Map<String, byte[]> messages;

    public MessageBox(){
        this.messages = new HashMap<String, byte[]>();
    }

    public byte[] getMessage(byte[] tag){
        byte[] message = this.messages.get(new String(tag));
        if(this.messages.containsKey(new String(tag))){
            System.out.println("DELETED MESSAGE - "+new String(message));
            System.out.println("WITH TAG - "+new String(tag));
        }
        this.messages.remove(new String(tag));
        return message;
    }

    public void setTag(byte[] tag, byte[] message){
        System.out.println("ADDED MESSAGE - "+new String(message));
        System.out.println("WITH TAG - "+new String(tag));
        this.messages.put(new String(tag), message);
    }

}
