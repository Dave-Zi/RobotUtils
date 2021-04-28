package Communication;

public enum QueueNameEnum{
    Commands("Commands"),
    Data("Data"),
    Free("Free"),
    SOS("SOS");

    private String queueName;
    QueueNameEnum(String queueName) {
        this.queueName = queueName;
    }
}
