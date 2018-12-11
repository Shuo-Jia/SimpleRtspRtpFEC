package org.bjtu.iplab.js.rtp;

public class FECPacket {
    //FEC-Header
    private static int FECHEADER_SIZE = 10;
    private byte[] fecHeader;
    private int extension = 0;
    private int longMask = 0;

    //RTP-Informationen from XOR
    private int RTPPadding;
    private int RTPExtension;
    private int RTPCC;
    private int RTPMarker;
    private int RTPPayloadType;
    private int RTPTimeStamp;
    private int RTPLength;

    //Referenz-SequenceNumber
    private int SNBase;

    //FEC-Level0-Header
    private static int FECL0HEADER_SIZE = 4;
    private byte[] fecL0Header;
    private int L0protLength;
    private int L0mask;

    //FEC-Level0-Payload
    private byte[] L0payload;

    // 使用两个数据包构造FECpacket
    FECPacket(RTPacket p1, RTPacket p2, int SNBaseIn) {

        //get RTP-information and XOR
        RTPPadding = p1.getPadding() ^ p2.getPadding();
        RTPExtension = p1.getExtension() ^ p2.getExtension();
        RTPCC = p1.getCc() ^ p2.getCc();
        RTPMarker = p1.getMarker() ^ p2.getMarker();
        RTPPayloadType = p1.getPayloadType() ^ p2.getPayloadType();
        RTPTimeStamp = p1.getTimeStamp() ^ p2.getTimeStamp();
        RTPLength = p1.getLength() ^ p2.getLength();

        //set SNBase
        SNBase = SNBaseIn;

        //build FEC-Header
        fecHeader = new byte[FECHEADER_SIZE];
        fecHeader[0] = (byte) (extension << 7);
        fecHeader[0] |= (byte) (longMask << 6);
        fecHeader[0] |= (byte) (RTPPadding << 5);
        fecHeader[0] |= (byte) (RTPExtension << 4);
        fecHeader[0] |= (byte) (RTPCC);
        fecHeader[1] = (byte) (RTPMarker << 7);
        fecHeader[1] |= (byte) RTPPayloadType;
        fecHeader[2] = (byte) (SNBase >> 8);
        fecHeader[3] = (byte) (SNBase & 0xFF);
        fecHeader[4] = (byte) (RTPTimeStamp >> 24);
        fecHeader[5] = (byte) (RTPTimeStamp >> 16);
        fecHeader[6] = (byte) (RTPTimeStamp >> 8);
        fecHeader[7] = (byte) (RTPTimeStamp & 0xFF);
        fecHeader[8] = (byte) (RTPLength >> 8);
        fecHeader[9] = (byte) (RTPLength & 0xFF);

        //build FEC-Level0-Header
        L0protLength = Math.max(p1.getPayloadLength(), p2.getPayloadLength());
        L0mask = (1 << (p1.getSequenceNumber() - SNBaseIn)) | (1 << (p2.getSequenceNumber() - SNBaseIn));
        fecL0Header = new byte[FECL0HEADER_SIZE];
        fecL0Header[0] = (byte) (L0protLength >> 8);
        fecL0Header[1] = (byte) (L0protLength & 0xFF);
        fecL0Header[2] = (byte) (L0mask >> 8);
        fecL0Header[3] = (byte) (L0mask & 0xFF);

        //helpfulbuffer insert with files
        byte[] buf1 = p1.getPayload();
        byte[] buf2 = p2.getPayload();

        //create payLoad
        L0payload = new byte[L0protLength];
        for (int i = 0; i < L0protLength; i++) {
            L0payload[i] = (byte) (buf1[i] ^ buf2[i]);
        }
    }


    // --------------------------
    // 从收到的FEC数据包反序列化出FECpacket对象，用于后续提取该数据宝的信息
    // --------------------------
    public FECPacket(byte[] packet, int packet_size) {
        //Paket isn't a FEC-paket
        if (packet_size <= FECHEADER_SIZE + FECL0HEADER_SIZE) return;

        //read FEC-Header
        fecHeader = new byte[FECHEADER_SIZE];
        System.arraycopy(packet, 0, fecHeader, 0, FECHEADER_SIZE);

        //read the header and set the var
        extension = unsigned_int(fecHeader[0] & 128);
        longMask = fecHeader[0] & 64;
        RTPPadding = fecHeader[0] & 32;
        RTPExtension = fecHeader[0] & 16;
        RTPCC = fecHeader[0] & 15;
        RTPMarker = unsigned_int(fecHeader[1] & 128);
        RTPPayloadType = unsigned_int(fecHeader[1] & 127);
        RTPTimeStamp = unsigned_int(fecHeader[7]) + 256 * unsigned_int(fecHeader[6]) + 65536 * unsigned_int(fecHeader[5]) + 16777216 * unsigned_int(fecHeader[4]);
        RTPLength = 256 * unsigned_int(fecHeader[8]) + unsigned_int(fecHeader[9]);
        SNBase = 256 * unsigned_int(fecHeader[2]) + unsigned_int(fecHeader[3]);

        //read L0-Header
        fecL0Header = new byte[FECL0HEADER_SIZE];
        System.arraycopy(packet, FECHEADER_SIZE, fecL0Header, 0, FECL0HEADER_SIZE);

        //read the header and set the var
        L0protLength = 256 * unsigned_int(fecL0Header[0]) + unsigned_int(fecL0Header[1]);
        L0mask = 256 * unsigned_int(fecL0Header[2]) + unsigned_int(fecL0Header[3]);

        //read L0Payload
        L0payload = new byte[L0protLength];
        for (int i = 0; i < L0protLength; i++) {
            L0payload[i] = packet[i + FECHEADER_SIZE + FECL0HEADER_SIZE];
        }
    }


    //重建丢失的数据包
    public RTPacket restoreL0(RTPacket inPacket) {
        int seqNr;
        //get the sequencenumber
        int index = inPacket.getSequenceNumber();
        if ((index & 0x01) == 0) {
            seqNr = index + 1;
        } else {
            seqNr = index - 1;
        }

        //restore var
        int timeStamp = RTPTimeStamp ^ inPacket.getTimeStamp();
        int payloadType = RTPPayloadType ^ inPacket.getPayloadType();
        int data_length = (RTPLength ^ inPacket.getLength()) - RTPacket.HEADER_SIZE;

        //restore payload
        byte[] bufin = inPacket.getPayload();
        byte[] bufout = new byte[15000];
        for (int i = 0; i < data_length; i++) {
            bufout[i] = (byte) (L0payload[i] ^ bufin[i]);
        }

        //create out-Paket
        RTPacket outPacket = new RTPacket(payloadType, seqNr, timeStamp, bufout, data_length);

        //return the restored paket
        return outPacket;
    }

    public int[] getNumbersOfL0Packets() {
        int count = 0;
        for (int i = 0; i < 16; i++) {
            if (((L0mask >> i) & 1) == 1) count++;
        }

        //get seqnumbers
        int[] sequNumbers = new int[count];
        for (int i = 0, j = 0; i < 16; i++) {
            if (((L0mask >> i) & 1) == 1) {
                sequNumbers[j] = SNBase + i;
                j++;
            }
        }

        return sequNumbers;
    }

    public void printFECheader() {
        System.out.print("FEC-Header: ");
        for (int i = 0; i < (FECHEADER_SIZE); i++)                        //Byte-Schleife
        {
            for (int j = 7; j >= 0; j--)                                    //Bit-Schleife
            {
                if (((1 << j) & fecHeader[i]) != 0) System.out.print("1");
                else System.out.print("0");
            }
            System.out.print(" ");
        }
        System.out.println();
    }

    public void printFECL0header() {
        System.out.print("L0 -Header: ");

        for (int i = 0; i < (FECL0HEADER_SIZE); i++) //Byte-Schleife
        {
            for (int j = 7; j >= 0; j--)                    //Bit-Schleife
            {
                if (((1 << j) & fecL0Header[i]) != 0) System.out.print("1");
                else System.out.print("0");
            }
            System.out.print(" ");
        }
        System.out.println();
    }

    int getPacket(byte[] packet) {
        //construct the packet = FECHEADER + FECL0HEADER + payload
        for (int i = 0; i < FECHEADER_SIZE; i++) packet[i] = fecHeader[i];
        for (int i = 0; i < FECL0HEADER_SIZE; i++) packet[i + FECHEADER_SIZE] = fecL0Header[i];
        for (int i = 0; i < L0protLength; i++) packet[i + FECHEADER_SIZE + FECL0HEADER_SIZE] = L0payload[i];
        //return total size of the packet
        return (FECHEADER_SIZE + FECL0HEADER_SIZE + L0protLength);
    }

    //return the unsigned value of 8-bit integer nb
    private static int unsigned_int(int nb) {
        if (nb >= 0) return (nb);
        else return (256 + nb);
    }
}
