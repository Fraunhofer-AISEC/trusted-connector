/*-
 * ========================LICENSE_START=================================
 * camel-ids
 * %%
 * Copyright (C) 2019 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.camel.ids.client;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static de.fhg.aisec.ids.camel.ids.client.WsProducer.STREAM_BUFFER_SIZE;
import static org.junit.Assert.assertArrayEquals;

public class WsProducerTest {
    public static final int[] TEST_SIZES = {
            0, 1,
            STREAM_BUFFER_SIZE - 1, STREAM_BUFFER_SIZE, STREAM_BUFFER_SIZE + 1,
            STREAM_BUFFER_SIZE * 2 - 1, STREAM_BUFFER_SIZE * 2, STREAM_BUFFER_SIZE * 2 + 1
    };

    public byte[] getRandomPrintableAsciiBytes(int length) {
        byte[] buffer = new byte[length];
        var rand = new Random();
        for (int i = 0; i < length; i++) {
            buffer[i] = (byte)('!' + rand.nextInt('~' - '!'));
        }
        return buffer;
    }

    public Stream<byte[]> getTestByteArrayStream() {
        return Arrays.stream(TEST_SIZES).mapToObj(this::getRandomPrintableAsciiBytes);
    }

//    public Exchange getTestExchange(Object body) {
//        var message = mock(Message.class);
//        when(message.getBody()).thenReturn(body);
//        var exchange = mock(Exchange.class);
//        when(exchange.getIn()).thenReturn(message);
//        return exchange;
//    }
//
//    public WsProducer getProducerMock(WebSocketMockHelper mockHelper, boolean streaming) throws Exception {
//        var endpoint = mock(WsEndpoint.class);
//        when(endpoint.isUseStreaming()).thenReturn(streaming);
//        when(endpoint.getWebSocket()).thenReturn(mockHelper.getMock());
//        var producer = mock(WsProducer.class);
//        when(producer.getEndpoint()).thenReturn(endpoint);
//        doCallRealMethod().when(producer).process(any(Exchange.class));
//        return producer;
//    }

    @Test
    public void testSendMessage() {
        var wsMockHelper = new WebSocketMockHelper();
//        var producerMockDefault = getProducerMock(wsMockHelper, false);
//        var producerMockStreaming = getProducerMock(wsMockHelper, true);
        getTestByteArrayStream().forEach(bytes -> {
            try {
                WsProducer.sendMessage(wsMockHelper.getMock(), bytes, false);
                assertArrayEquals("sendMessage() (bytes, non-streaming) seems flawed", bytes, wsMockHelper.getResultBytes());
                wsMockHelper.resetBuffer();
                WsProducer.sendMessage(wsMockHelper.getMock(), bytes, true);
                assertArrayEquals("sendMessage() (bytes, streaming) seems flawed", bytes, wsMockHelper.getResultBytes());
                wsMockHelper.resetBuffer();
                var string = new String(bytes, StandardCharsets.US_ASCII);
                WsProducer.sendMessage(wsMockHelper.getMock(), string, false);
                assertArrayEquals("sendMessage() (String, non-streaming) seems flawed", bytes, wsMockHelper.getResultBytes());
                wsMockHelper.resetBuffer();
                WsProducer.sendMessage(wsMockHelper.getMock(), string, true);
                assertArrayEquals("sendMessage() (String, streaming) seems flawed", bytes, wsMockHelper.getResultBytes());
                wsMockHelper.resetBuffer();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
