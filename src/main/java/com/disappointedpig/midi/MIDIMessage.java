package com.disappointedpig.midi;


import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.internal_events.PacketEvent;
import com.disappointedpig.midi.utility.DataBuffer;
import com.disappointedpig.midi.utility.DataBufferReader;
import com.disappointedpig.midi.utility.OutDataBuffer;

public class MIDIMessage extends RTPMessage {

    private Boolean valid;
    private DataBuffer m;

    private boolean firstHasDeltaTime;

    private int channel_status;
    int channel;
    private int note;
    private int velocity;

    public static MIDIMessage newUsing(int cs, int c, int n, int v) {
        MIDIMessage m = new MIDIMessage();
        m.createNote(cs,c,n,v);
        return m;
    }

    public static MIDIMessage newUsing(Bundle m) {
        return newUsing(   m.getInt(com.disappointedpig.midi.MIDIConstants.MSG_COMMAND,0x09),
                    m.getInt(com.disappointedpig.midi.MIDIConstants.MSG_CHANNEL,0),
                    m.getInt(com.disappointedpig.midi.MIDIConstants.MSG_NOTE,0),
                    m.getInt(com.disappointedpig.midi.MIDIConstants.MSG_VELOCITY,0));
    }

    public Bundle toBundle() {
        Bundle midi = new Bundle();
        midi.putInt(com.disappointedpig.midi.MIDIConstants.MSG_COMMAND,this.channel_status);
        midi.putInt(com.disappointedpig.midi.MIDIConstants.MSG_CHANNEL,this.channel);
        midi.putInt(com.disappointedpig.midi.MIDIConstants.MSG_NOTE, this.note);
        midi.putInt(com.disappointedpig.midi.MIDIConstants.MSG_VELOCITY, this.velocity);
        return midi;
    }


    public MIDIMessage() {
    }

    public boolean parseMessage(PacketEvent packet) {
        this.valid = false;
        parse(packet);
        final DataBufferReader reader = new DataBufferReader();
        final DataBuffer rawPayload = new DataBuffer(payload, payload_length);

        // payload should contain command + journal
        int block4 = reader.read8(rawPayload);

        System.out.println("BLOCK4");
        System.out.println(block4);

        channel_status = block4 >> 4;
        channel = block4 & 0xf;
        int block5 = reader.read8(rawPayload);
        note = block5 & 0x7f;
        int block6 = reader.read8(rawPayload);
        velocity = block6 & 0x7f;

        this.valid = true;

        System.out.println("PARSE!");

        Log.d("MIDIMessage", "cs:" + channel_status + " c:" + channel + " n:" + note + " v" + velocity);
        return true;
    }



    public void createNote(int channel_status, int channel, int note, int velocity) {

        System.out.println("CreateNote");

        System.out.println("channel "+channel);

        this.channel_status = channel_status;
        this.channel = channel;
        this.note = note;
        this.velocity = velocity;
    }
    public void createNote(int note, int velocity) {
        this.note = note;
        this.velocity = velocity;
    }

    public Boolean isValid() {
        return valid;
    }

    public byte[] generateBuffer() {
        OutDataBuffer buffer = generatePayload();

        /* original comment from DP:

        // TODO : this doesn't handle channel_status or channel correctly
        //        buffer.write8(0x00);

        */
        /*

        so they did: `buffer.write16(0x0390);`

        Note from Reiszecke: Not sure if what I am doing is clean so it's still a potential todo
        but inspecting the network traffic in wireshark made me come up with the workaround you see below

        No idea what the prefix is for or if I am correct at calling it that.
        Message type is 9 for note and 11 for CC which i shift. Then I add the channel number
        to it which seems to work for me.

        It gives values like 0x90 144 or 0xB1 for CC on Channel 2 etc.

        */



        int prefix = 0x0300;
        int messageType = channel_status << 4;

        int block4 = prefix+messageType+channel;


        System.out.println("BLOCK4 "+block4);
        buffer.write16(block4);


        buffer.write8(note);
        buffer.write8(velocity);

        System.out.println("velocity written: "+velocity);


        return buffer.toByteArray();
    }
}