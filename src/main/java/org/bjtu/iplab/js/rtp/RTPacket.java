package org.bjtu.iplab.js.rtp;

public class RTPacket {
    public static int HEADER_SIZE = 12;

    //Fields that compose the RTP header
    private int version;
    private int padding;
    private int extension;
    private int cc;
    private int marker;
    private int payloadType;
    private int sequenceNumber;
    private int timeStamp;
    private int ssrc;

    //Bitstream of the RTP header
    private byte[] header;

    //size of the RTP payload
    private int payloadSize;
    //Bitstream of the RTP payload
    private byte[] payload;

    //RTPacket的构造器，由发送数据包+包头填充成RTPacket，用于后续的序列化成byte[]数据发送出去
    public RTPacket(int pType, int frameNb, int time, byte[] data, int data_length) {
        //fill by default header fields:
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        ssrc = 0;

        //fill changing header fields:
        sequenceNumber = frameNb;
        timeStamp = time;
        payloadType = pType;

        //build the header bistream:
        //--------------------------
        header = new byte[HEADER_SIZE];

        //fill the header array of byte with RTP header fields
        header[0] = (byte) 128;
        header[1] = (byte) pType;

        header[2] = (byte) (sequenceNumber >> 8);
        header[3] = (byte) (sequenceNumber & 0xFF);

        header[4] = (byte) (timeStamp >> 24);
        header[5] = (byte) (timeStamp >> 16);
        header[6] = (byte) (timeStamp >> 8);
        header[7] = (byte) (timeStamp & 0xFF);

        header[8] = header[9] = header[10] = 0;
        header[11] = 1;

        //fill the payload bitstream:
        payloadSize = data_length;
        payload = new byte[data_length];

        //fill payload array of byte from data     payload =(given in parameter of the constructor)
        System.arraycopy(data, 0, payload, 0, data_length);
    }

    //--------------------------
    //RTPacket构造器，由收到的byte[]数据反序列化成RTPacket，用于提取RTPacket数据
    //--------------------------
    public RTPacket(byte[] packet, int packetSize) {
        //fill default fields:
        version = 2;
        padding = 0;
        extension = 0;
        cc = 0;
        marker = 0;
        ssrc = 0;

        //check if total packet size is lower than the header size
        if (packetSize >= HEADER_SIZE) {
            //get the header bitsream:
            header = new byte[HEADER_SIZE];
            for (int i = 0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            //get the payload bitstream:
            payloadSize = packetSize - HEADER_SIZE;
            payload = new byte[payloadSize];
            for (int i = HEADER_SIZE; i < packetSize; i++) {
                payload[i - HEADER_SIZE] = packet[i];
            }

            //interpret the changing fields of the header:
            payloadType = header[1] & 127;
            sequenceNumber = unsigned_int(header[3]) + 256 * unsigned_int(header[2]);
            timeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6]) + 65536 * unsigned_int(header[5]) + 16777216 * unsigned_int(header[4]);
        }
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getPayloadLength() {
        return payloadSize;
    }

    public int getLength() {
        return (payloadSize + HEADER_SIZE);
    }


    public byte[] getPacket() {
        //construct the packet = header + payload
        byte[] packet = new byte[HEADER_SIZE + payloadSize];
        for (int i = 0; i < HEADER_SIZE; i++)
            packet[i] = header[i];
        for (int i = 0; i < payloadSize; i++)
            packet[i + HEADER_SIZE] = payload[i];
        //return total size of the packet
        return packet;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public int getPadding() {
        return padding;
    }

    public int getExtension() {
        return extension;
    }

    public int getCc() {
        return cc;
    }

    public int getMarker() {
        return marker;
    }

    //print headers without the SSRC
    public void printHeader() {
        System.out.print("RTP-Header: ");
        for (int i = 0; i < (HEADER_SIZE - 4); i++) {
            for (int j = 7; j >= 0; j--)
                if (((1 << j) & header[i]) != 0)
                    System.out.print("1");
                else
                    System.out.print("0");
            System.out.print(" ");
        }
        System.out.println();
    }

    //return the unsigned value of 8-bit integer nb
    private static int unsigned_int(int nb) {
        if (nb >= 0)
            return (nb);
        else
            return (256 + nb);
    }
}
