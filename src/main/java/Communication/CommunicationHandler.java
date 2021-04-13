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

    public void openQueues() throws IOException, TimeoutException {
        connection = factory.newConnection();

        sendChannel = connection.createChannel();
        sendChannel.queueDeclare(sendQueueName, false, false, false, null);

        receiveChannel = connection.createChannel();
        receiveChannel.queueDeclare(receiveQueueName, false, false, false, null);

        receiveChannel.basicConsume(receiveQueueName, true, myCallback, consumerTag -> { });
    }

    public void closeQueues() throws IOException, TimeoutException {
        sendChannel.close();
        receiveChannel.close();
        connection.close();
    }

    public void send(String message) throws IOException {
        sendChannel.basicPublish("", sendQueueName, null, message.getBytes());
    }

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
