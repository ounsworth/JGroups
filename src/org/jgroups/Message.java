package org.jgroups;

import org.jgroups.util.Buffer;
import org.jgroups.util.Streamable;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Interface for all message types.
 * @author Bela Ban
 * @since  5.0
 */
public interface Message extends Streamable, Constructable<Message> {

    // The type of the message. Cannot be an enum, as users can register additional types
    byte BYTES_MSG=1, OBJ_MSG=2;

    /** Returns the type of the message, e.g. BYTES_MSG, OBJ_MSG etc */
    byte getType();

    static boolean isFlagSet(short flags, Flag flag) {
        return flag != null && ((flags & flag.value()) == flag.value());
    }

    static boolean isTransientFlagSet(short flags, TransientFlag flag) {
        return flag != null && (flags & flag.value()) == flag.value();
    }

    static String flagsToString(short flags) {
        StringBuilder sb=new StringBuilder();
        boolean first=true;

        Flag[] all_flags=Flag.values();
        for(Flag flag: all_flags) {
            if(isFlagSet(flags, flag)) {
                if(first)
                    first=false;
                else
                    sb.append("|");
                sb.append(flag);
            }
        }
        return sb.toString();
    }

    static String transientFlagsToString(short flags) {
        StringBuilder sb=new StringBuilder();
        boolean first=true;

        TransientFlag[] all_flags=TransientFlag.values();
        for(TransientFlag flag: all_flags) {
            if(isTransientFlagSet(flags, flag)) {
                if(first)
                    first=false;
                else
                    sb.append("|");
                sb.append(flag);
            }
        }
        return sb.toString();
    }

    Supplier<? extends Message> create();

    Address getDest();

    Address dest();

    <T extends Message> T setDest(Address new_dest);

    Message dest(Address new_dest);

    Address getSrc();

    Address src();

    Message setSrc(Address new_src);

    Message src(Address new_src);

    boolean hasArray();

    int     getOffset();

    int     offset();

    int     getLength();

    int     length();

    /**
     * Returns a <em>reference</em> to the payload (byte buffer). Note that this buffer should not be
     * modified as we do not copy the buffer on copy() or clone(): the buffer of the copied message
     * is simply a reference to the old buffer.<br/>
     * Even if offset and length are used: we return the <em>entire</em> buffer, not a subset.
     */
    byte[]  getRawBuffer();

    byte[]  rawBuffer();

    byte[]  buffer();

    Buffer buffer2();

    Message buffer(byte[] b);

    Message buffer(Buffer b);

    int     getNumHeaders();

    int     numHeaders();

    /**
     * Returns a copy of the buffer if offset and length are used, otherwise a reference.
     * @return byte array with a copy of the buffer.
     */
    byte[] getBuffer();

    Buffer getBuffer2();

    /**
     * Sets the buffer.<p/>
     * Note that the byte[] buffer passed as argument must not be modified. Reason: if we retransmit the
     * message, it would still have a ref to the original byte[] buffer passed in as argument, and so we would
     * retransmit a changed byte[] buffer !
     */
    Message setBuffer(byte[] b);

    Message setBuffer(byte[] b, int offset, int length);

    Message setBuffer(Buffer buf);

    /**
     * Returns a reference to the headers hashmap, which is <em>immutable</em>. Any attempt to modify
     * the returned map will cause a runtime exception
     */
    Map<Short,Header> getHeaders();

    String printHeaders();

    Message setObject(Object obj);

    <T extends Object> T getObject();

    <T extends Object> T getObject(ClassLoader loader);

    Message setFlag(Flag... flags);

    Message setTransientFlag(TransientFlag... flags);

    Message setFlag(short flag);

    Message setTransientFlag(short flag);

    short getFlags();

    short getTransientFlags();

    Message clearFlag(Flag... flags);

    Message clearTransientFlag(TransientFlag... flags);

    boolean isFlagSet(Flag flag);

    boolean isTransientFlagSet(TransientFlag flag);

    boolean setTransientFlagIfAbsent(TransientFlag flag);

    Message putHeader(short id, Header hdr);

    <T extends Header> T getHeader(short id);

    <T extends Header> T getHeader(short... ids);

    Message copy();

    Message copy(boolean copy_buffer);

    Message copy(boolean copy_buffer, boolean copy_headers);

    Message copy(boolean copy_buffer, short starting_id);

    Message copy(boolean copy_buffer, short starting_id, short... copy_only_ids);

    <T extends Message> T makeReply();

    String toString();

    String printObjectHeaders();

    void writeTo(DataOutput out) throws Exception;

    void writeToNoAddrs(Address src, DataOutput out, short... excluded_headers) throws Exception;

    void readFrom(DataInput in) throws Exception;

    /**
     * Returns the exact size of the marshalled message. Uses method size() of each header to compute
     * the size, so if a Header subclass doesn't implement size() we will use an approximation.
     * However, most relevant header subclasses have size() implemented correctly. (See
     * org.jgroups.tests.SizeTest).<p/>
     * The return type is a long as this is the length of the payload ({@link #getLength()}) plus metadata (e.g. flags,
     * headers, source and dest addresses etc). Since the largest payload can be Integer.MAX_VALUE, adding the metadata
     * might lead to an int overflow, that's why we use a long.
     * @return The number of bytes for the marshalled message
     */
    int size();

    // =============================== Flags ====================================
    enum Flag {
        OOB((short)            1),           // message is out-of-band
        DONT_BUNDLE(   (short)(1 <<  1)),    // don't bundle message at the transport
        NO_FC(         (short)(1 <<  2)),    // bypass flow control
        NO_RELIABILITY((short)(1 <<  4)),    // bypass UNICAST(2) and NAKACK
        NO_TOTAL_ORDER((short)(1 <<  5)),    // bypass total order (e.g. SEQUENCER)
        NO_RELAY(      (short)(1 <<  6)),    // bypass relaying (RELAY)
        RSVP(          (short)(1 <<  7)),    // ack of a multicast (https://issues.jboss.org/browse/JGRP-1389)
        RSVP_NB(       (short)(1 <<  8)),    // non blocking RSVP
        INTERNAL(      (short)(1 <<  9)),    // for internal use by JGroups only, don't use !
        SKIP_BARRIER(  (short)(1 << 10));    // passing messages through a closed BARRIER

        final short value;
        Flag(short value) {this.value=value;}

        public short value() {return value;}
    }

    // =========================== Transient flags ==============================
    enum TransientFlag {
        OOB_DELIVERED( (short)(1)),
        DONT_LOOPBACK( (short)(1 << 1));   // don't loop back up if this flag is set and it is a multicast message

        final short value;
        TransientFlag(short flag) {value=flag;}

        public short value() {return value;}
    }
}
