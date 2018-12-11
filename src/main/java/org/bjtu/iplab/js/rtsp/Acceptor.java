package org.bjtu.iplab.js.rtsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Acceptor implements Runnable {
    private final Logger log = LoggerFactory.getLogger(Acceptor.class);
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private Server server;
    private String acceptorName;
    private Selector selector = Selector.open();

    public Acceptor(Server server, String acceptorName) throws IOException {
        this.server = server;
        this.acceptorName = acceptorName;
    }

    @Override
    public void run() {
        log.info("{}开始监听", Thread.currentThread().getName());
        final SocketChannel client;
        try {
            client = server.serverSockeatAccept();
            client.configureBlocking(false);
            log.info("Acceptor接收到连接请求 {}", client);
            executorService.execute(new RtspHandler(client));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
