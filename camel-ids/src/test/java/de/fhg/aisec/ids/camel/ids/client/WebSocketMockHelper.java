package de.fhg.aisec.ids.camel.ids.client;

import org.asynchttpclient.ws.WebSocket;
import org.mockito.stubbing.Answer;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebSocketMockHelper {
    private final WebSocket mock;
    private final byte[] buffer = new byte[3 * WsProducer.STREAM_BUFFER_SIZE];
    private int bufferOffset = 0;
    private byte[] resultBytes = null;

    public WebSocketMockHelper() {
        mock = mock(WebSocket.class);
        var bytesAnswer = (Answer<?>) invocation -> {
            var nArgs = invocation.getArguments().length;
            handleFrame(
                    invocation.getArgument(0, byte[].class),
                    nArgs > 1 ? invocation.getArgument(1, Boolean.class) : true);
            return null;
        };
        var stringAnswer = (Answer<?>) invocation -> {
            var nArgs = invocation.getArguments().length;
            handleFrame(
                    invocation.getArgument(0, String.class).getBytes(StandardCharsets.US_ASCII),
                    nArgs > 1 ? invocation.getArgument(1, Boolean.class) : true);
            return null;
        };
        when(mock.sendBinaryFrame(any(byte[].class), any(Boolean.class), any(Integer.class))).thenAnswer(bytesAnswer);
        when(mock.sendBinaryFrame(any(byte[].class))).thenAnswer(bytesAnswer);
        when(mock.sendTextFrame(any(String.class), any(Boolean.class), any(Integer.class))).thenAnswer(stringAnswer);
        when(mock.sendTextFrame(any(String.class))).thenAnswer(stringAnswer);
        when(mock.sendContinuationFrame(any(byte[].class), any(Boolean.class), any(Integer.class))).thenAnswer(bytesAnswer);
        when(mock.sendContinuationFrame(any(String.class), any(Boolean.class), any(Integer.class))).thenAnswer(stringAnswer);
    }

    private synchronized void handleFrame(byte[] buffer, boolean isFinal) {
        if (resultBytes != null) {
            throw new RuntimeException("WebSocket mock received a final frame, fix the bug just observed " +
                    "or call resetBuffer() before reuse if intended!");
        }
        System.arraycopy(buffer, 0, this.buffer, bufferOffset, buffer.length);
        bufferOffset += buffer.length;
        // Set result on final frame
        if (isFinal) {
            resultBytes = new byte[bufferOffset];
            System.arraycopy(this.buffer, 0, resultBytes, 0, bufferOffset);
        }
    }

    public synchronized void resetBuffer() {
        bufferOffset = 0;
        resultBytes = null;
    }

    public WebSocket getMock() {
        return mock;
    }

    public byte[] getResultBytes() {
        return resultBytes;
    }
}
