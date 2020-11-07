package com.mdevv.tpo4.messagingserver.net;

import com.google.protobuf.Any;
import com.mdevv.tpo4.common.protobuf.*;
import com.mdevv.tpo4.messagingserver.utils.Topics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class MessagingService {
    private final static int BUFFER_SIZE = 1024;

    private Topics topics;
    private List<SelectionKey> allClients;
    private final ByteBuffer byteBuffer;
    private final ByteArrayOutputStream requestByteStream;

    public MessagingService(Topics topics) {
        this.topics = topics;
        this.allClients = new ArrayList<>();
        byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        requestByteStream = new ByteArrayOutputStream();
    }

    public void read(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        if (!clientChannel.isOpen()) return;

        byteBuffer.clear();
        requestByteStream.reset();

        while (true) {
            int n = clientChannel.read(byteBuffer);
            if (n == -1) {
                System.out.println("Channel closed.");
                clientChannel.close();
                removeClient(key);
                return;
            } else if (n > 0) {
                byteBuffer.flip();
                while (byteBuffer.hasRemaining()) {
                    requestByteStream.write(byteBuffer.get());
                }
                byteBuffer.clear();

                if (n < byteBuffer.capacity()) break;
            }
        }

        Any anyRequest = MessageWrapper.parseFrom(requestByteStream.toByteArray()).getMessage();

        if (anyRequest.is(SubscriberMessage.class)) {
            SubscriberMessage message = anyRequest.unpack(SubscriberMessage.class);
            handleSubscriberMessage(message, key);
        } else if (anyRequest.is(UpdateMessage.class)) {
            UpdateMessage message = anyRequest.unpack(UpdateMessage.class);
            handleUpdateMessage(message, key);
        } else {
            throw new UnsupportedOperationException("Request type is not supported");
        }
    }

    private void removeClient(SelectionKey key) {
        allClients.remove(key);
        topics.deleteItem(key);
    }

    // Topics message

    private void handleSubscriberMessage(SubscriberMessage message, SelectionKey key) {
        switch (message.getType()) {
            case REGISTER:
                registerClient(key);
                break;
            case SUBSCRIBE:
                subscribeTopic(message, key);
                break;
            case UNSUBSCRIBE:
                unsubscribeTopic(message, key);
                break;
        }
    }

    private void registerClient(SelectionKey key) {
        allClients.add(key);
        Set<String> allTopics = topics.list();
        respond(key, ServerResponse.Status.OK, allTopics);
    }

    private void subscribeTopic(SubscriberMessage message, SelectionKey key) {
        List<SelectionKey> registry = topics.get(message.getTopic());

        if (registry == null) {
            respond(key, ServerResponse.Status.INVALID_REQUEST, null);
            return;
        }

        registry.add(key);
        respond(key, ServerResponse.Status.OK, null);
    }

    private void unsubscribeTopic(SubscriberMessage message, SelectionKey key) {
        List<SelectionKey> registry = topics.get(message.getTopic());

        if (registry == null || !registry.remove(key)) {
            respond(key, ServerResponse.Status.INVALID_REQUEST, null);
            return;
        }

        respond(key, ServerResponse.Status.OK, null);
    }

    // Update message

    private void handleUpdateMessage(UpdateMessage message, SelectionKey key) {
        switch (message.getType()) {
            case TOPIC_CREATION:
                handleTopicCreation(message, key);
                break;
            case TOPIC_DELETION:
                handleTopicDeletion(message, key);
                break;
            case MESSAGE:
                handleMessage(message, key);
                break;
        }
    }

    private void handleTopicCreation(UpdateMessage message, SelectionKey key) {
        try {
            topics.add(message.getTopic());
        } catch (Exception e) {
            respond(key, ServerResponse.Status.NOT_FOUND, null);
            return;
        }

        notifyAll(message);
        respond(key, ServerResponse.Status.OK, null);
    }

    private void handleTopicDeletion(UpdateMessage message, SelectionKey key) {
        try {
            topics.delete(message.getTopic());
        } catch (Exception e) {
            respond(key, ServerResponse.Status.NOT_FOUND, null);
            return;
        }

        notifyAll(message);
        respond(key, ServerResponse.Status.OK, null);
    }

    private void handleMessage(UpdateMessage message, SelectionKey key) {
        if (!topics.exists(message.getTopic())) {
            respond(key, ServerResponse.Status.NOT_FOUND, null);
            return;
        }

        notify(message);
        respond(key, ServerResponse.Status.OK, null);
    }

    private void respond(SelectionKey key, ServerResponse.Status status, Iterable<String> content) {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ServerResponse.Builder responseBuilder = ServerResponse.newBuilder();
        responseBuilder.setStatus(status);
        if (content != null) responseBuilder.addAllContent(content);
        ServerResponse response = responseBuilder.build();

        System.out.println("Sending ServerResponse message:\n" + response.toString());

        MessageWrapper messageWrapper = MessageWrapper.newBuilder().
                setMessage(Any.pack(response)).
                build();

        ByteBuffer buffer = messageWrapper.toByteString().asReadOnlyByteBuffer();

        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socketChannel.close();
                key.cancel();
            } catch (IOException ignored) {
            }
        }
    }

    private void notify(UpdateMessage message) {
        List<SelectionKey> keys = topics.get(message.getTopic());
        keys.forEach(key -> {
            ((Queue<UpdateMessage>) key.attachment()).add(message);
            key.interestOps(SelectionKey.OP_WRITE);
        });
    }

    private void notifyAll(UpdateMessage message) {
        allClients.forEach(key -> {
            ((Queue<UpdateMessage>) key.attachment()).add(message);
            key.interestOps(SelectionKey.OP_WRITE);
        });
    }

    public void writeFromQueue(SelectionKey key) throws IOException {
        Queue<UpdateMessage> queue = (Queue<UpdateMessage>) key.attachment();
        if (queue.isEmpty()) {
            return;
        }

        SocketChannel socketChannel = (SocketChannel) key.channel();
        UpdateMessage message = queue.remove();

        System.out.println("Sending queued UpdateMessage:\n" + message.toString());

        MessageWrapper messageWrapper = MessageWrapper.newBuilder().
                setMessage(Any.pack(message)).
                build();

        ByteBuffer buffer = messageWrapper.toByteString().asReadOnlyByteBuffer();
        socketChannel.write(buffer);
        key.interestOps(SelectionKey.OP_READ);
    }
}
