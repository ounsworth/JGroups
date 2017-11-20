
package org.jgroups;


import org.jgroups.util.*;

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
public class ObjectMessage extends BaseMessage {
    protected Object obj;
    protected byte[] serialized_obj;


    /**
    * Constructs a message given a destination address
    * @param dest The Address of the receiver. If it is null, then the message is sent to the group. Otherwise, it is
    *             sent to a single member.
    */
    public ObjectMessage(Address dest) {
        setDest(dest);
        headers=createHeaders(Util.DEFAULT_HEADERS);
    }



   /**
    * Constructs a message given a destination and source address and the payload object
    * @param dest The Address of the receiver. If it is null, then the message is sent to the group. Otherwise, it is
    *             sent to a single member.
    * @param obj The object that will be marshalled into the byte buffer. Has to be serializable (e.g. implementing
    *            Serializable, Externalizable or Streamable, or be a basic type (e.g. Integer, Short etc)).
    */
    public ObjectMessage(Address dest, Object obj) {
        this(dest);
        setObject(obj);
    }


    public ObjectMessage() {
        this(true);
    }


    public ObjectMessage(boolean create_headers) {
        if(create_headers)
            headers=createHeaders(Util.DEFAULT_HEADERS);
    }

    public Supplier<? extends Message> create() {
        return ObjectMessage::new;
    }

    public byte    getType()       {return Message.OBJ_MSG;}
    public boolean hasArray()      {return false;}
    public int     getOffset()     {return 0;}
    public int     offset()        {return 0;}
    public int     length()        {return getLength();}

    public int     getLength()     {
        if(obj == null)
            return 0;
        if(obj instanceof SizeStreamable)
            return ((SizeStreamable)obj).serializedSize();
        swizzle();
        return serialized_obj.length;
    }


    public byte[]  getRawBuffer()            {swizzle(); return serialized_obj;}
    public byte[]  rawBuffer()               {swizzle(); return serialized_obj;}
    public byte[]  buffer()                  {return getBuffer();}
    public Buffer  buffer2()                 {return getBuffer2();}
    public Message buffer(byte[] b)          {return setBuffer(b);}
    public Message buffer(Buffer b)          {return setBuffer(b);}


    public byte[] getBuffer() {
        swizzle();
        return serialized_obj;
    }

    public Buffer getBuffer2() {
        swizzle();
        if(serialized_obj == null)
            return null;
        return new Buffer(serialized_obj, 0, serialized_obj.length);
    }


    public Message setBuffer(byte[] b) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the buffer<p/>
     * Note that the byte[] buffer passed as argument must not be modified. Reason: if we retransmit the
     * message, it would still have a ref to the original byte[] buffer passed in as argument, and so we would
     * retransmit a changed byte[] buffer !
     */
    public Message setBuffer(Buffer buf) {
        throw new UnsupportedOperationException();
    }



    /**
     * Takes an object and uses Java serialization to generate the byte[] buffer which is set in the
     * message. Parameter 'obj' has to be serializable (e.g. implementing Serializable,
     * Externalizable or Streamable, or be a basic type (e.g. Integer, Short etc)).
     */
    public Message setObject(Object obj) {
        if(obj == null) return this;
        this.obj=obj;
        return this;
    }


    public <T extends Object> T getObject() {
        return (T)obj;
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
        return (T)obj;
    }



   /**
    * Create a copy of the message. If offset and length are used (to refer to another buffer), the
    * copy will contain only the subset offset and length point to, copying the subset into the new
    * copy.<p/>
    * Note that for headers, only the arrays holding references to the headers are copied, not the headers themselves !
    * The consequence is that the headers array of the copy hold the *same* references as the original, so do *not*
    * modify the headers ! If you want to change a header, copy it and call {@link ObjectMessage#putHeader(short,Header)} again.
    *
    * @param copy_buffer
    * @param copy_headers
    *           Copy the headers
    * @return Message with specified data
    */
    public Message copy(boolean copy_buffer, boolean copy_headers) {
        ObjectMessage retval=new ObjectMessage(false);
        retval.dest_addr=dest_addr;
        retval.src_addr=src_addr;
        short tmp_flags=this.flags;
        byte tmp_tflags=this.transient_flags;
        retval.flags=tmp_flags;
        retval.transient_flags=tmp_tflags;

        if(copy_buffer && obj != null)
            retval.setObject(obj);

        //noinspection NonAtomicOperationOnVolatileField
        retval.headers=copy_headers && headers != null? Headers.copy(this.headers) : createHeaders(Util.DEFAULT_HEADERS);
        return retval;
    }



    public Message makeReply() {
        Message retval=new ObjectMessage(src_addr);
        if(dest_addr != null)
            retval.setSrc(dest_addr);
        return retval;
    }


    protected ObjectMessage swizzle() {
        if(serialized_obj != null || obj == null)
            return this;
        try {
            serialized_obj=Util.objectToByteBuffer(obj);
            return this;
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* ----------------------------------- Interface Streamable  ------------------------------- */

    public int size() {
        int retval=super.size() + Global.BYTE_SIZE; // streamable

        if(obj instanceof SizeStreamable)
            return retval + Util.size((SizeStreamable)obj);

        retval+=Global.INT_SIZE; // length (integer)
        if(obj == null)
            return retval;

        if(serialized_obj == null)
            swizzle();
        return retval + serialized_obj.length; // number of bytes in the buffer
    }



    /** Streams all members (dest and src addresses, buffer and headers) to the output stream */
    public void writeTo(DataOutput out) throws Exception {
        super.writeTo(out);
        write(out);
    }

   /**
    * Writes the message to the output stream, but excludes the dest and src addresses unless the
    * src address given as argument is different from the message's src address
    * @param excluded_headers Don't marshal headers that are part of excluded_headers
    */
    public void writeToNoAddrs(Address src, DataOutput out, short... excluded_headers) throws Exception {
        super.writeToNoAddrs(src, out, excluded_headers);
        write(out);
    }


    public void readFrom(DataInput in) throws Exception {
        super.readFrom(in);
        boolean streamable=in.readBoolean();
        if(streamable)
            obj=Util.readGenericStreamable(in);
        else {
            int len=in.readInt();
            if(len == -1)
                return;
            serialized_obj=new byte[len];
            in.readFully(serialized_obj, 0, len);
            obj=Util.objectFromByteBuffer(serialized_obj);
        }
    }


    protected void write(DataOutput out) throws Exception {
        out.writeBoolean(obj instanceof SizeStreamable);
        if(obj instanceof SizeStreamable) {
            Util.writeGenericStreamable((Streamable)obj, out);
            return;
        }
        if(obj != null) {
            if(serialized_obj == null)
                swizzle();
            out.writeInt(serialized_obj.length);
            out.write(serialized_obj, 0, serialized_obj.length);
        }
        else
            out.writeInt(-1);
    }


    /* --------------------------------- End of Interface Streamable ----------------------------- */


    public String toString() {
        return super.toString() + String.format(", serialized size: %d", serialized_obj != null? serialized_obj.length : 0);
    }
}
