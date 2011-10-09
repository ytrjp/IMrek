package com.ibm.mqtt;

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
    	//This essentially assembles the fixed header, variable header, and payload
    	
        int i = 0; //Define an index (current byte #)
        message = new byte[42]; //Create a new array of 42 bytes
        //toBytes()[0] is: (msgType << 4 & 0xf0) | ((qos & 3) << 1) | (retain ? 1 : 0) | (dup ? 8 : 0)
        message[i++] = super.toBytes()[0];
        //Create UTF-8 string of protocol name
        byte abyte0[] = MqttUtils.StringToUTF(ProtoName);
        System.arraycopy(abyte0, 0, message, i, abyte0.length); //?? I think this copies stuff into abyte0
        i += abyte0.length; //Set i to the new byte index by adding the length of abyte0
        message[i++] = (byte)ProtoVersion; //Add protocol version to message
        //TopicNameCompression doesn't look like it's ever set anywhere,
        //And the 3.1 spec claims it's part of CONNACK messages, not CONNECT
        boolean flag = TopicNameCompression;
        
        //**********//
        // Assemble Variable Header flags
        /* The v3.1 spec gives the following example for byte10 of the variable header (flags):
         * 
         * Username flag (1)
         * Password flag (1)
         * Will RETAIN (0)
         * Will QoS (01)
         * Will flag (1)
         * Clean Session (1)
         * 
         * ...For the byte:
         * user | pass | wRetain | wQoS | wFlag | Clean | Unused
         *   1  |   1  |    0    | 0  1 |   1   |   1   |   *
         *   
         *   So we need to set the username and password flags, and then properly add them to the payload below
         */
        byte byte0 = ((byte)(CleanStart ? 2 : 0)); //Cleanstart
        //byte0 = 00000010
        //Will RETAIN, Will QoS, Will flag
        byte byte1 = Will ? (byte)((WillRetain ? 0x20 : 0) | (byte)((WillQoS & 3) << 3) | 4) : 0;
        //WillRetain = 00100000
        //WillQoS = 00000000
        //byte1 = 00100000 | 00000000 = 00100000
        //The following line was decompiled as such: flag | byte0 | byte1
        //But there was a type error, because you can't use bitwise operators on a boolean(flag)
        //So I rewrote it as it is now, since I figured this was what the logic was:
        message[i++] = (byte) (0xC0 | ((byte)((flag ? (byte)1 : (byte)0) | byte0 | byte1)));
        //...Though wouldn't 0 or 1 (whatever flag is) as a byte set/unset the last bit?: 00000001 or 00000000
        //And the 3/1 spec claims that bit is unused..
        //
        //**********//
        
        message[i++] = (byte)(KeepAlive / 256); //This is Keepalive MSB
        message[i++] = (byte)(KeepAlive % 256); //Keepalive LSB
        
        //**********//
        // Assemble the payload
        /*The v3.1 calls for UTF8 encoded strings in the following order:
         * 
         * Client Identifier
         * Will Topic
         * Will Message
         * User Name
         * Password
         * 
         * It recommends that usernames and passwords are limited to 12 characters, but does not require it.
         * 
         */
        abyte0 = MqttUtils.StringToUTF(ClientId); //ClientID to UTF-8 string (max 23 chars)
        System.arraycopy(abyte0, 0, message, i, abyte0.length); //?? I think this copies stuff into abyte0
        i += abyte0.length;
        if(Will)
        {
            byte abyte1[] = MqttUtils.StringToUTF(WillTopic);
            byte abyte2[] = MqttUtils.StringToUTF(WillMessage);
            // I'm *pretty sure* this takes message, concats it with abyte1,
            // then takes the result (message + abyte1) and contacts it with abyte2
            message = MqttUtils.concatArray(MqttUtils.concatArray(message, 0, i, abyte1, 0, abyte1.length), abyte2);
            i += abyte1.length + abyte2.length; //Then we update the index
            if(Username.length() > 1 && Password.length() > 1) {
            	// If above is the case, then we can do this to add the username and password:
                byte abyte3[] = MqttUtils.StringToUTF(Username);
                byte abyte4[] = MqttUtils.StringToUTF(Password);
                message = MqttUtils.concatArray(MqttUtils.concatArray(message, 0, i, abyte3, 0, abyte3.length), abyte4);
                //And then we just add the user and pass length (abyte3 and abyte4) to the total length:
                i+= abyte3.length + abyte4.length;
            }
        }
        //**********//
        
        message = MqttUtils.SliceByteArray(message, 0, i);
        createMsgLength();
        return message;
    }

    public void process(MqttProcessor mqttprocessor)
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

    public void setUsername(String user) throws MqttException {
        if(user.length() > 23)
        {
            throw new MqttException("MQIsdp Username > 12 bytes");
        } else
        {
            Username = user;
            return;
        }
    }

    public void setPassword(String pass) throws MqttException {
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
