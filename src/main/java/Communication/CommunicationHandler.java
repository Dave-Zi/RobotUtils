package Communication;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class CommunicationHandler {

    private Channel sendChannel;
    private Channel receiveChannel;
    private String sendQueueName = "Commands";
    private String receiveQueueName = "Data";
    private ConnectionFactory factory = new ConnectionFactory();
    private Connection connection;
    private DeliverCallback myCallback = this::onReceiveCallback;

    /**
     * Open Queue for sending messages
     * @param purge existing messages in queue
     * @throws IOException on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void openSendQueue(boolean purge) throws IOException, TimeoutException {
        sendChannel = setUpQueueOpening(sendQueueName, purge);
    }
    /**
     * Open Queue for receiving messages
     * @param purge existing messages in queue
     * @throws IOException on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void openReceiveQueue(boolean purge) throws IOException, TimeoutException {
        receiveChannel = setUpQueueOpening(receiveQueueName, purge);
        receiveChannel.basicConsume(receiveQueueName, true, myCallback, consumerTag -> { });
    }
    /**
     * Initiate new connection if necessary.
     * Close channel if it was already open, and create new one.
     * Purge queue if requested
     * @param queueName name of new queue
     * @param purge existing messages on queue
     * @return the channel with its opened queue
     * @throws IOException on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    private Channel setUpQueueOpening(String queueName, boolean purge) throws IOException, TimeoutException {
        if (connection == null){
            connection = factory.newConnection();
        }

        Channel channel = connection.createChannel();

        if (purge){
            channel.queuePurge(queueName);
        }
        channel.queueDeclare(queueName, false, false, false, null);
        return channel;
    }

    /**
     * Close Send queue
     * @throws IOException on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void closeSendQueue() throws IOException, TimeoutException {
        sendChannel.close();
    }

    /**
     * Close Receive queue
     * @throws IOException on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void closeReceiveQueue() throws IOException, TimeoutException {
        receiveChannel.close();
    }

    /**
     * Close Send and Receive connection
     * @throws IOException on connection error
     * @throws TimeoutException on no response from RabbitMQ server
     */
    public void closeQueues() throws IOException, TimeoutException {
        sendChannel.close();
        receiveChannel.close();
        connection.close();
    }

    /**
     * Put message in Send queue
     * @param message to send
     * @throws IOException on connection error
     */
    public void send(String message) throws IOException {
        sendChannel.basicPublish("", sendQueueName, null, message.getBytes());
    }

    /**
     * Default callback for receiving messages
     * @param consumerTag Rabbimq consumer tag
     * @param delivery object containing message and data
     */
    private void onReceiveCallback(String consumerTag, Delivery delivery){
        String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
        System.out.println(json);
    }

    public CommunicationHandler(String sendQueueName, String receiveQueueName){
        this.sendQueueName = sendQueueName;
        this.receiveQueueName = receiveQueueName;
    }

    public CommunicationHandler(String sendQueueName, String receiveQueueName, DeliverCallback myCallback){
        this.sendQueueName = sendQueueName;
        this.receiveQueueName = receiveQueueName;
        this.myCallback = myCallback;
    }

    public CommunicationHandler(DeliverCallback myCallback){
        this.myCallback = myCallback;
    }

    public CommunicationHandler(){
    }

    public String getSendQueueName() {
        return sendQueueName;
    }

    public String getReceiveQueueName() {
        return receiveQueueName;
    }

    public DeliverCallback getMyCallback() {
        return myCallback;
    }

    public void setSendQueueName(String sendQueueName) {
        this.sendQueueName = sendQueueName;
    }

    public void setReceiveQueueName(String receiveQueueName) {
        this.receiveQueueName = receiveQueueName;
    }

    public void setMyCallback(DeliverCallback myCallback) {
        this.myCallback = myCallback;
    }
}
