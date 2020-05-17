package de.fhg.aisec.ids.idscp2.idscp_core;

import com.google.protobuf.ByteString;
import de.fhg.aisec.ids.messages.IDSCPv2.*;
import java.util.Arrays;

/**
 * A factory for creating IDSCPv2 messages
 *
 * @author Leon Beckmann (leon.beckmann@aisec.fraunhofer.de)
 */
public class IdscpMessageFactory {

    public static IdscpMessage createIdscpHelloMessage(byte[] dat, String[] supportedRatSuite, String[] expectedRatSuite){
        IdscpDat idscpDat = IdscpDat.newBuilder()
                .setToken(ByteString.copyFrom(dat))
                .build();

        IdscpHello idscpHello = IdscpHello.newBuilder()
                .setVersion(2)
                .setDynamicAttributeToken(idscpDat)
                .addAllExpectedRatSuite(Arrays.asList(expectedRatSuite))
                .addAllSupportedRatSuite(Arrays.asList(supportedRatSuite))
                .build();

        return IdscpMessage.newBuilder()
                .setIdscpHello(idscpHello)
                .build();
    }

    public static IdscpMessage createIdscpCloseMessage(String closeMsg, IdscpClose.CloseCause causeCode){
        IdscpClose idscpClose = IdscpClose.newBuilder()
                .setCauseCode(causeCode)
                .setCauseMsg(closeMsg)
                .build();

        return IdscpMessage.newBuilder()
                .setIdscpClose(idscpClose)
                .build();
    }

    public static IdscpMessage createIdscpDatExpiredMessage(){
        return IdscpMessage.newBuilder()
                .setIdscpDatExpired(IdscpDatExpired.newBuilder().build())
                .build();
    }

    public static IdscpMessage createIdscpDatMessage(byte[] dat){
        IdscpDat idscpDat = IdscpDat.newBuilder()
                .setToken(ByteString.copyFrom(dat))
                .build();

        return IdscpMessage.newBuilder()
                .setIdscpDat(idscpDat)
                .build();
    }

    public static IdscpMessage createIdscpReRatMessage(String cause){
        IdscpReRat idscpReRat = IdscpReRat.newBuilder()
                .setCause(cause)
                .build();

        return IdscpMessage.newBuilder()
                .setIdscpReRat(idscpReRat)
                .build();
    }

    public static IdscpMessage createIdscpDataMessage(String type, Integer typeTag, byte[] data) {
        IdscpData.Builder idscpData = IdscpData.newBuilder()
                .setData(ByteString.copyFrom(data));
        if (type != null) {
            idscpData.setType(type);
        }
        if (typeTag != null) {
            idscpData.setTypeTag(typeTag);
        }

        return IdscpMessage.newBuilder()
                .setIdscpData(idscpData)
                .build();
    }

    public static IdscpMessage createIdscpDataMessage(int shortType, byte[] data) {
        return createIdscpDataMessage(null, shortType, data);
    }

    public static IdscpMessage createIdscpDataMessage(String type, byte[] data) {
        return createIdscpDataMessage(type, null, data);
    }

    public static IdscpMessage createIdscpRatProverMessage(byte[] body){
        IdscpRatProver idscpRatProver = IdscpRatProver.newBuilder()
            .setData(ByteString.copyFrom(body))
            .build();

        return IdscpMessage.newBuilder()
                .setIdscpRatProver(idscpRatProver)
                .build();
    }

    public static IdscpMessage createIdscpRatVerifierMessage(byte[] body){
        IdscpRatVerifier idscpRatVerifier = IdscpRatVerifier.newBuilder()
            .setData(ByteString.copyFrom(body))
            .build();

        return IdscpMessage.newBuilder()
                .setIdscpRatVerifier(idscpRatVerifier)
                .build();
    }

}
