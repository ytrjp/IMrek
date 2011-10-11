// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MqttConnect.java

package com.ibm.mqtt;


// Referenced classes of package com.ibm.mqtt:
//            MqttPacket, MqttUtils, MqttException, MqttProcessor

public class MqttConnect extends MqttPacket
{

    public MqttConnect()
    {
        ProtoName = "MQIsdp";
        ProtoVersion = 3;
        setMsgType((short)1);
    }

    public MqttConnect(byte abyte0[])
    {
        super(abyte0);
        ProtoName = "MQIsdp";
        ProtoVersion = 3;
        setMsgType((short)1);
    }

    public byte[] toBytes()
    {
        int i = 0;
        message = new byte[42];
        message[i++] = super.toBytes()[0];
        byte abyte0[] = MqttUtils.StringToUTF(ProtoName);
        System.arraycopy(abyte0, 0, message, i, abyte0.length);
        i += abyte0.length;
        message[i++] = (byte)ProtoVersion;
        boolean flag = TopicNameCompression;
        byte byte0 = (byte)(CleanStart ? 2 : 0);
        byte byte1 = Will ? (byte)((WillRetain ? 0x20 : 0) | (byte)((WillQoS & 3) << 3) | 4) : 0;
        message[i++] = (byte)(0xc0 | (byte)((((byte)(flag ? 1 : 0))) | byte0 | byte1));
        message[i++] = (byte)(KeepAlive / 256);
        message[i++] = (byte)(KeepAlive % 256);
        abyte0 = MqttUtils.StringToUTF(ClientId);
        System.arraycopy(abyte0, 0, message, i, abyte0.length);
        i += abyte0.length;
        if(Will)
        {
            byte abyte1[] = MqttUtils.StringToUTF(WillTopic);
            byte abyte2[] = MqttUtils.StringToUTF(WillMessage);
            message = MqttUtils.concatArray(MqttUtils.concatArray(message, 0, i, abyte1, 0, abyte1.length), abyte2);
            i += abyte1.length + abyte2.length;
            if(Username.length() > 1 && Password.length() > 1)
            {
                byte abyte3[] = MqttUtils.StringToUTF(Username);
                byte abyte4[] = MqttUtils.StringToUTF(Password);
                message = MqttUtils.concatArray(MqttUtils.concatArray(message, 0, i, abyte3, 0, abyte3.length), abyte4);
                i += abyte3.length + abyte4.length;
            }
        }
        message = MqttUtils.SliceByteArray(message, 0, i);
        createMsgLength();
        return message;
    }

    public void process(MqttProcessor mqttprocessor1)
    {
    }

    public String getClientId()
    {
        return ClientId;
    }

    public void setClientId(String s)
        throws MqttException
    {
        if(s.length() > 23)
        {
            throw new MqttException("MQIsdp ClientId > 23 bytes");
        } else
        {
            ClientId = s;
            return;
        }
    }

    public void setUsername(String user)
        throws MqttException
    {
        if(user.length() > 23)
        {
            throw new MqttException("MQIsdp Username > 12 bytes");
        } else
        {
            Username = user;
            return;
        }
    }

    public void setPassword(String pass)
        throws MqttException
    {
        if(pass.length() > 12)
        {
            throw new MqttException("MQIsdp Password > 12 bytes");
        } else
        {
            Password = pass;
            return;
        }
    }

    public String ProtoName;
    public short ProtoVersion;
    public boolean CleanStart;
    public boolean TopicNameCompression;
    public short KeepAlive;
    public boolean Will;
    public int WillQoS;
    public boolean WillRetain;
    public String WillTopic;
    public String WillMessage;
    public String Username;
    public String Password;
    protected String ClientId;
}
