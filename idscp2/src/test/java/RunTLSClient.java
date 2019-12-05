import de.fhg.aisec.ids.idscp2.Client.ClientConfiguration;
import de.fhg.aisec.ids.idscp2.Client.TLSClient;

public class RunTLSClient {

    public static void main(String[] args){

        //start client
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setKeyStorePath(RunTLSClient.class.getClassLoader().
                getResource("jsse/aisecconnector2-keystore.jks").getPath());
        clientConfiguration.setTrustStorePath(RunTLSClient.class.getClassLoader().
                getResource("jsse/client-truststore_new.jks").getPath());
        TLSClient tlsClient = new TLSClient(clientConfiguration);
        if (!tlsClient.connect())
            return;

        tlsClient.send("hi, how are u");

        tlsClient.disconnect(true);
    }

}
