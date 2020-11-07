package com.mdevv.tpo4.messagingserver;

import com.mdevv.tpo4.common.Configuration;
import com.mdevv.tpo4.common.protobuf.UpdateMessage;
import com.mdevv.tpo4.messagingserver.net.MessagingService;
import com.mdevv.tpo4.messagingserver.utils.Topics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class MessagingServer {
    private final static String LISTENING_HOST = "LISTENING_HOST";
    private final static String LISTENING_PORT = "LISTENING_PORT";

    private final MessagingService messagingService;
    private final InetSocketAddress socketAddress;

    public static void main(String[] args) {
        try {
            Configuration configuration = Configuration.fromCommandLine(args, Arrays.asList(
                    LISTENING_HOST,
                    LISTENING_PORT
            ));
            MessagingServer messagingServer = new MessagingServer(configuration);
            messagingServer.connect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public MessagingServer(Configuration configuration) throws IOException {
        socketAddress = new InetSocketAddress(configuration.get(LISTENING_HOST), configuration.getAsInt(LISTENING_PORT));
        Topics topics = new Topics();
        messagingService = new MessagingService(topics);
    }

    public void connect() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(socketAddress);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            try {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverSocketChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_READ, new LinkedList<UpdateMessage>());
                    } else if (key.isWritable()) {
                        messagingService.writeFromQueue(key);
                    } else if (key.isReadable()) {
                        messagingService.read(key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}