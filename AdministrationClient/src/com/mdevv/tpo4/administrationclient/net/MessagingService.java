package com.mdevv.tpo4.administrationclient.net;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.mdevv.tpo4.administrationclient.AdministrationClient;
import com.mdevv.tpo4.common.protobuf.MessageWrapper;
import com.mdevv.tpo4.common.protobuf.ServerResponse;
import com.mdevv.tpo4.common.protobuf.UpdateMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MessagingService {
    private final int BUFFER_SIZE = 1024;

    private final SocketChannel socketChannel;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    public MessagingService() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(AdministrationClient.SERVER_ADDRESS);
        while (!socketChannel.finishConnect());
    }

    public ServerResponse.Status createTopic(String topicName) {
        UpdateMessage updateMessage = UpdateMessage.newBuilder().
                setType(UpdateMessage.Type.TOPIC_CREATION).
                setTopic(topicName).
                build();
        return send(updateMessage);
    }

    public ServerResponse.Status deleteTopic(String topicName) {
        UpdateMessage updateMessage = UpdateMessage.newBuilder().
                setType(UpdateMessage.Type.TOPIC_DELETION).
                setTopic(topicName).
                build();
        return send(updateMessage);
    }

    public ServerResponse.Status createMessage(String topicName, String message) {
        UpdateMessage updateMessage = UpdateMessage.newBuilder().
                setType(UpdateMessage.Type.MESSAGE).
                setTopic(topicName).
                setMessage(message).
                build();
        return send(updateMessage);
    }

    private ServerResponse.Status send(Message message) {
        ServerResponse.Status status = ServerResponse.Status.INVALID_REQUEST;

        try {
            MessageWrapper requestWrapper = MessageWrapper.newBuilder().
                    setMessage(Any.pack(message)).
                    build();

            System.out.println("Sending a request to " + socketChannel.toString());

            socketChannel.write(requestWrapper.toByteString().asReadOnlyByteBuffer());
            System.out.println(message.toString());

            byteBuffer.clear();
            ByteArrayOutputStream requestByteStream = new ByteArrayOutputStream();

            while (true) {
                int n = socketChannel.read(byteBuffer);
                if (n == -1) {
                    System.out.println("Channel closed.");
                    socketChannel.close();
                    return ServerResponse.Status.INVALID_REQUEST;
                } else if (n > 0) {
                    byteBuffer.flip();
                    while(byteBuffer.hasRemaining()) {
                        requestByteStream.write(byteBuffer.get());
                    }
                    byteBuffer.clear();
                    if (n < byteBuffer.capacity()) break;
                }
            }

            Any response = MessageWrapper.parseFrom(requestByteStream.toByteArray()).getMessage();

            if (response.is(ServerResponse.class)) {
                status = response.unpack(ServerResponse.class).getStatus();
            } else {
                throw new Exception("Invalid response type");
            }

            System.out.println("Request status code: " + status.name());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return status;
    }
}
