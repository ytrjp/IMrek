// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.ibm.mqtt;


// Referenced classes of package com.ibm.mqtt:
//            MqttProcessor

public abstract class MqttPacket
{

    public MqttPacket()
    {
        msgId = 0;
        payload = null;
    }

    public MqttPacket(byte abyte0[])
    {
        msgId = 0;
        payload = null;
        byte byte0 = abyte0[0];
        msgType = getMsgType(byte0);
        retain = (byte0 & 1) != 0;
        dup = (byte0 >>> 3 & 1) != 0;
        qos = byte0 >>> 1 & 3;
    }

    protected static short getMsgType(byte byte0)
    {
        return (short)(byte0 >>> 4 & 0xf);
    }

    protected void createMsgLength()
    {
        int i = message.length + -1;
        if(payload != null)
            i += payload.length;
        msgLength = i;
        int j = 0;
        byte abyte0[] = new byte[4];
        do
        {
            int k = i % 128;
            i /= 128;
            if(i > 0)
                k |= 0x80;
            abyte0[j++] = (byte)k;
        } while(i > 0);
        byte abyte1[] = new byte[message.length + j];
        abyte1[0] = message[0];
        System.arraycopy(abyte0, 0, abyte1, 1, j);
        System.arraycopy(message, 1, abyte1, j + 1, message.length - 1);
        message = abyte1;
    }

    public abstract void process(MqttProcessor mqttprocessor);

    public byte[] toBytes()
    {
        byte abyte0[] = new byte[1];
        abyte0[0] = (byte)(msgType << 4 & 0xf0);
        if((msgType == 8) | (msgType == 9) | (msgType == 10) | (msgType == 11))
            qos = 1;
        byte byte2 = (byte)((qos & 3) << 1);
        byte byte0 = (byte)(dup ? 8 : 0);
        byte byte1 = (byte)(retain ? 1 : 0);
        abyte0[0] = (byte)(abyte0[0] | byte2 | byte1 | byte0);
        return abyte0;
    }

    public byte[] getPayload()
    {
        return payload;
    }

    public void setPayload(byte abyte0[])
    {
        payload = abyte0;
    }

    public boolean isDup()
    {
        return dup;
    }

    public void setDup(boolean flag)
    {
        dup = flag;
    }

    public byte[] getMessage()
    {
        return message;
    }

    public void setMessage(byte abyte0[])
    {
        message = abyte0;
    }

    public int getMsgId()
    {
        return msgId;
    }

    public void setMsgId(int i)
    {
        msgId = i;
    }

    public int getMsgLength()
    {
        return msgLength;
    }

    public void setMsgLength(int i)
    {
        msgLength = i;
    }

    public short getMsgType()
    {
        return msgType;
    }

    public void setMsgType(short word0)
    {
        msgType = word0;
    }

    public int getQos()
    {
        return qos;
    }

    public void setQos(int i)
    {
        qos = i;
    }

    public boolean isRetain()
    {
        return retain;
    }

    public void setRetain(boolean flag)
    {
        retain = flag;
    }

    protected byte message[];
    private short msgType;
    private int msgLength;
    private int msgId;
    private boolean retain;
    private boolean dup;
    private int qos;
    private byte payload[];
    public static final int MAX_CLIENT_ID_LEN = 23;
    public static final int MAX_MSGID = 65535;
}
