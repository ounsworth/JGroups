
package org.jgroups;


import org.jgroups.conf.ClassConfigurator;
import org.jgroups.util.Buffer;
import org.jgroups.util.Headers;
import org.jgroups.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Map;

/**
 * A Message encapsulates data sent to members of a group. It contains among other things the
 * address of the sender, the destination address, a payload (byte buffer) and a list of headers.
 * Headers are added by protocols on the sender side and removed by protocols on the receiver's side.
 * <p>
 * The byte buffer can point to a reference, and we can subset it using index and length. However,
 * when the message is serialized, we only write the bytes between index and length.
 *
 * @since 2.0
 * @author Bela Ban
 */
public abstract class BaseMessage implements Message {
    protected Address           dest_addr;
    protected Address           src_addr;
    protected volatile Header[] headers;
    protected volatile short    flags;
    protected volatile byte     transient_flags; // transient_flags is neither marshalled nor copied


    static final byte           DEST_SET         =  1;
    static final byte           SRC_SET          =  1 << 1;


    /**
    * Constructs a message given a destination address
    * @param dest The Address of the receiver. If it is null, then the message is sent to the group. Otherwise, it is
    *             sent to a single member.
    */
    public BaseMessage(Address dest) {
        setDest(dest);
        headers=createHeaders(Util.DEFAULT_HEADERS);
    }

   /**
    * Constructs a message given a destination and source address and the payload byte buffer
    * @param dest The Address of the receiver. If it is null, then the message is sent to the group. Otherwise, it is
    *             sent to a single member.
    * @param buf The payload. Note that this buffer must not be modified (e.g. buf[0]='x' is not
    *           allowed) since we don't copy the contents.
    */
    public BaseMessage(Address dest, byte[] buf) {
        this(dest, buf, 0, buf != null? buf.length : 0);
    }


   /**
    * Constructs a message. The index and length parameters provide a reference to a byte buffer, rather than a copy,
    * and refer to a subset of the buffer. This is important when we want to avoid copying. When the message is
    * serialized, only the subset is serialized.</p>
    * <em>
    * Note that the byte[] buffer passed as argument must not be modified. Reason: if we retransmit the
    * message, it would still have a ref to the original byte[] buffer passed in as argument, and so we would
    * retransmit a changed byte[] buffer !
    * </em>
    *
    * @param dest The Address of the receiver. If it is null, then the message is sent to the group. Otherwise, it is
    *             sent to a single member.
    * @param buf A reference to a byte buffer
    * @param offset The index into the byte buffer
    * @param length The number of bytes to be used from <tt>buf</tt>. Both index and length are checked
    *           for array index violations and an ArrayIndexOutOfBoundsException will be thrown if invalid
    */
    public BaseMessage(Address dest, byte[] buf, int offset, int length) {
        this(dest);
        setBuffer(buf, offset, length);
    }


    public BaseMessage(Address dest, Buffer buf) {
        this(dest);
        setBuffer(buf);
    }


   /**
    * Constructs a message given a destination and source address and the payload object
    * @param dest The Address of the receiver. If it is null, then the message is sent to the group. Otherwise, it is
    *             sent to a single member.
    * @param obj The object that will be marshalled into the byte buffer. Has to be serializable (e.g. implementing
    *            Serializable, Externalizable or Streamable, or be a basic type (e.g. Integer, Short etc)).
    */
    public BaseMessage(Address dest, Object obj) {
        this(dest);
        setObject(obj);
    }


    public BaseMessage() {
        this(true);
    }


    public BaseMessage(boolean create_headers) {
        if(create_headers)
            headers=createHeaders(Util.DEFAULT_HEADERS);
    }


    public Address getDest()                 {return dest_addr;}
    public Address dest()                    {return dest_addr;}

    public <T extends Message> T setDest(Address new_dest) {
        dest_addr=new_dest; return (T)this;
    }

    // public Msg setDest(Address new_dest) {dest_addr=new_dest; return this;}


    public Message dest(Address new_dest)    {dest_addr=new_dest; return this;}
    public Address getSrc()                  {return src_addr;}
    public Address src()                     {return src_addr;}
    public Message setSrc(Address new_src)   {src_addr=new_src; return this;}
    public Message src(Address new_src)      {src_addr=new_src; return this;}
    public abstract int     getOffset();
    public abstract int     offset();
    public abstract int     getLength();
    public abstract int     length();


    /**
     * Returns a <em>reference</em> to the payload (byte buffer). Note that this buffer should not be
     * modified as we do not copy the buffer on copy() or clone(): the buffer of the copied message
     * is simply a reference to the old buffer.<br/>
     * Even if offset and length are used: we return the <em>entire</em> buffer, not a subset.
     */
    public abstract byte[]  getRawBuffer();
    public abstract byte[]  rawBuffer();
    public abstract byte[]  buffer();
    public abstract Buffer  buffer2();
    public abstract Message buffer(byte[] b);
    public abstract Message buffer(Buffer b);
    public int     getNumHeaders()           {return Headers.size(this.headers);}
    public int     numHeaders()              {return Headers.size(this.headers);}




   /**
    * Returns a reference to the headers hashmap, which is <em>immutable</em>. Any attempt to modify
    * the returned map will cause a runtime exception
    */
    public Map<Short,Header> getHeaders() {
        return Headers.getHeaders(this.headers);
    }

    public String printHeaders() {
        return Headers.printHeaders(this.headers);
    }




    public <T extends Object> T getObject() {
        return getObject(null);
    }


    /**
     * Sets a number of flags in a message
     * @param flags The flag or flags
     * @return A reference to the message
     */
    public Message setFlag(Flag... flags) {
        if(flags != null) {
            short tmp=this.flags;
            for(Flag flag : flags) {
                if(flag != null)
                    tmp|=flag.value();
            }
            this.flags=tmp;
        }
        return this;
    }

    /**
     * Same as {@link #setFlag(Flag...)} except that transient flags are not marshalled
     * @param flags The flag
     */
    public Message setTransientFlag(TransientFlag... flags) {
        if(flags != null) {
            short tmp=this.transient_flags;
            for(TransientFlag flag : flags)
                if(flag != null)
                    tmp|=flag.value();
            this.transient_flags=(byte)tmp;
        }
        return this;
    }

    /**
     * Sets the flags from a short. <em>Not recommended</em> (use {@link #setFlag(Message.Flag...)} instead),
     * as the internal representation of flags might change anytime.
     * @param flag
     * @return
     */
    public Message setFlag(short flag) {
        short tmp=this.flags;
        tmp|=flag;
        this.flags=tmp;
        return this;
    }

    public Message setTransientFlag(short flag) {
        short tmp=this.transient_flags;
        tmp|=flag;
        this.transient_flags=(byte)tmp;
        return this;
    }


    /**
     * Returns the internal representation of flags. Don't use this, as the internal format might change at any time !
     * This is only used by unit test code
     * @return
     */
    public short getFlags()          {return flags;}
    public short getTransientFlags() {return transient_flags;}

    /**
     * Clears a number of flags in a message
     * @param flags The flags
     * @return A reference to the message
     */
    public Message clearFlag(Flag... flags) {
        if(flags != null) {
            short tmp=this.flags;
            for(Flag flag : flags)
                if(flag != null)
                    tmp&=~flag.value();
            this.flags=tmp;
        }
        return this;
    }

    public Message clearTransientFlag(TransientFlag... flags) {
        if(flags != null) {
            short tmp=this.transient_flags;
            for(TransientFlag flag : flags)
                if(flag != null)
                    tmp&=~flag.value();
            this.transient_flags=(byte)tmp;
        }
        return this;
    }

    /**
     * Checks if a given flag is set
     * @param flag The flag
     * @return Whether or not the flag is currently set
     */
    public boolean isFlagSet(Flag flag) {
        return Message.isFlagSet(flags, flag);
    }

    public boolean isTransientFlagSet(TransientFlag flag) {
        return Message.isTransientFlagSet(transient_flags, flag);
    }

    /**
    * Atomically checks if a given flag is set and - if not - sets it. When multiple threads
    * concurrently call this method with the same flag, only one of them will be able to set the
    * flag
    *
    * @param flag
    * @return True if the flag could be set, false if not (was already set)
    */
    public synchronized boolean setTransientFlagIfAbsent(TransientFlag flag) {
        if(isTransientFlagSet(flag))
            return false;
        setTransientFlag(flag);
        return true;
    }


    /*---------------------- Used by protocol layers ----------------------*/

    /** Puts a header given an ID into the hashmap. Overwrites potential existing entry. */
    public Message putHeader(short id, Header hdr) {
        if(id < 0)
            throw new IllegalArgumentException("An ID of " + id + " is invalid");
        if(hdr != null)
            hdr.setProtId(id);
        synchronized(this) {
            Header[] resized_array=Headers.putHeader(this.headers, id, hdr, true);
            if(resized_array != null)
                this.headers=resized_array;
        }
        return this;
    }



    public <T extends Header> T getHeader(short id) {
        if(id <= 0)
            throw new IllegalArgumentException("An ID of " + id + " is invalid. Add the protocol which calls " +
                                                 "getHeader() to jg-protocol-ids.xml");
        return Headers.getHeader(this.headers, id);
    }

    /** Returns a header for a range of IDs, or null if not found */
    public <T extends Header> T getHeader(short... ids) {
        if(ids == null || ids.length == 0)
            return null;
        return Headers.getHeader(this.headers, ids);
    }
    /*---------------------------------------------------------------------*/


    public Message copy() {
        return copy(true);
    }

   /**
    * Create a copy of the message. If offset and length are used (to refer to another buffer), the
    * copy will contain only the subset offset and length point to, copying the subset into the new
    * copy.
    *
    * @param copy_buffer
    * @return Message with specified data
    */
    public Message copy(boolean copy_buffer) {
        return copy(copy_buffer, true);
    }



   /**
    * Doesn't copy any headers except for those with ID >= copy_headers_above
    *
    * @param copy_buffer
    * @param starting_id
    * @return A message with headers whose ID are >= starting_id
    */
    public Message copy(boolean copy_buffer, short starting_id) {
        return copy(copy_buffer, starting_id, (short[])null);
    }

    /**
     * Copies a message. Copies only headers with IDs >= starting_id or IDs which are in the copy_only_ids list
     * @param copy_buffer
     * @param starting_id
     * @param copy_only_ids
     * @return
     */
    public Message copy(boolean copy_buffer, short starting_id, short... copy_only_ids) {
        Message retval=copy(copy_buffer, false);
        for(Map.Entry<Short,Header> entry: getHeaders().entrySet()) {
            short id=entry.getKey();
            if(id >= starting_id || Util.containsId(id, copy_only_ids))
                retval.putHeader(id, entry.getValue());
        }
        return retval;
    }



    public String toString() {
        StringBuilder ret=new StringBuilder(64);
        ret.append("[dst: ");
        if(dest_addr == null)
            ret.append("<null>");
        else
            ret.append(dest_addr);
        ret.append(", src: ");
        if(src_addr == null)
            ret.append("<null>");
        else
            ret.append(src_addr);

        int size;
        if((size=getNumHeaders()) > 0)
            ret.append(" (").append(size).append(" headers)");

        ret.append(", size=").append(getLength()).append(" bytes");
        if(flags > 0)
            ret.append(", flags=").append(Message.flagsToString(flags));
        if(transient_flags > 0)
            ret.append(", transient_flags=" + Message.transientFlagsToString(transient_flags));
        ret.append(']');
        return ret.toString();
    }



    public String printObjectHeaders() {
        return Headers.printObjectHeaders(this.headers);
    }





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
    public long size() {
        long retval=(long)Global.BYTE_SIZE   // leading byte
                + Global.SHORT_SIZE;   // flags
        if(dest_addr != null)
            retval+=Util.size(dest_addr);
        if(src_addr != null)
            retval+=Util.size(src_addr);

        retval+=Global.SHORT_SIZE;  // number of headers
        retval+=Headers.marshalledSize(this.headers);
        return retval;
    }


    public void writeTo(DataOutput out) throws Exception {
        byte leading=0;

        if(dest_addr != null)
            leading=Util.setFlag(leading, DEST_SET);

        if(src_addr != null)
            leading=Util.setFlag(leading, SRC_SET);

        // 1. write the leading byte first
        out.write(leading);

        // 2. the flags (e.g. OOB, LOW_PRIO), skip the transient flags
        out.writeShort(flags);

        // 3. dest_addr
        if(dest_addr != null)
            Util.writeAddress(dest_addr, out);

        // 4. src_addr
        if(src_addr != null)
            Util.writeAddress(src_addr, out);

        // 5. headers
        Header[] hdrs=this.headers;
        int size=Headers.size(hdrs);
        out.writeShort(size);
        if(size > 0) {
            for(Header hdr : hdrs) {
                if(hdr == null)
                    break;
                out.writeShort(hdr.getProtId());
                writeHeader(hdr, out);
            }
        }
    }

    public void writeToNoAddrs(Address src, DataOutput out, short... excluded_headers) throws Exception {
        byte leading=0;

        boolean write_src_addr=src == null || src_addr != null && !src_addr.equals(src);

        if(write_src_addr)
            leading=Util.setFlag(leading, SRC_SET);

        // 1. write the leading byte first
        out.write(leading);

        // 2. the flags (e.g. OOB, LOW_PRIO)
        out.writeShort(flags);

        // 4. src_addr
        if(write_src_addr)
            Util.writeAddress(src_addr, out);

        // 5. headers
        Header[] hdrs=this.headers;
        int size=Headers.size(hdrs, excluded_headers);
        out.writeShort(size);
        if(size > 0) {
            for(Header hdr : hdrs) {
                if(hdr == null)
                    break;
                short id=hdr.getProtId();
                if(excluded_headers != null && Util.containsId(id, excluded_headers))
                    continue;
                out.writeShort(id);
                writeHeader(hdr, out);
            }
        }
    }


    public void readFrom(DataInput in) throws Exception {
        // 1. read the leading byte first
        byte leading=in.readByte();

        // 2. the flags
        flags=in.readShort();

        // 3. dest_addr
        if(Util.isFlagSet(leading, DEST_SET))
            dest_addr=Util.readAddress(in);

        // 4. src_addr
        if(Util.isFlagSet(leading, SRC_SET))
            src_addr=Util.readAddress(in);

        // 5. headers
        int len=in.readShort();
        this.headers=createHeaders(len);
        for(int i=0; i < len; i++) {
            short id=in.readShort();
            Header hdr=readHeader(in).setProtId(id);
            this.headers[i]=hdr;
        }
    }



    protected static void writeHeader(Header hdr, DataOutput out) throws Exception {
        short magic_number=hdr.getMagicId();
        out.writeShort(magic_number);
        hdr.writeTo(out);
    }



    protected static Header readHeader(DataInput in) throws Exception {
        short magic_number=in.readShort();
        Header hdr=ClassConfigurator.create(magic_number);
        hdr.readFrom(in);
        return hdr;
    }

    protected static Header[] createHeaders(int size) {
        return size > 0? new Header[size] : new Header[3];
    }


}
