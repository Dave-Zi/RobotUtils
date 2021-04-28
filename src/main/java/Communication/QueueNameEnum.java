package Communication;

@SuppressWarnings("unused")
public enum QueueNameEnum{
    Commands("Commands"),
    Data("Data"),
    Free("Free"),
    SOS("SOS");

    @SuppressWarnings("FieldCanBeLocal")
    private final String queueName;
    QueueNameEnum(String queueName) {
        this.queueName = queueName;
    }
}
