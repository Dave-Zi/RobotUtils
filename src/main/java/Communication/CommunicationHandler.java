package Communication;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class CommunicationHandler implements ICommunication {

    private Channel restrictedChannel;
    private Channel unrestrictedChannel;
    private ConnectionFactory factory = new ConnectionFactory();
    private Connection connection;
    private int messageId = 0;

    public CommunicationHandler() throws IOException, TimeoutException {
        connection = factory.newConnection();

        restrictedChannel = connection.createChannel();
        restrictedChannel.basicQos(1);
        Map<String, Object> args = Map.of("x-max-length", 1);
        restrictedChannel.queueDeclare(QueueNameEnum.Commands.name(), false, false, false, args);
        restrictedChannel.queueDeclare(QueueNameEnum.Data.name(), false, false, false, args);

        unrestrictedChannel = connection.createChannel();
        unrestrictedChannel.queueDeclare(QueueNameEnum.Free.name(), false, false, false, null);
        unrestrictedChannel.queueDeclare(QueueNameEnum.SOS.name(), false, false, false, null);
    }

    public void purgeQueue(QueueNameEnum queue) throws IOException {
        switch (queue){
            case Commands:
                restrictedChannel.queuePurge(queue.name());
                break;

            case Data:
                restrictedChannel.queuePurge(queue.name());
                break;

            case SOS:
                unrestrictedChannel.queuePurge(queue.name());
                break;

            case Free:
                unrestrictedChannel.queuePurge(queue.name());
        }
    }

    public void consumeFromQueue(QueueNameEnum queue, DeliverCallback callback) throws IOException {
        switch (queue){
            case SOS:
                unrestrictedChannel.basicConsume(queue.name(), true,
                        callback, consumerTag -> {});
                break;

            case Commands:
                restrictedChannel.basicConsume(queue.name(), false,
                        (consumerTag, delivery) -> delayedAckCallback(consumerTag, delivery, callback), consumerTag -> {});
                break;

            case Data:
                restrictedChannel.basicConsume(queue.name(), false,
                        (consumerTag, delivery) -> delayedAckCallback(consumerTag, delivery, callback), consumerTag -> {});
                break;

            case Free:
                unrestrictedChannel.basicConsume(queue.name(), true,
                        callback, consumerTag -> { });
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
        if (restrictedChannel != null){
            restrictedChannel.close();
        }
        if (unrestrictedChannel != null){
            unrestrictedChannel.close();
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
                restrictedChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
                                .messageId(String.valueOf(messageId))
                                .build(),
                        message.getBytes());
                break;

            case Data:
                restrictedChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
                                .messageId(String.valueOf(messageId))
                                .build(),
                        message.getBytes());
                break;

            case SOS:
                unrestrictedChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
                                .messageId(String.valueOf(messageId))
                                .build(),
                        message.getBytes());
                break;

            case Free:
                unrestrictedChannel.basicPublish("", queueName.name(), new AMQP.BasicProperties.Builder()
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
    private void delayedAckCallback(String consumerTag, Delivery delivery, DeliverCallback callback) {
        try {
            callback.handle(consumerTag, delivery);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            unrestrictedChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCredentials(String host, String username, String password) {
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
    }
}
