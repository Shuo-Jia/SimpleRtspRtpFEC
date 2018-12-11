package org.bjtu.iplab.js.rtsp;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RtspHandler implements Runnable {
    private final Logger log = LoggerFactory.getLogger(RtspHandler.class);

    private SocketChannel socketRTSPChannelClient;
    private ByteBuffer requestBuffer = ByteBuffer.allocate(2048);
    private ByteBuffer responseBuffer = ByteBuffer.allocate(2048);
    private SimpleDateFormat sdf = new SimpleDateFormat("EE, MMM dd yyyy HH:mm:ss", Locale.ENGLISH);

    private DatagramChannel serverRTCPChannel;
    private DatagramChannel serverRTPChannel;

    public RtspHandler() {
    }

    public RtspHandler(SocketChannel client) {
        this.socketRTSPChannelClient = client;
    }

    int n = 0;
    boolean flag = false;

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                if (socketRTSPChannelClient.isConnected()) {
                    while (socketRTSPChannelClient.read(requestBuffer) > 0) {
                        System.out.println("《读数据流" + socketRTSPChannelClient.isConnected() + (n++) + "》");
                        flag = true;
                        requestBuffer.flip();
                        System.out.println(getString(requestBuffer));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            if (flag) {
                String requestInfo = getString(requestBuffer);
                String type = requestInfo.split(" ")[0];
                Map<String, String> requestMap = stringToMap(requestInfo.split("\n"));
                switch (type) {
                    case "OPTIONS":
                        handleOption(requestMap);
                        break;
                    case "DESCRIBE":
                        handleDsecribe(requestMap);
                        break;
                    case "SETUP":
                        handleSetup(requestMap);
                        break;
                    case "PLAY":
                        handlePlay(requestMap);
                        break;
                }
                requestBuffer.clear();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            flag = false;
        }
    }

    private void handleProbe() throws IOException {
        System.out.println("开启RTP/RTCP端口");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    openRtcport(6971);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    openRtport(6970);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleOption(Map<String, String> requestMap) {
        try {
            String CSeq = StringUtils.trim(requestMap.get("CSeq"));
            Date date = new Date();
            String stringDate = sdf.format(date);
            String str =
                    "RTSP/1.0 200 OK\r\n" +
                            "CSeq: " + CSeq + "\r\n" +
                            "Date: " + stringDate + " GMT\r\n" +
                            "Public: OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, GET_PARAMETER, SET_PARAMETER\r\n" +
                            "\r\n";
            responseBuffer = ByteBuffer.wrap(str.getBytes());
            responseBuffer.clear();
            System.out.println("》》响应数据流《《");
            while (responseBuffer.hasRemaining()) {
                System.out.println(getString(responseBuffer));
                socketRTSPChannelClient.write(responseBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDsecribe(Map<String, String> requestMap) {
        try {
            String CSeq = StringUtils.trim(requestMap.get("CSeq"));
            Date date = new Date();
            String stringDate = sdf.format(date);
            String str =
                    "RTSP/1.0 200 OK\r\n" +
                            "CSeq: " + CSeq + "\r\n" +
                            "Date: " + stringDate + " GMT\r\n" +
                            "Content-Base: rtsp://172.27.148.59:1234/f.mkv\r\n" +
                            "Content-Type: application/sdp\r\n" +
                            "Content-Length: 663\r\n" +
                            "\r\n" +
                            "v=0\r\n" +
                            "o=- 1543727961399959 1 IN IP4 172.27.148.59\r\n" +
                            "s=Matroska video+audio+(optional)subtitles, streamed by the LIVE555 Media Server\r\n" +
                            "i=f.mkv\r\n" +
                            "t=0 0\r\n" +
                            "a=tool:LIVE555 Streaming Media v2018.11.26\r\n" +
                            "a=type:broadcast\r\n" +
                            "a=control:*\r\n" +
                            "a=range:npt=0-\r\n" +
                            "a=x-qt-text-nam:Matroska video+audio+(optional)subtitles, streamed by the LIVE555 Media Server\r\n" +
                            "a=x-qt-text-inf:f.mkv\r\n" +
                            "m=video 0 RTP/AVP 96\r\n" +
                            "c=IN IP4 0.0.0.0\r\n" +
                            "b=AS:500\r\n" +
                            "a=rtpmap:96 H264/90000\r\n" +
                            "a=fmtp:96 packetization-mode=1;profile-level-id=640033;sprop-parameter-sets=Z2QAM6w04kBQBF+aEAAZdPAExLQI8YMYmA==,aO6yyLA=\r\n" +
                            "a=control:track1\r\n" +
                            "m=audio 0 RTP/AVP 97\r\n" +
                            "c=IN IP4 0.0.0.0\r\n" +
                            "b=AS:48\r\n" +
                            "a=rtpmap:97 AC3/48000\r\n" +
                            "a=control:track2\r\n" +
                            "\r\n";
            responseBuffer = ByteBuffer.wrap(str.getBytes());
            System.out.println("》》响应数据流《《");
            while (responseBuffer.hasRemaining()) {
                System.out.println(getString(responseBuffer));
                socketRTSPChannelClient.write(responseBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            responseBuffer.clear();
        }
    }

    int i = 0;

    private void handleSetup(Map<String, String> requestMap) {
        Date date = new Date();
        String stringDate = sdf.format(date);
        String CSeq = StringUtils.trim(requestMap.get("CSeq"));
        final String client_port = StringUtils.trim(requestMap.get("Transport").split("client_port=")[1]);
        try {
            String str =
                    "RTSP/1.0 200 OK\r\n" +
                            "CSeq: " + CSeq + "\r\n" +
                            "Date: " + stringDate + " GMT\r\n" +
                            "Transport: RTP/AVP;unicast;destination=172.27.148.59;source=172.27.148.59;" + "client_port=" + client_port + ";server_port=6970-6971\r\n" +
                            "Session: CEF9FCAB;timeout=65\r\n" +
                            "\r\n";
            responseBuffer = ByteBuffer.wrap(str.getBytes());
            System.out.println("》》响应数据流《《");
            while (responseBuffer.hasRemaining()) {
                System.out.println(getString(responseBuffer));
                socketRTSPChannelClient.write(responseBuffer);
            }
            if (i++ == 0) {
                handleProbe();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            responseBuffer.clear();
        }
    }

    private void handlePlay(Map<String, String> requestMap) {
        try {
            int[] rtcp_array = {
                    0x80c80006,
                    0x68ccd77f,
                    0xdfb0812d,
                    0x8f995ee1,
                    0x8f995ee1,
                    0x3011e0c4,
                    0x00000000,
                    0x00000000,
                    0x81ca0006,
                    0x68ccd77f,
                    0x010f4445,
                    0x534b544f,
                    0x502d4544,
                    0x52375155,
                    0x48000000};
            byte[] rtcp_bs = itob(rtcp_array);
            ByteBuffer rtcp_report = ByteBuffer.allocate(1024);
            rtcp_report.put(rtcp_bs);
            rtcp_report.flip();
            while (rtcp_report.hasRemaining()) {
                System.out.println("发送RTCP：" + rtcp_report.toString());
                serverRTCPChannel.write(rtcp_report);

            }

            Date date = new Date();
            String stringDate = sdf.format(date);
            String CSeq = StringUtils.trim(requestMap.get("CSeq"));
            String str = "RTSP/1.0 200 OK\r\n" +
                    "CSeq: " + CSeq + "\r\n" +
                    "Date: " + stringDate + " GMT\r\n" +
                    "Range: npt=0.000-\r\n" +
                    "Session:  CEF9FCAB\r\n" +
                    "RTP-Info: url=rtsp://172.27.148.59/f.mkv/track1;seq=11835;rtptime=806482728,url=rtsp://127.0.0.1/f.mkv/track2;seq=4144;rtptime=3002712689\r\n" +
                    "\r\n";
            responseBuffer = ByteBuffer.wrap(str.getBytes());
            System.out.println("》》响应数据流《《");
            while (responseBuffer.hasRemaining()) {
                System.out.println(getString(responseBuffer));
                socketRTSPChannelClient.write(responseBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] itob(int[] array) {
        int bytelength = array.length * 4;//长度
        byte[] bs = new byte[bytelength];//开辟数组
        int curint = 0;
        for (int j = 0, k = 0; j < array.length; j++, k += 4) {
            curint = array[j];
            bs[k] = (byte) ((curint >> 24) & 0b1111_1111);//右移4位，与1作与运算
            bs[k + 1] = (byte) ((curint >> 16) & 0b1111_1111);
            bs[k + 2] = (byte) ((curint >> 8) & 0b1111_1111);
            bs[k + 3] = (byte) ((curint >> 0) & 0b1111_1111);
        }
        return bs;
    }

    private void openRtcport(int server_port) throws IOException {
        serverRTCPChannel = DatagramChannel.open();
        serverRTCPChannel.configureBlocking(true);
        serverRTCPChannel.socket().bind(new InetSocketAddress(server_port));
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        SocketAddress rtcpAddress = serverRTCPChannel.receive(byteBuffer);
        System.out.println("RTCP:" + rtcpAddress + "---" + getString(byteBuffer));
        serverRTCPChannel.connect(rtcpAddress);
    }

    SocketAddress rtcpAddress;

    private void openRtport(int server_port) throws IOException {
        DatagramChannel serverRTPChannel = DatagramChannel.open();
        serverRTPChannel.configureBlocking(true);
        serverRTPChannel.socket().bind(new InetSocketAddress(server_port));
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        SocketAddress rtpAddress = serverRTPChannel.receive(byteBuffer);
        System.out.println("RTP:" + rtcpAddress + "---" + getString(byteBuffer));
        serverRTPChannel.connect(rtpAddress);
    }


    private static String getString(ByteBuffer buffer) {
        Charset charset = null;
        CharsetDecoder decoder = null;
        CharBuffer charBuffer = null;
        try {
            charset = Charset.forName("UTF-8");
            decoder = charset.newDecoder();
            charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
            return charBuffer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private static Map<String, String> stringToMap(String[] str) {
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i < str.length - 1; i++) {
            String[] res = str[i].split(": ");
            if (res.length == 2) {
                map.put(res[0], res[1]);
            }
        }
        return map;
    }

    public static void main(String[] args) throws IOException {

        new RtspHandler().testSocket();
        SimpleDateFormat sdf = new SimpleDateFormat("EE, MMM dd yyyy HH:mm:ss", Locale.ENGLISH);
        String a = " 123";
        String b = " eerr";
        String client_port = "1234";
        String str =
                "RTSP/1.0 200 OK\r\n" +
                        "CSeq: " + a + "\r\n" +
                        "Date: " + b + " GMT\r\n" +
                        "Transport: RTP/AVP;unicast;destination=127.0.0.1;source=127.0.0.1;" + "client_port=" + client_port + ";server_port=6970-6971\r\n" +
                        "Session: CEF9FCAB;timeout=65\r\n" +
                        "\r\n";
        Date date = new Date();
        String stringDate = sdf.format(date);
        System.out.println(str);
    }

    private void testSocket() throws IOException {
        ServerSocketChannel listnChannel = ServerSocketChannel.open();
        listnChannel.socket().bind(new InetSocketAddress(7788));
        listnChannel.configureBlocking(true);
        listnChannel.accept();

        DatagramChannel channel = DatagramChannel.open();
        DatagramChannel channel2 = DatagramChannel.open();
        channel.configureBlocking(false);
        channel2.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(7777));
        channel2.socket().bind(new InetSocketAddress(7779));
    }
}
