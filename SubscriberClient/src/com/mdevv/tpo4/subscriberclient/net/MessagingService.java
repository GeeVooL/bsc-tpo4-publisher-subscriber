package com.mdevv.tpo4.subscriberclient.net;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.mdevv.tpo4.common.protobuf.MessageWrapper;
import com.mdevv.tpo4.common.protobuf.ServerResponse;
import com.mdevv.tpo4.subscriberclient.SubscriberClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

public class MessagingService {
    final static String SERVER_ADDRESS = "SERVER_ADDRESS";
    final static String SERVER_PORT = "SERVER_PORT";
    final static int BUFFER_SIZE = 1024;

    private final InetSocketAddress serverAddress;
    private final Consumer<MessageWrapper> callback;
    private SocketChannel socketChannel;
    private ByteBuffer buffer;

    public MessagingService(Consumer<MessageWrapper> callback) {
        serverAddress = SubscriberClient.SERVER_ADDRESS;
        this.callback = callback;
    }

    public void connect() throws IOException {
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
        socketChannel = SocketChannel.open(serverAddress);
    }

    public void startReading() {
        Thread t = new Thread(() -> {
            ByteArrayOutputStream requestByteStream = new ByteArrayOutputStream();

            try {
                while (true) {
                    buffer.clear();
                    requestByteStream.reset();

                    while (true) {
                        int n = socketChannel.read(buffer);
                        if (n == -1) {
                            System.out.println("Channel closed.");
                            socketChannel.close();
                            return;
                        } else if (n > 0) {
                            buffer.flip();
                            while (buffer.hasRemaining()) {
                                requestByteStream.write(buffer.get());
                            }
                            buffer.clear();

                            if (n < buffer.capacity()) break;
                        }
                    }

                    MessageWrapper message = MessageWrapper.parseFrom(requestByteStream.toByteArray());
                    callback.accept(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

    public void send(Message message) throws IOException {
        MessageWrapper messageWrapper = MessageWrapper.newBuilder().
                setMessage(Any.pack(message)).
                build();

        System.out.println("Sending a request to " + socketChannel.toString());

        socketChannel.write(messageWrapper.toByteString().asReadOnlyByteBuffer());
        System.out.println(message.toString());
    }
}
