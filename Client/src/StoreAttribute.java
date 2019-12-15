import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class StoreAttribute {

    private SecretKey key;
    private int boxNummer;
    private byte[] tag;

    public StoreAttribute(SecretKey key, int boxNummer, byte[] tag) {
        this.key = key;
        this.boxNummer = boxNummer;
        this.tag = tag;
    }

    public StoreAttribute(String storeAttributeString){
        String[] split = storeAttributeString.split("\\|");
        System.out.println("KEY: "+split[0]);
        byte[] decodedKey = Base64.getDecoder().decode(split[0]);

        this.key = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        this.tag = split[1].getBytes();
        this.boxNummer = Integer.parseInt(split[2]);

    }

    public SecretKey getKey() {
        return key;
    }

    public void setKey(SecretKey key) {
        this.key = key;
    }

    public int getBoxNummer() {
        return boxNummer;
    }

    public void setBoxNummer(int boxNummer) {
        this.boxNummer = boxNummer;
    }

    public byte[] getTag() {
        return tag;
    }

    public void setTag(byte[] tag) {
        this.tag = tag;
    }

    @Override
    public String toString(){
        String oplossing = "";

        String keyToString = Base64.getEncoder().encodeToString(this.key.getEncoded());
        String tagToString = new String(this.tag);
        oplossing += keyToString+"|";
        oplossing += tagToString+"|";
        oplossing += this.boxNummer;

        return oplossing;
    }
}
