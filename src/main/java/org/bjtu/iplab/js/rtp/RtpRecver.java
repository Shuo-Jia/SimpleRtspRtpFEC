package org.bjtu.iplab.js.rtp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.util.*;

public class RtpRecver {

    private DatagramChannel datagramChannelRecver;
    private DatagramChannel datagramChannelFEC;
    private FileChannel fileChannel;
    private List<ByteBuffer> byteBufferListCache = new LinkedList<>();
    private ByteBuffer byteBuffer = ByteBuffer.allocate(15000);

    private Map<Integer, RTPacket> rtpMap = new TreeMap<>();
    private Map<Integer, RTPacket> rtpWithFecMap = new TreeMap<>();
    private List<Integer> dropIndex = new ArrayList<>();

    public RtpRecver(String filePath) throws IOException {
        datagramChannelRecver = DatagramChannel.open();
        datagramChannelRecver.configureBlocking(true);
        datagramChannelRecver.socket().bind(new InetSocketAddress(4321));

        datagramChannelFEC = DatagramChannel.open();
        datagramChannelFEC.configureBlocking(true);
        datagramChannelFEC.socket().bind(new InetSocketAddress(4322));

        fileChannel = new FileOutputStream(new File(filePath)).getChannel();
    }

    public void recvPacket() throws IOException, InterruptedException {
        int rtpIndex = 0;
        int rtpMaxIndex = 0;
        boolean end = false;
        while (true) {
            byte[] rtpByte;
            for (int i = 0; i < 9; i++) {
                //Thread.sleep(11);
                datagramChannelRecver.receive(byteBuffer);
                byteBuffer.flip();
                rtpByte = new byte[byteBuffer.remaining()];
                byteBuffer.get(rtpByte);
                RTPacket rtPacket = new RTPacket(rtpByte, rtpByte.length);
                if (rtPacket.getPayloadType() == 26) {
                    System.out.println("revering RTP packet:" + rtPacket.getSequenceNumber() + " len=" + rtpByte.length);
                    rtpMap.put(rtPacket.getSequenceNumber(), rtPacket);
                    rtpMaxIndex = rtPacket.getSequenceNumber();
                } else if (rtPacket.getPayloadType() == 100) {
                    System.out.println("revering FEC packet--->" + rtPacket.getSequenceNumber() + " len=" + rtpByte.length);
                    rtpWithFecMap.put(rtPacket.getSequenceNumber(), rtPacket);
                } else if (rtPacket.getPayloadType() == 101) {
                    System.out.println("revering:" + rtPacket.getSequenceNumber());
                    end = true;
                    break;
                }
                byteBuffer.clear();
            }

            for (int i = rtpIndex; i < rtpMaxIndex; i++) {
                if (rtpMap.get(i) == null) {
                    dropIndex.add(i);
                }
            }
            rtpIndex = rtpMaxIndex;

            for (int n : dropIndex) {
                if ((n & 0x01) == 0 && !dropIndex.contains(n + 1) && rtpWithFecMap.get(n + 1) != null) {
                    rebuild(n+1,n+1);
                } else if((n & 0x01) == 1 && !dropIndex.contains(n-1) && rtpWithFecMap.get(n) != null) {
                    rebuild(n-1,n);
                }
            }

            if (end) {
                System.out.println("receive ok!");
                break;
            }
        }

        writeTofile();
    }

    private void writeTofile() throws IOException {
        for(RTPacket rtPacket : rtpMap.values()){
            fileChannel.write(ByteBuffer.wrap(rtPacket.getPayload()));
        }
    }

    private void rebuild(int nRTP,int mFEC) {
        RTPacket rtPacketWithFEC = rtpWithFecMap.get(mFEC);
        byte[] fecByte = rtPacketWithFEC.getPayload();
        FECPacket fecPacket = new FECPacket(rtPacketWithFEC.getPayload(), fecByte.length);
        RTPacket sucessPacket = rtpMap.get(nRTP);
        RTPacket rebuidPacket = fecPacket.restoreL0(sucessPacket);
        rtpMap.put(rebuidPacket.getSequenceNumber(),rebuidPacket);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        RtpRecver rtpRecver = new RtpRecver("file/sr.264");
        rtpRecver.recvPacket();
    }
}
