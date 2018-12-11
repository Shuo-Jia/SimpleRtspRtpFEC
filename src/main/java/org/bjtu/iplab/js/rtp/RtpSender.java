package org.bjtu.iplab.js.rtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;

public class RtpSender {
    private ByteBuffer byteBufferA = ByteBuffer.allocate(1488);
    private ByteBuffer byteBufferB = ByteBuffer.allocate(1488);
    private DatagramChannel datagramChannelSender;
    private FileChannel fileChannel;

    public RtpSender(String filePath) throws IOException {
        this.datagramChannelSender = DatagramChannel.open();
        datagramChannelSender.configureBlocking(true);
        datagramChannelSender.socket().bind(new InetSocketAddress(1024));
        datagramChannelSender.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 4321));
        fileChannel = new FileInputStream(new File(filePath)).getChannel();
    }

    public void sendPacket() throws IOException, InterruptedException {
        int a = 1;
        int b = 1;
        int num = 0;
        byte[] b1;
        byte[] b2;
        while (a > 0 || b > 0) {
            a = fileChannel.read(byteBufferA);
            b = fileChannel.read(byteBufferB);
            byteBufferA.flip();
            byteBufferB.flip();
            b1 = byteBufferA.array();
            b2 = byteBufferB.array();
            RTPacket p1 = new RTPacket(26, num++, 0, b1, b1.length);
            RTPacket p2 = new RTPacket(26, num++, 0, b2, b2.length);
            if (a > 0) {
                //Thread.sleep(100);
                System.out.println("A:" + p1.getSequenceNumber() + " len=" + a);
                byteBufferA.flip();
                datagramChannelSender.send(ByteBuffer.wrap(p1.getPacket()), new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 4321));
                byteBufferA.clear();
            }
            if (b > 0) {
                //Thread.sleep(100);
                //模拟丢包
                if (p2.getSequenceNumber() != 23 && p2.getSequenceNumber() != 45) {
                    System.out.println("B:" + p2.getSequenceNumber() + " len=" + b);
                    byteBufferB.flip();
                    datagramChannelSender.send(ByteBuffer.wrap(p2.getPacket()), new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 4321));
                    byteBufferB.clear();
                }else{
                    System.out.println("drop "+p2.getSequenceNumber());
                }
            }
            if (a > 0 && b > 0) {
                //Thread.sleep(100);
                byte[] fecBuf = new byte[1600];
                FECPacket fecPacket = new FECPacket(p1, p2, num-1);
                int length = fecPacket.getPacket(fecBuf);
                RTPacket rtPacketWithFEC = new RTPacket(100, num - 1, 0, fecBuf, length);
                datagramChannelSender.send(ByteBuffer.wrap(rtPacketWithFEC.getPacket()), new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 4321));
                System.out.println("FEC:" + rtPacketWithFEC.getSequenceNumber() + " len=" + length);
            }
        }
        Thread.sleep(100);
        byte[] byeBuf = new byte[1600];
        RTPacket rtPacketBye = new RTPacket(101, num - 1, 0, byeBuf, 1488);
        datagramChannelSender.send(ByteBuffer.wrap(rtPacketBye.getPacket()), new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 4321));
        System.out.println("Send ok:" + rtPacketBye.getSequenceNumber());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        RtpSender rtpSender = new RtpSender("file/s.264");
        rtpSender.sendPacket();
    }
}
