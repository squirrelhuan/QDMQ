package cn.demomaster;

import static cn.demomaster.qdsocket.QDTcpServer.SERVICE_IP;
import static cn.demomaster.qdsocket.QDTcpServer.SERVICE_PORT;
import static org.junit.Assert.assertTrue;

import cn.demomaster.qdsocket.QDMessage;
import cn.demomaster.qdsocket.QDTcpServer;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void tcpServer()
    {
        QDTcpServer qdTcpServer = QDTcpServer.getInstance();
        qdTcpServer.creat(SERVICE_IP,SERVICE_PORT);
        qdTcpServer.setOnReceiveMessageListener(new QDTcpServer.OnReceiveMessageListener() {
            public void onReceiveMessage(long clientId, QDMessage qdMessage) {
                System.out.println("shou dao"+qdMessage.getMsg());
            }
        });
        qdTcpServer.start();
    }
}
