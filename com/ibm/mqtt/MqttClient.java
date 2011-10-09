// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package com.ibm.mqtt;

public class MqttClient extends MqttBaseClient
    implements IMqttClient
{

    public static final IMqttClient createMqttClient(String s, MqttPersistence mqttpersistence)
        throws MqttException
    {
        return new MqttClient(s, mqttpersistence);
    }

    protected MqttClient()
    {
        traceClass = null;
        persistenceLayer = null;
        reader = null;
        connAckLock = new Object();
        isAppConnected = false;
        advCallbackHandler = null;
        simpleCallbackHandler = null;
    }

    public MqttClient(String s)
        throws MqttException
    {
        this(s, null);
    }

    public MqttClient(String s, MqttPersistence mqttpersistence)
        throws MqttException
    {
        traceClass = null;
        persistenceLayer = null;
        reader = null;
        connAckLock = new Object();
        isAppConnected = false;
        advCallbackHandler = null;
        simpleCallbackHandler = null;
        initialise(s, mqttpersistence);
    }

    protected void initialise(String s, MqttPersistence mqttpersistence)
        throws MqttException
    {
        Class class1 = null;
        connection = s;
        persistenceLayer = mqttpersistence;
        if(s.startsWith("local://"))
            class1 = loadLocalBindings();
        else
        if(s.startsWith("tcp://"))
        {
            class1 = loadTcpBindings();
            connection = s.replace('@', ':');
        } else
        {
            throw new MqttException("Unrecognised connection method:" + s);
        }
        super.initialise(connection, persistenceLayer, class1);
        try
        {
            traceClass = Class.forName("com.ibm.mqtt.trace.MQeTraceToBinaryFile");
        }
        catch(ClassNotFoundException classnotfoundexception)
        {
            traceClass = null;
        }
        start(class1);
    }

    private Class loadTcpBindings()
        throws MqttException
    {
        boolean flag = false;
        Class class1 = null;
        try
        {
            class1 = Class.forName("com.ibm.mqtt.j2se.MqttJavaNetSocket");
            flag = true;
        }
        catch(ClassNotFoundException classnotfoundexception) { }
        if(!flag)
            try
            {
                class1 = Class.forName("com.ibm.mqtt.midp.MqttMidpSocket");
            }
            catch(ClassNotFoundException classnotfoundexception1)
            {
                MqttException mqttexception = new MqttException("Cannot locate a J2SE Socket or J2ME StreamConnection class");
                mqttexception.initCause(classnotfoundexception1);
                throw mqttexception;
            }
        return class1;
    }

    private Class loadLocalBindings()
        throws MqttException
    {
        boolean flag = false;
        Class class1 = null;
        try
        {
            class1 = Class.forName("com.ibm.mqtt.local.MqttLocalBindingV2");
            flag = true;
        }
        catch(ClassNotFoundException classnotfoundexception) { }
        if(!flag)
            try
            {
                class1 = Class.forName("com.ibm.mqtt.local.MqttLocalBindingV1");
            }
            catch(ClassNotFoundException classnotfoundexception1)
            {
                MqttException mqttexception = new MqttException("LocalBinding unavailable: Microbroker classes not found");
                mqttexception.initCause(classnotfoundexception1);
                throw mqttexception;
            }
        return class1;
    }

    private void start(Class class1)
        throws MqttException
    {
        try
        {
            reader = new Thread(this);
            reader.start();
            super.setRetry(120);
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
            throw new MqttException(exception);
        }
    }

    public void startTrace()
        throws MqttException
    {
        if(traceClass != null)
        {
            MQeTrace.setFilter(-1L);
            try
            {
                MQeTrace.setHandler((MQeTraceHandler)traceClass.newInstance());
            }
            catch(Exception exception)
            {
                throw new MqttException(exception);
            }
        } else
        {
            throw new MqttException("Trace classes (com.ibm.mqtt.trace.*) not found.\nCheck they are in wmqtt.jar.");
        }
    }

    public void stopTrace()
    {
        MQeTrace.setFilter(0L);
        MQeTrace.setHandler(null);
    }

    public MqttPersistence getPersistence()
    {
        return persistenceLayer;
    }

    public void registerAdvancedHandler(MqttAdvancedCallback mqttadvancedcallback)
    {
        advCallbackHandler = mqttadvancedcallback;
        simpleCallbackHandler = mqttadvancedcallback;
    }

    public void registerSimpleHandler(MqttSimpleCallback mqttsimplecallback)
    {
        simpleCallbackHandler = mqttsimplecallback;
    }
    
    public void connect(String s, boolean flag, short word0)
        throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException
    {
        connect(s, flag, word0, null, 0, null, false);
    }
    
    public void connect(String s, boolean flag, short word0, String user, String pass)
        throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException
    {
        connect(s, flag, word0, null, 0, null, false, user, pass);
    }

    public void connect(String s, boolean flag, short word0, String s1, int i, String s2, boolean flag1)
        throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException
    {
        MQeTrace.trace(this, (short)-30002, 0x100004L);
        if(!isAppConnected || !isSocketConnected())
        {
            int j = 0;
            synchronized(connAckLock)
            {
                conRetCode = -1;
                super.connect(s, flag, false, word0, s1, i, s2, flag1);
                try
                {
                    connAckLock.wait(getRetry() * 1000);
                }
                catch(InterruptedException interruptedexception) { }
                j = conRetCode;
            }
            MQeTrace.trace(this, (short)-30003, 0x100008L);
            switch(j)
            {
            case 0: // '\0'
                isAppConnected = true;
                break;

            case 1: // '\001'
                MqttConnect mqttconnect = new MqttConnect();
                throw new MqttException("WMQTT protocol name or version not supported:" + mqttconnect.ProtoName + " Version:" + mqttconnect.ProtoVersion);

            case 2: // '\002'
                throw new MqttException("WMQTT ClientId is invalid");

            case 3: // '\003'
                throw new MqttBrokerUnavailableException("WMQTT Broker is unavailable");

            default:
                tcpipDisconnect(true);
                throw new MqttNotConnectedException("WMQTT " + msgTypes[2] + " not received");
            }
        }
    }
    
    public void connect(String s, boolean flag, short word0, String s1, int i, String s2, boolean flag1, String user, String pass)
            throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException
        {
            MQeTrace.trace(this, (short)-30002, 0x100004L);
            if(!isAppConnected || !isSocketConnected())
            {
                int j = 0;
                synchronized(connAckLock)
                {
                    conRetCode = -1;
                    super.connect(s, flag, false, word0, s1, i, s2, flag1, user, pass);
                    try
                    {
                        connAckLock.wait(getRetry() * 1000);
                    }
                    catch(InterruptedException interruptedexception) { }
                    j = conRetCode;
                }
                MQeTrace.trace(this, (short)-30003, 0x100008L);
                switch(j)
                {
                case 0: // '\0'
                    isAppConnected = true;
                    break;

                case 1: // '\001'
                    MqttConnect mqttconnect = new MqttConnect();
                    throw new MqttException("WMQTT protocol name or version not supported:" + mqttconnect.ProtoName + " Version:" + mqttconnect.ProtoVersion);

                case 2: // '\002'
                    throw new MqttException("WMQTT ClientId is invalid");

                case 3: // '\003'
                    throw new MqttBrokerUnavailableException("WMQTT Broker is unavailable");

                default:
                    tcpipDisconnect(true);
                    throw new MqttNotConnectedException("WMQTT " + msgTypes[2] + " not received");
                }
            }
        }

    protected void connectionLost()
        throws Exception
    {
        MQeTrace.trace(this, (short)-30004, 0x200000L);
        super.connectionLost();
        if(simpleCallbackHandler != null)
            simpleCallbackHandler.connectionLost();
        else
            throw new MqttNotConnectedException("WMQtt Connection Lost");
    }

    public void disconnect()
        throws MqttPersistenceException
    {
        MQeTrace.trace(this, (short)-30005, 0x100004L);
        if(isAppConnected)
        {
            super.disconnect();
            isAppConnected = false;
        }
        MQeTrace.trace(this, (short)-30006, 0x100008L);
    }

    public String getConnection()
    {
        return connection;
    }

    protected void notifyAck(int i, int j)
    {
        switch(i)
        {
        case 2: // '\002'
        case 4: // '\004'
        case 5: // '\005'
        case 7: // '\007'
        case 9: // '\t'
        default:
            break;

        case 1: // '\001'
            synchronized(connAckLock)
            {
                conRetCode = j;
                connAckLock.notifyAll();
            }
            break;

        case 8: // '\b'
            if(advCallbackHandler != null)
                advCallbackHandler.subscribed(j, getReturnedQoS(j));
            break;

        case 10: // '\n'
            if(advCallbackHandler != null)
                advCallbackHandler.unsubscribed(j);
            break;

        case 3: // '\003'
        case 6: // '\006'
            if(advCallbackHandler != null)
                advCallbackHandler.published(j);
            break;
        }
    }

    public void ping()
        throws MqttException
    {
        if(Thread.currentThread().equals(reader))
            invalidApiInvocation();
        pingOut();
    }

    public int publish(String s, byte abyte0[], int i, boolean flag)
        throws MqttNotConnectedException, MqttPersistenceException, MqttException, IllegalArgumentException
    {
        MQeTrace.trace(this, (short)-30007, 0x100004L);
        if(s == null)
            throw new IllegalArgumentException("NULL topic");
        if(abyte0 == null)
            throw new IllegalArgumentException("NULL message");
        if(s.indexOf('#') > -1 || s.indexOf('+') > -1)
            throw new IllegalArgumentException("Topic contains '#' or '+'");
        if(Thread.currentThread().equals(reader))
            invalidApiInvocation();
        anyErrors();
        int j = super.publish(s, abyte0, i, flag);
        MQeTrace.trace(this, (short)-30008, 0x100008L);
        return j;
    }

    protected void publishArrived(String s, byte abyte0[], int i, boolean flag)
        throws Exception
    {
        String s1 = MqttUtils.toHexString(abyte0, 0, 30);
        String s2 = s;
        if(s.length() > 30)
            s2 = s.substring(0, 31);
        MQeTrace.trace(this, (short)-30009, 0x100004L, Integer.toString(s.length()), s2, Integer.toString(abyte0.length), s1);
        if(simpleCallbackHandler != null)
            simpleCallbackHandler.publishArrived(s, abyte0, i, flag);
        MQeTrace.trace(this, (short)-30010, 0x100008L);
    }

    public void terminate()
    {
        terminate(true);
    }

    public void terminate(boolean flag)
    {
        if(isAppConnected && flag)
            try
            {
                disconnect();
            }
            catch(Exception exception) { }
        super.terminate();
        try
        {
            reader.join();
        }
        catch(InterruptedException interruptedexception) { }
    }

    public int subscribe(String as[], int ai[])
        throws MqttNotConnectedException, MqttException, IllegalArgumentException
    {
        MQeTrace.trace(this, (short)-30011, 0x100004L);
        if(as == null)
            throw new IllegalArgumentException("NULL topic array");
        if(ai == null)
            throw new IllegalArgumentException("NULL requested QoS array");
        if(as.length != ai.length)
            throw new IllegalArgumentException("Array lengths unequal. Topics:" + as.length + ", QoS:" + ai.length);
        for(int i = 0; i < as.length; i++)
            if(as[i] == null)
                throw new IllegalArgumentException("NULL topic in topic array at index " + i);

        if(Thread.currentThread().equals(reader))
            invalidApiInvocation();
        for(int j = 0; j < ai.length; j++)
        {
            if(ai[j] > 2)
            {
                ai[j] = 2;
                continue;
            }
            if(ai[j] < 0)
                ai[j] = 0;
        }

        anyErrors();
        int k = super.subscribe(as, ai);
        MQeTrace.trace(this, (short)-30012, 0x100008L);
        return k;
    }

    public int unsubscribe(String as[])
        throws MqttNotConnectedException, MqttException, IllegalArgumentException
    {
        MQeTrace.trace(this, (short)-30013, 0x100004L);
        if(as == null)
            throw new IllegalArgumentException("NULL topic array");
        for(int i = 0; i < as.length; i++)
            if(as[i] == null)
                throw new IllegalArgumentException("NULL topic in topic array at index " + i);

        if(Thread.currentThread().equals(reader))
            invalidApiInvocation();
        anyErrors();
        int j = super.unsubscribe(as);
        MQeTrace.trace(this, (short)-30014, 0x100008L);
        return j;
    }

    private void invalidApiInvocation()
        throws MqttException
    {
        throw new MqttException("MqttClient API called in a callback method! Use a different thread.");
    }

    private Class traceClass;
    private String connection;
    private MqttPersistence persistenceLayer;
    private Thread reader;
    private Object connAckLock;
    private int conRetCode;
    private boolean isAppConnected;
    private MqttAdvancedCallback advCallbackHandler;
    private MqttSimpleCallback simpleCallbackHandler;
}
