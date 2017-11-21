package org.jgroups;

import org.jgroups.util.Buffer;
import org.jgroups.util.Streamable;

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

    /** Creates an instance of a message */
    Supplier<? extends Message> create();

    /** Returns the destination address to send the message to. A null value sends the message to all cluster members */
    Address                     getDest();

    /** Sets the destination address to send the message to. A null value sends the message to all cluster members */
    <T extends Message> T       setDest(Address new_dest);

    /** Returns the address of the sender */
    Address                     getSrc();

    /** Sets the address of the sender of this message */
    <T extends Message> T       setSrc(Address new_src);

    /** Adds a header to the message */
    <T extends Message> T       putHeader(short id, Header hdr);

    /** Gets a header from the message */
    <T extends Header> T        getHeader(short id);

    /** Returns a hashmap of all header IDs and their associated headers */
    Map<Short,Header>           getHeaders();

    /** Returns the number of headers */
    int                         getNumHeaders();

    /** Returns a pretty-printed string of the headers */
    String                      printHeaders();

    /** Sets one or more flags */
    <T extends Message> T       setFlag(Flag... flags);

    /** Sets the flags as a short; this way, multiple flags can be set in one operation
     * @param flag The flag to be set (as a short)
     * @param is_transient True if the flag is transient, false otherwise
     */
    <T extends Message> T       setFlag(short flag, boolean is_transient);

    /** Sets one or more transient flags. Transient flags are not marshalled */
    <T extends Message> T       setFlag(TransientFlag... flags);

    /** Atomically sets a transient flag if not set. Returns true if the flags was set, else false (already set) */
    boolean                     setFlagIfAbsent(TransientFlag flag);

    /** Returns the flags as an or-ed short
     * @param is_transient Returns transient flags if true, else regular flags
     */
    short                       getFlags(boolean is_transient);

    /** Removes a number of flags from the message. No-op for a flag if it is not set */
    <T extends Message> T       clearFlag(Flag... flags);

    /** Removes a number of transient flags from the message. No-op for a flag if it is not set */
    <T extends Message> T       clearFlag(TransientFlag... flags);

    boolean                     isFlagSet(Flag flag);

    boolean                     isFlagSet(TransientFlag flag);





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

    <T extends Message> T buffer(byte[] b);

    <T extends Message> T buffer(Buffer b);



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
    <T extends Message> T setBuffer(byte[] b);

    <T extends Message> T setBuffer(byte[] b, int offset, int length);

    <T extends Message> T setBuffer(Buffer buf);

    <T extends Message> T setObject(Object obj);

    <T extends Object> T getObject();

    <T extends Object> T getObject(ClassLoader loader);





    <T extends Message> T copy();

    <T extends Message> T copy(boolean copy_buffer);

    <T extends Message> T copy(boolean copy_buffer, boolean copy_headers);

    <T extends Message> T copy(boolean copy_buffer, short starting_id);

    <T extends Message> T copy(boolean copy_buffer, short starting_id, short... copy_only_ids);

    <T extends Message> T makeReply();

    String toString();



    void writeToNoAddrs(Address src, DataOutput out, short... excluded_headers) throws Exception;


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
