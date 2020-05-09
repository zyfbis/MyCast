package com.example.mycast.rtsp;

import java.io.IOException;

public class MicStream extends AudioStream {

    public MicStream() {
        mPacketizer = new AACLATMPacketizer();
    }

    public synchronized void start() throws IllegalStateException, IOException {
        if (!mStreaming) {
            super.start();
        }
    }

    /**
     * Configures the stream. You need to call this before calling {@link #getSessionDescription()} to apply
     * your configuration of the stream.
     */
    public synchronized void configure() throws IllegalStateException, IOException {
        super.configure();
    }

    @Override
    public String getSessionDescription() throws IllegalStateException {
        return "m=audio " + String.valueOf(getDestinationPorts()[0]) + " RTP/AVP 96\r\n" +
                "a=rtpmap:96 mpeg4-generic/" + "44100" + "\r\n" +
                "a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=" + "1208" + "; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n";
    }
}
