import javax.crypto.SecretKey;

public class StoreAttribute {

    private SecretKey key;
    private int boxNummer;
    private byte[] tag;

    public StoreAttribute(SecretKey key, int boxNummer, byte[] tag) {
        this.key = key;
        this.boxNummer = boxNummer;
        this.tag = tag;
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
}
