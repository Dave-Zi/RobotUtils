package Communication;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("unused")
public class CommunicationHandler implements ICommunication {

    private Channel commandsChannel;
    private Channel dataChannel;
    private Channel freeChannel;
    private Channel sosChannel;
    private Connection connection;
    private final ConnectionFactory factory = new ConnectionFactory();
    private int messageId = 0;

    /**
     * Connect to RabbitMQ queues.
     * @throws IOException thrown if connecting failed
     * @throws TimeoutException thrown if connecting timed out
     */
    public void connect() throws IOException, TimeoutException {
        connection = factory.newConnection();
        Map<String, Object> args = Map.of("x-max-length", 1);
        commandsChannel = connection.createChannel();
        dataChannel = connection.createChannel();
        sosChannel = connection.createChannel();
        freeChannel = connection.createChannel();

        commandsChannel.basicQos(1);
        dataChannel.basicQos(1);
        sosChannel.basicQos(1);

        commandsChannel.queueDeclare(QueueNameEnum.Commands.name(), false, false, false, args);
        dataChannel.queueDeclare(QueueNameEnum.Data.name(), false, false, false, args);
        sosChannel.queueDeclare(QueueNameEnum.Free.name(), false, false, false, null);
        freeChannel.queueDeclare(QueueNameEnum.SOS.name(), false, false, false, null);
    }

    /**
     * Remove all messages from queue
     * @param queue name of queue to purge
     * @throws IOException if reaching queue failed
     */
    public void purgeQueue(QueueNameEnum queue) throws IOException {
        switch (queue){
            case Commands:
                commandsChannel.queuePurge(queue.name());
                break;

            case Data:
                dataChannel.queuePurge(queue.name());
                break;

            case SOS:
                sosChannel.queuePurge(queue.name());
                break;

            case Free:
                freeChannel.queuePurge(queue.name());
                break;
        }
    }

    /**
     * Listen to queue and execute callback when messages from it arrive
     * @param queue name of queue to listen to
     * @param callback to execute when messages arrive
     * @throws IOException thrown if connecting failed
     */
    public void consumeFromQueue(QueueNameEnum queue, DeliverCallback callback) throws IOException {
        switch (queue){
            case SOS:
                sosChannel.basicConsume(queue.name(), true,
                        callback, consumerTag -> {});
                break;

            case Free:
                freeChannel.basicConsume(queue.name(), true,
                        callback, consumerTag -> {});
                break;

            case Commands:
                commandsChannel.basicConsume(queue.name(), false,
                        (consumerTag, delivery) -> delayedAckCallback(consumerTag, delivery, callback, commandsChannel), consumerTag -> {});
                break;

            case Data:
                dataChannel.basicConsume(queue.name(), false,
                        (consumerTag, delivery) -> delayedAckCallback(consumerTag, delivery, callback, dataChannel), consumerTag -> {});
                break;
        }
    }

    /**
     * Close Send and Receive connection
     *
     * @throws IOException      on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void closeConnection() throws IOException, TimeoutException {
        if (commandsChannel != null){
            commandsChannel.close();
        }
        if (dataChannel != null){
            freeChannel.close();
        }
        if (sosChannel != null){
            freeChannel.close();
        }
        if (freeChannel != null){
            freeChannel.close();
        }
        if (connection != null){
            connection.close();
        }
    }

    /**
     * Put message in Send queue
     *
     * @param message to send
     * @param queueName  send the message in this queue
     * @throws IOException on connection error
     */
    public void send(String message, QueueNameEnum queueName) throws IOException {
        switch (queueName){
            case Commands:
                commandsChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
                                .messageId(String.valueOf(messageId))
                                .build(),
                        message.getBytes());
                break;

            case Data:
                dataChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
                                .messageId(String.valueOf(messageId))
                                .build(),
                        message.getBytes());
                break;

            case SOS:
                sosChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
                                .messageId(String.valueOf(messageId))
                                .build(),
                        message.getBytes());
                break;

            case Free:
                freeChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
                                .messageId(String.valueOf(messageId))
                                .build(),
                        message.getBytes());
                break;
        }
        messageId++;
    }

    /**
     * Default callback for receiving messages
     *
     * @param consumerTag Rabbitmq consumer tag
     * @param delivery    object containing message and data
     */
    private void delayedAckCallback(String consumerTag, Delivery delivery, DeliverCallback callback, Channel channel) {
        try {
            callback.handle(consumerTag, delivery);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set credentials of RabbitMQ service location
     * @param host name/IP address of target machine
     * @param username to log in with
     * @param password to log in with
     */
    public void setCredentials(String host, String username, String password){
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
    }
}
