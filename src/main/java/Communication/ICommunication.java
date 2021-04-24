package Communication;

import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface ICommunication {
    void openSendQueue(boolean purge, boolean sos) throws IOException, TimeoutException;

    void openReceiveQueue(boolean purge, boolean sos) throws IOException, TimeoutException;

    void closeConnection() throws IOException, TimeoutException;

    void send(String message, boolean sos) throws IOException;

    void setCredentials(String host, String username, String password) throws IOException, TimeoutException;

    void setCallback(DeliverCallback myCallback);
}
