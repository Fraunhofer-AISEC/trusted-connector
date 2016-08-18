/**
 * 
 */
package org.apache.camel.component.websocket;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MemoryWebsocketStoreTest {

    private static final String KEY_1 = "one";
    private static final String KEY_2 = "two";
    
    @Mock
    private DefaultWebsocket websocket1;

    @Mock
    private DefaultWebsocket websocket2;

    private MemoryWebsocketStore store;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        store = new MemoryWebsocketStore();
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#add(java.lang.String, org.apache.camel.component.websocket.DefaultWebsocket)}.
     */
    @Test
    public void testAdd() {
        store.add(KEY_1, websocket1);
        assertEquals(websocket1, store.get(KEY_1));
        assertEquals(websocket1, store.get(KEY_1));
        store.add(KEY_1, websocket2);
        assertEquals(websocket2, store.get(KEY_1));
    }
    
    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#add(java.lang.String, org.apache.camel.component.websocket.DefaultWebsocket)}.
     */
    @Test(expected = NullPointerException.class)
    public void testAddNullValue() {
        store.add(KEY_1, null);
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#add(java.lang.String, org.apache.camel.component.websocket.DefaultWebsocket)}.
     */
    @Test(expected = NullPointerException.class)
    public void testAddNullKey() {
        store.add(null, websocket1);
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#remove(org.apache.camel.component.websocket.DefaultWebsocket)}.
     */
    @Test
    public void testRemoveDefaultWebsocket() {
        when(websocket1.getConnectionKey()).thenReturn(KEY_1);
        store.add(KEY_1, websocket1);
        assertEquals(websocket1, store.get(KEY_1));
        store.remove(websocket1);
        assertNull(store.get(KEY_1));
        
        InOrder inOrder = inOrder(websocket1, websocket2);
        inOrder.verify(websocket1, times(1)).getConnectionKey();
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#remove(org.apache.camel.component.websocket.DefaultWebsocket)}.
     */
    @Test
    public void testRemoveDefaultWebsocketKeyNotSet() {
        when(websocket1.getConnectionKey()).thenReturn(null);
        store.add(KEY_1, websocket1);
        assertEquals(websocket1, store.get(KEY_1));
        
        try {
            store.remove(websocket1);
            fail("Exception expected");
        }
        catch (Exception e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
        
        InOrder inOrder = inOrder(websocket1, websocket2);
        inOrder.verify(websocket1, times(1)).getConnectionKey();
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#remove(org.apache.camel.component.websocket.DefaultWebsocket)}.
     */
    @Test
    public void testRemoveNotExisting() {
        when(websocket2.getConnectionKey()).thenReturn(KEY_2);
        store.add(KEY_1, websocket1);
        assertEquals(websocket1, store.get(KEY_1));
        assertNull(store.get(KEY_2));
        store.remove(websocket2);
        assertEquals(websocket1, store.get(KEY_1));
        assertNull(store.get(KEY_2));
        
        InOrder inOrder = inOrder(websocket1, websocket2);
        inOrder.verify(websocket2, times(1)).getConnectionKey();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#remove(java.lang.String)}.
     */
    @Test
    public void testRemoveString() {
        store.add(KEY_1, websocket1);
        assertEquals(websocket1, store.get(KEY_1));
        store.remove(KEY_1);
        assertNull(store.get(KEY_1));
    }
    
    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#remove(java.lang.String)}.
     */
    @Test
    public void testRemoveStringNotExisting() {
        store.add(KEY_1, websocket1);
        assertEquals(websocket1, store.get(KEY_1));
        assertNull(store.get(KEY_2));
        store.remove(KEY_2);
        assertEquals(websocket1, store.get(KEY_1));
        assertNull(store.get(KEY_2));
    }


    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#get(java.lang.String)}.
     */
    @Test
    public void testGetString() {
        store.add(KEY_1, websocket1);
        assertEquals(websocket1, store.get(KEY_1));
        assertNull(store.get(KEY_2));
        store.add(KEY_2, websocket2);
        assertEquals(websocket1, store.get(KEY_1));
        assertEquals(websocket2, store.get(KEY_2));
    }

    /**
     * Test method for {@link org.apache.camel.component.websocket.MemoryWebsocketStore#getAll()}.
     */
    @Test
    public void testGetAll() {
        Collection<DefaultWebsocket> sockets = store.getAll();
        assertNotNull(sockets);
        assertEquals(0, sockets.size());
        
        store.add(KEY_1, websocket1);
        sockets = store.getAll();
        assertNotNull(sockets);
        assertEquals(1, sockets.size());
        assertTrue(sockets.contains(websocket1));

        store.add(KEY_2, websocket2);
        sockets = store.getAll();
        assertNotNull(sockets);
        assertEquals(2, sockets.size());
        assertTrue(sockets.contains(websocket1));
        assertTrue(sockets.contains(websocket2));
    }
}
