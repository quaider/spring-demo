package org.example.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 单reactor实现
 */
public class Reactor implements Runnable {

    // selector对象，多路复用器，类似epoll对象
    private Selector selector;

    private final ServerSocketChannel serverSocket;

    public Reactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));  // listen and bind port

        // 非阻塞
        serverSocket.configureBlocking(false);

        // 注册感兴趣的事件到selector对象，这里的事件是 accept，即连接事件
        SelectionKey selectionKey = serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        // selector监听到事件到来时，分发给Handler处理，即事件就绪时，对应要回调的callback
        selectionKey.attach(new Acceptor());
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                // 阻塞事件的到来，一旦有事件，便马上返回
                selector.select();

                // 获取已经就绪的事件
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    // 分发就绪的事件给对应的Handler处理，Reactor需要负责dispatch收到的事件
                    dispatch(iterator.next());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 事件分发
     *
     * @param selectionKey 事件对象
     */
    private void dispatch(SelectionKey selectionKey) {
        Runnable r = (Runnable) (selectionKey.attachment());
        if (r != null) {
            r.run();
        }
    }

    /**
     * Acceptor用于处理客户端的连接建立请求，即处理连接就绪事件: SelectionKey.OP_ACCEPT
     */
    class Acceptor implements Runnable {

        public void run() {
            try {
                // 建立与客户端的socket连接，channel代表一个客户端连接
                SocketChannel channel = serverSocket.accept();
                if (channel != null) {
                    new AcceptorHandler(selector, channel);
                }
            } catch (IOException ex) {
                /* ... */
            }
        }
    }

    class AcceptorHandler implements Runnable {

        // 客户端连接
        final SocketChannel channel;

        final SelectionKey sk;

        ByteBuffer input = ByteBuffer.allocate(1024);
        ByteBuffer output = ByteBuffer.allocate(1024);

        static final int READING = 0, SENDING = 1;

        int state = READING;

        AcceptorHandler(Selector selector, SocketChannel c) throws IOException {
            channel = c;
            c.configureBlocking(false);

            // 注册读事件到selector，客户端channel由selector管理
            sk = channel.register(selector, 0);

            // 自身作为Handler
            sk.attach(this);

            // 第二步, 注册Read就绪事件
            sk.interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }

        @Override
        public void run() {
            try {
                if (state == READING) {
                    read();
                } else if (state == SENDING) {
                    send();
                }
            } catch (IOException ex) {
                /* ... */
            }
        }

        private void send() throws IOException {
            channel.write(output);
            // write完就结束了, 关闭select key
            if (outputIsComplete()) {
                sk.cancel();
            }
        }

        private void read() throws IOException {
            channel.read(input);
            if (inputIsComplete()) {

                process();

                state = SENDING;

                // 第三步, 接收write就绪事件, write就绪应用层才能写(发送)数据
                sk.interestOps(SelectionKey.OP_WRITE);
            }
        }

        private void process() {
        }

        private boolean inputIsComplete() {
            return false;
        }

        private boolean outputIsComplete() {
            return false;
        }


    }
}
