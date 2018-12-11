一个简单的附带了FEC功能的rtsp/rtp协议的实现

1、rtsp和rtp的实现是分开的，需独立运行  
(1)rtsp作为服务器实现，需配合vlc等rtsp客户端调试运行  
(2)rtp有完整的发送端-接收端-FEC冗余编解码的实现，可直接调试运行

2、该示例只是简单的模拟rtsp/rtp协议，并不代表实际的媒体传输逻辑，
而且细节部分往往都是填充的自定义的默认值，需要使用的话，请根据实际媒体数据信息进行填充

3、fec编解码算法为模拟的最简单的两个数据包XOR处理

4、rtsp部分的实现感谢https://www.wolfcstech.com/2017/09/05/live555_src_analysis_play/提供的理论支持；fec部分感谢https://github.com/EricEricson/RTSP-Streaming-FEC/提供的开源支持

==========================================================================================

Implementation of a simple rtsp/rtp protocol with FEC function
1. The implementation of RTSP and RTP is separate and needs to run independently.
(1) RTSP is implemented as a server, which needs to be debugged and run with RTSP client such as VLC
(2) RTP has complete redundant coding and decoding of sender-receiver-FEC, which can be debugged and run directly.
2. This example is only a simple simulation of rtsp/rtp protocol, and does not represent the actual media transmission logic.
And the details are often filled with custom default values. If you need to use them, please fill them according to the actual media data information.
3. The FEC encoding and decoding algorithm is the simplest XOR processing of two simulated data packets.
4. The implementation of RTSP is thankful for the theoretical support provided by  
https://www.wolfcstech.com/2017/09/05/live555_src_analysis_play/  
while the implementation of FEC is thankful for the open source support provided by  
https://github.com/EricEricson/RTSP-Streaming-FEC/