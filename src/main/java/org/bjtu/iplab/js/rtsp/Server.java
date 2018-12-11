package org.bjtu.iplab.js.rtsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

/**
 * Hello world!
 *
 */
public class Server {
    private final Logger log = LoggerFactory.getLogger(Server.class);

    private static final int DEFAULT_PORT = 1234;
    private static final int KEEP_ALIVE_TIMEOUT = 5000;
    private static final int MAX_KEEP_ALIVE_REQUESTS = 100;

    private ServerSocketChannel server;
    private volatile boolean isRunning = true;

    private Acceptor acceptor;

    static {
        Thread.currentThread().setName("Main-thread");
    }

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port){
        try{
            initServersocket(port);
            initAcceptor();
            log.info("服务器启动成功！");
        }catch (Exception e){
            e.printStackTrace();
            log.info("服务器启动失败！");
        }
    }

    private void initServersocket(int port) throws IOException {
        log.info("服务器初始化serverSocket");
        server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(port));
        server.configureBlocking(true);
    }


    private void initAcceptor() throws IOException {
        log.info("服务器初始化Acceptor线程");
        String acceptorName = "Acceptor-thread";
        acceptor = new Acceptor(this, acceptorName);
        Thread t = new Thread(acceptor, acceptorName);
        //t.setDaemon(true);
        t.start();
    }


    public SocketChannel serverSockeatAccept() throws IOException {
        return server.accept();
    }


    public int getKeepAliveTimeout() {
        return KEEP_ALIVE_TIMEOUT;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public static void main(String[] args) {
        new Server();
    }

    public void register(Selector selector) throws ClosedChannelException {
        server.register(selector, SelectionKey.OP_ACCEPT);
    }
}
