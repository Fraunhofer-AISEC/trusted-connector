import de.fhg.aisec.ids.idscp2.Server.ServerConfiguration;
import de.fhg.aisec.ids.idscp2.Server.TLSServer;

public class RunTLSServer {

    public static void main(String[] argv){

        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setKeyStorePath(RunTLSServer.class.getClassLoader().
                getResource("jsse/aisecconnector2-keystore.jks").getPath());
        serverConfiguration.setTrustStorePath(RunTLSServer.class.getClassLoader().
                getResource("jsse/client-truststore_new.jks").getPath());
        TLSServer tlsServer = new TLSServer(serverConfiguration);
        tlsServer.start();
        tlsServer.close();
    }
}
