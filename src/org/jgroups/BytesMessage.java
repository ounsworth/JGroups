
package org.jgroups;


import org.jgroups.util.Buffer;
import org.jgroups.util.Headers;
import org.jgroups.util.Util;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.function.Supplier;

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
public class BytesMessage extends BaseMessage {

    /** The payload */
    protected byte[]            buf;

    /** The index into the payload (usually 0) */
    protected int               offset;

    /** The number of bytes in the buffer (usually buf.length is buf not equal to null). */
    protected int               length;



    /**
    * Constructs a message given a destination address
    * @param dest The Address of the receiver. If it is null, then the message is sent to the group. Otherwise, it is
    *             sent to a single member.
    */
    public BytesMessage(Address dest) {
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
    public BytesMessage(Address dest, byte[] buf) {
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
    public BytesMessage(Address dest, byte[] buf, int offset, int length) {
        this(dest);
        setBuffer(buf, offset, length);
    }


    public BytesMessage(Address dest, Buffer buf) {
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
    public BytesMessage(Address dest, Object obj) {
        this(dest);
        setObject(obj);
    }


    public BytesMessage() {
        this(true);
    }


    public BytesMessage(boolean create_headers) {
        if(create_headers)
            headers=createHeaders(Util.DEFAULT_HEADERS);
    }

    public Supplier<? extends Message> create() {
        return BytesMessage::new;
    }

    public byte getType() {
        return Message.BYTES_MSG;
    }

    public boolean hasArray()                {return true;}
    public int     getOffset()               {return offset;}
    public int     getLength()               {return length;}


    /**
     * Returns a <em>reference</em> to the payload (byte buffer). Note that this buffer should not be
     * modified as we do not copy the buffer on copy() or clone(): the buffer of the copied message
     * is simply a reference to the old buffer.<br/>
     * Even if offset and length are used: we return the <em>entire</em> buffer, not a subset.
     */
    public byte[]  getRawBuffer()            {return buf;}
    public byte[]  rawBuffer()               {return buf;}
    public byte[]  buffer()                  {return getBuffer();}
    public Buffer  buffer2()                 {return getBuffer2();}
    public Message buffer(byte[] b)          {return setBuffer(b);}
    public Message buffer(Buffer b)          {return setBuffer(b);}


   /**
    * Returns a copy of the buffer if offset and length are used, otherwise a reference.
    * @return byte array with a copy of the buffer.
    */
    public byte[] getBuffer() {
        if(buf == null)
            return null;
        if(offset == 0 && length == buf.length)
            return buf;
        else {
            byte[] retval=new byte[length];
            System.arraycopy(buf, offset, retval, 0, length);
            return retval;
        }
    }

    public Buffer getBuffer2() {
        if(buf == null)
            return null;
        return new Buffer(buf, offset, length);
    }

    /**
     * Sets the buffer.<p/>
     * Note that the byte[] buffer passed as argument must not be modified. Reason: if we retransmit the
     * message, it would still have a ref to the original byte[] buffer passed in as argument, and so we would
     * retransmit a changed byte[] buffer !
     */
    public <T extends Message> T setBuffer(byte[] b) {
        buf=b;
        if(buf != null) {
            offset=0;
            length=buf.length;
        }
        else
            offset=length=0;
        return (T)this;
    }

    /**
     * Sets the internal buffer to point to a subset of a given buffer.<p/>
     * <em>
     * Note that the byte[] buffer passed as argument must not be modified. Reason: if we retransmit the
     * message, it would still have a ref to the original byte[] buffer passed in as argument, and so we would
     * retransmit a changed byte[] buffer !
     * </em>
     *
     * @param b The reference to a given buffer. If null, we'll reset the buffer to null
     * @param offset The initial position
     * @param length The number of bytes
     */
    public Message setBuffer(byte[] b, int offset, int length) {
        buf=b;
        if(buf != null) {
            if(offset < 0 || offset > buf.length)
                throw new ArrayIndexOutOfBoundsException(offset);
            if((offset + length) > buf.length)
                throw new ArrayIndexOutOfBoundsException((offset+length));
            this.offset=offset;
            this.length=length;
        }
        else
            this.offset=this.length=0;
        return this;
    }

    /**
     * Sets the buffer<p/>
     * Note that the byte[] buffer passed as argument must not be modified. Reason: if we retransmit the
     * message, it would still have a ref to the original byte[] buffer passed in as argument, and so we would
     * retransmit a changed byte[] buffer !
     */
    public Message setBuffer(Buffer buf) {
        if(buf != null) {
            this.buf=buf.getBuf();
            this.offset=buf.getOffset();
            this.length=buf.getLength();
        }
        return this;
    }



    /**
     * Takes an object and uses Java serialization to generate the byte[] buffer which is set in the
     * message. Parameter 'obj' has to be serializable (e.g. implementing Serializable,
     * Externalizable or Streamable, or be a basic type (e.g. Integer, Short etc)).
     */
    public Message setObject(Object obj) {
        if(obj == null) return this;
        if(obj instanceof byte[])
            return setBuffer((byte[])obj);
        if(obj instanceof Buffer)
            return setBuffer((Buffer)obj);
        try {
            return setBuffer(Util.objectToByteBuffer(obj));
        }
        catch(Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }


    public <T extends Object> T getObject() {
        return getObject(null);
    }

    /**
     * Uses custom serialization to create an object from the buffer of the message. Note that this is dangerous when
     * using your own classloader, e.g. inside of an application server ! Most likely, JGroups will use the system
     * classloader to deserialize the buffer into an object, whereas (for example) a web application will want to use
     * the webapp's classloader, resulting in a ClassCastException. The recommended way is for the application to use
     * their own serialization and only pass byte[] buffer to JGroups.<p/>
     * As of 3.5, a classloader can be passed in. It will be used first to find a class, before contacting
     * the other classloaders in the list. If null, the default list of classloaders will be used.
     * @return the object
     */
    public <T extends Object> T getObject(ClassLoader loader) {
        try {
            return Util.objectFromByteBuffer(buf, offset, length, loader);
        }
        catch(Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }



   /**
    * Create a copy of the message. If offset and length are used (to refer to another buffer), the
    * copy will contain only the subset offset and length point to, copying the subset into the new
    * copy.<p/>
    * Note that for headers, only the arrays holding references to the headers are copied, not the headers themselves !
    * The consequence is that the headers array of the copy hold the *same* references as the original, so do *not*
    * modify the headers ! If you want to change a header, copy it and call {@link BytesMessage#putHeader(short,Header)} again.
    *
    * @param copy_buffer
    * @param copy_headers
    *           Copy the headers
    * @return Message with specified data
    */
    public Message copy(boolean copy_buffer, boolean copy_headers) {
        BytesMessage retval=new BytesMessage(false);
        retval.dest_addr=dest_addr;
        retval.src_addr=src_addr;
        short tmp_flags=this.flags;
        byte tmp_tflags=this.transient_flags;
        retval.flags=tmp_flags;
        retval.transient_flags=tmp_tflags;

        if(copy_buffer && buf != null)
            retval.setBuffer(buf, offset, length);

        //noinspection NonAtomicOperationOnVolatileField
        retval.headers=copy_headers && headers != null? Headers.copy(this.headers) : createHeaders(Util.DEFAULT_HEADERS);
        return retval;
    }



    public Message makeReply() {
        Message retval=new BytesMessage(src_addr);
        if(dest_addr != null)
            retval.setSrc(dest_addr);
        return retval;
    }



    /* ----------------------------------- Interface Streamable  ------------------------------- */

    public int size() {
        int retval=super.size() + Global.INT_SIZE; // length
        if(buf != null)
            retval+=length;       // number of bytes in the buffer
        return retval;
    }


    @Override public void writeTo(DataOutput out) throws Exception {
        super.writeTo(out);
        out.writeInt(buf != null? length : -1);
        if(buf != null)
            out.write(buf, offset, length);
    }

   /**
    * Writes the message to the output stream, but excludes the dest and src addresses unless the
    * src address given as argument is different from the message's src address
    * @param excluded_headers Don't marshal headers that are part of excluded_headers
    */
    @Override public void writeToNoAddrs(Address src, DataOutput out, short... excluded_headers) throws Exception {
        super.writeToNoAddrs(src, out, excluded_headers);
        out.writeInt(buf != null? length : -1);
        if(buf != null)
            out.write(buf, offset, length);
    }


    @Override public void readFrom(DataInput in) throws Exception {
        super.readFrom(in);
        int len=in.readInt();
        if(len >= 0) {
            buf=new byte[len];
            in.readFully(buf, 0, len);
            length=len;
        }
    }

    /* --------------------------------- End of Interface Streamable ----------------------------- */



}
