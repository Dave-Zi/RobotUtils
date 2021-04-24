package Communication;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class CommunicationHandler implements ICommunication {

    private final String sosQueueName = "Sos";
    private final int queueSize = 5;
    private Channel sendChannel;
    private Channel receiveChannel;
    private String sendQueueName = "Commands";
    private String receiveQueueName = "Data";
    private ConnectionFactory factory = new ConnectionFactory();
    private Connection connection;
    private DeliverCallback myCallback = this::defaultCallback;
    private int messageId = 0;

    public CommunicationHandler(String sendQueueName, String receiveQueueName) {
        this.sendQueueName = sendQueueName;
        this.receiveQueueName = receiveQueueName;
    }

    public CommunicationHandler(String sendQueueName, String receiveQueueName, DeliverCallback myCallback) {
        this.sendQueueName = sendQueueName;
        this.receiveQueueName = receiveQueueName;
        this.myCallback = myCallback;
    }

//    /**
//     * Open Queue for receiving messages
//     * @param sender is the caller intending to send messages in this queue or to receive.
//     * @throws IOException on connection error
//     * @throws TimeoutException on no response from RabbitMQ server
//     */
//    public void openSosQueue(boolean sender) throws IOException, TimeoutException {
//        sosChannel = setUpQueueOpening("sos", true, false);
//        if (!sender){
//            sosChannel.basicConsume("sos", true, this::onReceiveCallback, consumerTag -> {});
//        }
//    }

    public CommunicationHandler(DeliverCallback myCallback) {
        this.myCallback = myCallback;
    }

//    /**
//     * Close Send queue
//     * @throws IOException on connection error
//     * @throws TimeoutException on no response from RabbitMQ server
//     */
//    public void closeSendQueue() throws IOException, TimeoutException {
//        sendChannel.close();
//    }
//
//    /**
//     * Close Receive queue
//     * @throws IOException on connection error
//     * @throws TimeoutException on no response from RabbitMQ server
//     */
//    public void closeReceiveQueue() throws IOException, TimeoutException {
//        receiveChannel.close();
//    }

    public CommunicationHandler() {
    }

    /**
     * Open Queue for sending messages
     *
     * @param purge existing messages in queue
     * @throws IOException      on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void openSendQueue(boolean purge, boolean sos) throws IOException, TimeoutException {
        sendChannel = setUpQueueOpening(sendQueueName, purge);
        sendChannel.queueDeclare(sosQueueName, false, false, false, null);
        if (sos){
            sendChannel.queuePurge(sosQueueName);
        }
    }

    /**
     * Open Queue for receiving messages
     *
     * @param purge existing messages in queue
     * @param sos   open an additional sos queue to receive messages from
     * @throws IOException      on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void openReceiveQueue(boolean purge, boolean sos) throws IOException, TimeoutException {
        receiveChannel = setUpQueueOpening(receiveQueueName, purge);
        receiveChannel.basicConsume(receiveQueueName, false, this::onReceiveCallback, consumerTag -> {
        });
        if (sos) {
            receiveChannel.basicConsume(sosQueueName, false, this::onReceiveCallback, consumerTag -> {
            });
        }
    }

    /**
     * Initiate new connection if necessary.
     * Close channel if it was already open, and create new one.
     * Purge queue if requested
     *
     * @param queueName name of new queue
     * @param purge     existing messages on queue
     * @return the channel with its opened queue
     * @throws IOException      on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    private Channel setUpQueueOpening(String queueName, boolean purge) throws IOException, TimeoutException {
        if (connection == null) {
            connection = factory.newConnection();
        }

        Channel channel = connection.createChannel();
        channel.basicQos(1);
        Map<String, Object> args = Map.of("x-max-length", queueSize);
        channel.queueDeclare(queueName, false, false, false, args);

        if (purge) {
            channel.queuePurge(queueName);
        }

        return channel;
    }

    /**
     * Close Send and Receive connection
     *
     * @throws IOException      on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void closeConnection() throws IOException, TimeoutException {
        sendChannel.close();
        receiveChannel.close();
        connection.close();
    }

    /**
     * Put message in Send queue
     *
     * @param message to send
     * @param sos     send this message in the sos queue
     * @throws IOException on connection error
     */
    public void send(String message, boolean sos) throws IOException {
        String queueName = sos ? sosQueueName : sendQueueName;
        sendChannel.basicPublish("", queueName, new AMQP.BasicProperties.Builder()
                        .messageId(String.valueOf(messageId))
                        .build(),
                message.getBytes());
        messageId++;
    }

    /**
     * Default callback for receiving messages
     *
     * @param consumerTag Rabbimq consumer tag
     * @param delivery    object containing message and data
     */
    private void onReceiveCallback(String consumerTag, Delivery delivery) {
        try {
            myCallback.handle(consumerTag, delivery);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            receiveChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void defaultCallback(String consumerTag, Delivery delivery) {
        String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    public String getSendQueueName() {
        return sendQueueName;
    }

    public void setSendQueueName(String sendQueueName) {
        this.sendQueueName = sendQueueName;
    }

    public String getReceiveQueueName() {
        return receiveQueueName;
    }

    public void setReceiveQueueName(String receiveQueueName) {
        this.receiveQueueName = receiveQueueName;
    }

    public DeliverCallback getMyCallback() {
        return myCallback;
    }

    public void setCallback(DeliverCallback myCallback) {
        this.myCallback = myCallback;
    }

    public void setCredentials(String host, String username, String password) {
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
    }
}
