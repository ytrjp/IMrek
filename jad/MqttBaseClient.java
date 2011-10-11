// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MqttBaseClient.java

package com.ibm.mqtt;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;

// Referenced classes of package com.ibm.mqtt:
//            Mqtt, MqttHashTable, MqttTimedEventQueue, MqttException, 
//            MqttPersistenceException, MqttConnect, MqttPersistence, MqttRetry, 
//            MqttPingreq, MqttPacket, MqttPublish, MqttPubrel, 
//            MQeTrace, MqttDisconnect, MqttByteArray, MqttConnack, 
//            MqttPuback, MqttPubcomp, MqttUtils, MqttPubrec, 
//            MqttSuback, MqttUnsuback, MqttReconn, MqttSubscribe, 
//            MqttUnsubscribe, MqttNotConnectedException

public abstract class MqttBaseClient extends Mqtt
    implements Runnable
{

    protected MqttBaseClient()
    {
        outstandingQueue = null;
        cleanSession = false;
        qos2PubsArrived = new Hashtable();
        persistenceLayer = null;
        readerControl = new Object();
        retryQueue = null;
        grantedQoS = new Hashtable();
        terminated = false;
        outLockNotified = false;
        outLock = new Object();
    }

    protected void initialise(String s, MqttPersistence mqttpersistence, Class class1)
    {
        super.initialise(s, class1);
        retryPeriod = 10000;
        outstandingQueue = new MqttHashTable();
        retryQueue = new MqttTimedEventQueue(10, this);
        retryQueue.start();
        persistenceLayer = mqttpersistence;
    }

    protected abstract void notifyAck(int i, int j);

    protected abstract void publishArrived(String s, byte abyte0[], int i, boolean flag)
        throws Exception;

    public void anyErrors()
        throws MqttException
    {
        if(registeredException != null)
            throw registeredException;
        else
            return;
    }

    protected void connect(String s, boolean flag, boolean flag1, short word0, String s1, int i, String s2, 
            boolean flag2, String user, String pass)
        throws MqttException, MqttPersistenceException
    {
        synchronized(outLock)
        {
            outLockNotified = false;
        }
        MqttConnect mqttconnect = new MqttConnect();
        mqttconnect.setClientId(s);
        mqttconnect.setUsername(user);
        mqttconnect.setPassword(pass);
        mqttconnect.CleanStart = flag;
        mqttconnect.TopicNameCompression = flag1;
        mqttconnect.KeepAlive = word0;
        if(s1 != null)
        {
            mqttconnect.Will = true;
            mqttconnect.WillTopic = s1;
            mqttconnect.WillQoS = i;
            mqttconnect.WillRetain = flag2;
            mqttconnect.WillMessage = s2;
        } else
        {
            mqttconnect.Will = false;
        }
        setKeepAlive(word0);
        doConnect(mqttconnect, flag, word0);
    }

    protected void connect(String s, boolean flag, boolean flag1, short word0, String s1, int i, String s2, 
            boolean flag2)
        throws MqttException, MqttPersistenceException
    {
        synchronized(outLock)
        {
            outLockNotified = false;
        }
        MqttConnect mqttconnect = new MqttConnect();
        mqttconnect.setClientId(s);
        mqttconnect.CleanStart = flag;
        mqttconnect.TopicNameCompression = flag1;
        mqttconnect.KeepAlive = word0;
        if(s1 != null)
        {
            mqttconnect.Will = true;
            mqttconnect.WillTopic = s1;
            mqttconnect.WillQoS = i;
            mqttconnect.WillRetain = flag2;
            mqttconnect.WillMessage = s2;
        } else
        {
            mqttconnect.Will = false;
        }
        setKeepAlive(word0);
        doConnect(mqttconnect, flag, word0);
    }

    private void doConnect(MqttConnect mqttconnect, boolean flag, short word0)
        throws MqttException, MqttPersistenceException
    {
        retryQueue.resetTimedEventQueue();
        outstandingQueue.clear();
        qos2PubsArrived.clear();
        initialiseOutMsgIds(null);
        if(persistenceLayer != null && !isConnectionLost())
            persistenceLayer.open(mqttconnect.getClientId(), connection);
        if(flag)
        {
            cleanSession = true;
            if(persistenceLayer != null)
                persistenceLayer.reset();
        }
        if(word0 > 0)
        {
            MqttRetry mqttretry = new MqttRetry(this, new MqttPingreq(), word0 * 1000);
            synchronized(outstandingQueue)
            {
                outstandingQueue.put(0L, mqttretry);
            }
            retryQueue.enqueue(mqttretry);
        }
        if(flag || persistenceLayer == null || isConnectionLost()) goto _L2; else goto _L1
_L1:
        byte abyte0[][];
        Vector vector;
        int i;
        abyte0 = persistenceLayer.getAllSentMessages();
        if(abyte0 == null)
            break MISSING_BLOCK_LABEL_497;
        vector = new Vector();
        i = 0;
          goto _L3
_L5:
        byte abyte2[];
        int i1;
        abyte2 = abyte0[i];
        int k = 0;
        i1 = 1;
        int k1 = 1;
        byte byte0;
        do
        {
            byte0 = abyte2[i1];
            k += (byte0 & 0x7f) * k1;
            k1 *= 128;
            i1++;
        } while((byte0 & 0x80) != 0);
        if(k + i1 != abyte2.length)
        {
            invalidSentMessageRestored(i);
            continue; /* Loop/switch isn't completed */
        }
        MqttRetry mqttretry1;
        mqttretry1 = null;
        switch(MqttPacket.getMsgType(abyte2[0]))
        {
        case 3: // '\003'
            MqttPublish mqttpublish1 = new MqttPublish(abyte2, i1);
            vector.addElement(new Integer(mqttpublish1.getMsgId()));
            mqttretry1 = new MqttRetry(this, mqttpublish1, retryPeriod);
            break;

        case 6: // '\006'
            MqttPubrel mqttpubrel = new MqttPubrel(abyte2, i1);
            vector.addElement(new Integer(mqttpubrel.getMsgId()));
            mqttretry1 = new MqttRetry(this, mqttpubrel, retryPeriod);
            break;

        case 4: // '\004'
        case 5: // '\005'
        default:
            invalidSentMessageRestored(i);
            break;
        }
        if(mqttretry1 != null)
            try
            {
                synchronized(outstandingQueue)
                {
                    outstandingQueue.put(mqttretry1.getMsgId(), mqttretry1);
                }
                retryQueue.enqueue(mqttretry1);
            }
            catch(IndexOutOfBoundsException indexoutofboundsexception)
            {
                invalidSentMessageRestored(i);
            }
        i++;
_L3:
        if(i < abyte0.length) goto _L5; else goto _L4
_L4:
        initialiseOutMsgIds(vector);
        byte abyte1[][] = persistenceLayer.getAllReceivedMessages();
        if(abyte1 == null) goto _L2; else goto _L6
_L6:
        int j = 0;
          goto _L7
_L8:
        byte abyte3[] = abyte1[j];
        int l = 0;
        int j1 = 1;
        int l1 = 1;
        byte byte1;
        do
        {
            byte1 = abyte3[j1];
            l += (byte1 & 0x7f) * l1;
            l1 *= 128;
            j1++;
        } while((byte1 & 0x80) != 0);
        if(l + j1 != abyte3.length)
        {
            invalidReceivedMessageRestored(j);
            continue; /* Loop/switch isn't completed */
        }
        try
        {
            if(MqttPacket.getMsgType(abyte3[0]) == 3)
            {
                MqttPublish mqttpublish = new MqttPublish(abyte3, j1);
                qos2PubsArrived.put(Integer.toString(mqttpublish.getMsgId()), mqttpublish);
            } else
            {
                invalidReceivedMessageRestored(j);
            }
        }
        catch(IndexOutOfBoundsException indexoutofboundsexception1)
        {
            invalidReceivedMessageRestored(j);
        }
        j++;
_L7:
        if(j < abyte1.length) goto _L8; else goto _L2
_L2:
        registeredException = null;
        setConnectionLost(false);
        try
        {
            synchronized(readerControl)
            {
                tcpipConnect(mqttconnect);
                readerControl.notify();
            }
        }
        catch(MqttException mqttexception)
        {
            throw mqttexception;
        }
        catch(Exception exception)
        {
            MqttException mqttexception1 = new MqttException();
            mqttexception1.initCause(exception);
            throw mqttexception1;
        }
        return;
    }

    private void invalidSentMessageRestored(int i)
    {
        MQeTrace.trace(this, (short)-30036, 1L, new Integer(i));
    }

    private void invalidReceivedMessageRestored(int i)
    {
        MQeTrace.trace(this, (short)-30037, 1L, new Integer(i));
    }

    protected void connectionLost()
        throws Exception
    {
        synchronized(outLock)
        {
            outLockNotified = true;
            outLock.notifyAll();
        }
    }

    protected synchronized void setConnectionState(boolean flag)
    {
        super.setConnectionState(flag);
        retryQueue.canDeliverEvents(flag);
    }

    protected void terminate()
    {
        synchronized(readerControl)
        {
            terminated = true;
            readerControl.notify();
        }
        if(retryQueue != null)
            retryQueue.close();
    }

    protected void disconnect()
        throws MqttPersistenceException
    {
        try
        {
            setConnectionState(false);
            MqttDisconnect mqttdisconnect = new MqttDisconnect();
            writePacket(mqttdisconnect);
        }
        catch(MqttException mqttexception)
        {
            tcpipDisconnect(false);
            synchronized(readerControl)
            {
                if(isSocketConnected())
                    try
                    {
                        readerControl.wait(30000L);
                    }
                    catch(InterruptedException interruptedexception) { }
            }
            synchronized(outLock)
            {
                outLockNotified = true;
                outLock.notifyAll();
            }
            qos2PubsArrived.clear();
            retryQueue.resetTimedEventQueue();
            outstandingQueue.clear();
            if(cleanSession)
            {
                cleanSession = false;
                if(persistenceLayer != null)
                    persistenceLayer.reset();
            }
            if(persistenceLayer != null)
                persistenceLayer.close();
            break MISSING_BLOCK_LABEL_430;
        }
        break MISSING_BLOCK_LABEL_296;
        Exception exception;
        exception;
        tcpipDisconnect(false);
        synchronized(readerControl)
        {
            if(isSocketConnected())
                try
                {
                    readerControl.wait(30000L);
                }
                catch(InterruptedException interruptedexception1) { }
        }
        synchronized(outLock)
        {
            outLockNotified = true;
            outLock.notifyAll();
        }
        qos2PubsArrived.clear();
        retryQueue.resetTimedEventQueue();
        outstandingQueue.clear();
        if(cleanSession)
        {
            cleanSession = false;
            if(persistenceLayer != null)
                persistenceLayer.reset();
        }
        if(persistenceLayer != null)
            persistenceLayer.close();
        throw exception;
        tcpipDisconnect(false);
        synchronized(readerControl)
        {
            if(isSocketConnected())
                try
                {
                    readerControl.wait(30000L);
                }
                catch(InterruptedException interruptedexception2) { }
        }
        synchronized(outLock)
        {
            outLockNotified = true;
            outLock.notifyAll();
        }
        qos2PubsArrived.clear();
        retryQueue.resetTimedEventQueue();
        outstandingQueue.clear();
        if(cleanSession)
        {
            cleanSession = false;
            if(persistenceLayer != null)
                persistenceLayer.reset();
        }
        if(persistenceLayer != null)
            persistenceLayer.close();
    }

    public int getRetry()
    {
        return retryPeriod / 1000;
    }

    protected byte[] getReturnedQoS(int i)
    {
        MqttByteArray mqttbytearray = (MqttByteArray)grantedQoS.remove(new Integer(i));
        return mqttbytearray == null ? null : mqttbytearray.getByteArray();
    }

    private MqttPacket messageAck(int i)
    {
        MqttRetry mqttretry = null;
        mqttretry = (MqttRetry)outstandingQueue.get(i);
        if(mqttretry == null)
            return null;
        if(mqttretry.getQoS() == 2 && mqttretry.getMsgType() == 3)
            return messageAckQoS2(i);
        try
        {
            if(persistenceLayer != null)
                synchronized(persistenceLayer)
                {
                    persistenceLayer.delSentMessage(i);
                }
            MqttRetry mqttretry1;
            synchronized(outstandingQueue)
            {
                mqttretry1 = (MqttRetry)outstandingQueue.remove(i);
            }
            releaseMsgId(i);
            if(mqttretry1 != null)
            {
                int j = mqttretry1.getMsgType();
                switch(j)
                {
                case 3: // '\003'
                case 6: // '\006'
                case 8: // '\b'
                case 10: // '\n'
                    if(outstandingQueue.size() == maxOutstanding - 1)
                        synchronized(outLock)
                        {
                            outLockNotified = true;
                            outLock.notifyAll();
                        }
                    notifyAck(j, i);
                    break;
                }
            }
        }
        catch(MqttPersistenceException mqttpersistenceexception) { }
        return null;
    }

    private MqttPacket messageAckQoS2(int i)
    {
        Object obj = null;
        MqttPubrel mqttpubrel = null;
        mqttpubrel = genPubRelPacket(i, false);
        try
        {
            if(persistenceLayer != null)
                synchronized(persistenceLayer)
                {
                    persistenceLayer.updSentMessage(i, mqttpubrel.toBytes());
                }
            synchronized(outstandingQueue)
            {
                MqttRetry mqttretry = (MqttRetry)outstandingQueue.remove(i);
                if(mqttretry != null)
                {
                    mqttretry.setMessage(mqttpubrel);
                    outstandingQueue.put(i, mqttretry);
                }
            }
        }
        catch(MqttPersistenceException mqttpersistenceexception) { }
        return mqttpubrel;
    }

    public boolean outstanding(int i)
    {
        boolean flag = false;
        synchronized(outstandingQueue)
        {
            flag = outstandingQueue.containsKey(i);
        }
        return flag;
    }

    public void process(MqttConnack mqttconnack)
    {
        MQeTrace.trace(this, (short)-30017, 0x200000L, new Integer(mqttconnack.returnCode));
        super.process(mqttconnack);
        notifyAck(1, mqttconnack.returnCode);
    }

    public void process(MqttPuback mqttpuback)
    {
        MQeTrace.trace(this, (short)-30018, 0x200000L, new Integer(mqttpuback.getMsgId()));
        messageAck(mqttpuback.getMsgId());
    }

    public void process(MqttPubcomp mqttpubcomp)
    {
        MQeTrace.trace(this, (short)-30019, 0x200000L, new Integer(mqttpubcomp.getMsgId()));
        messageAck(mqttpubcomp.getMsgId());
    }

    public void process(MqttPublish mqttpublish)
    {
        boolean flag = false;
        int i = 0;
        if(mqttpublish.getPayload() != null)
            i = mqttpublish.getPayload().length;
        MQeTrace.trace(this, (short)-30020, 0x200000L, Integer.toString(mqttpublish.getMsgId()), Integer.toString(mqttpublish.getQos()), (new Boolean(mqttpublish.isRetain())).toString(), Integer.toString(i));
        if(mqttpublish.getQos() != 2)
            try
            {
                publishArrived(mqttpublish.topicName, mqttpublish.getPayload(), mqttpublish.getQos(), mqttpublish.isRetain());
            }
            catch(Exception exception)
            {
                flag = true;
                System.out.println((new StringBuilder("publishArrived Exception caught (QoS ")).append(mqttpublish.getQos()).append("):").toString());
                exception.printStackTrace();
            }
        if(mqttpublish.getQos() > 0 && !flag)
        {
            MQeTrace.trace(this, (short)-30021, 0x200000L, mqttpublish.getQos() == 1 ? "PUBACK" : "PUBREC", new Integer(mqttpublish.getMsgId()));
            if(mqttpublish.getQos() == 1)
            {
                MqttPuback mqttpuback = new MqttPuback();
                mqttpuback.setMsgId(mqttpublish.getMsgId());
                try
                {
                    writePacket(mqttpuback);
                }
                catch(Exception exception2) { }
            } else
            {
                try
                {
                    if(persistenceLayer != null)
                        try
                        {
                            synchronized(persistenceLayer)
                            {
                                byte abyte0[] = mqttpublish.toBytes();
                                if(mqttpublish.getPayload() != null)
                                    abyte0 = MqttUtils.concatArray(abyte0, mqttpublish.getPayload());
                                persistenceLayer.addReceivedMessage(mqttpublish.getMsgId(), abyte0);
                            }
                        }
                        catch(MqttPersistenceException mqttpersistenceexception)
                        {
                            throw mqttpersistenceexception;
                        }
                        catch(Exception exception1)
                        {
                            throw new MqttPersistenceException((new StringBuilder("process(MqttPublish) - packet.toBytes() failed - msgid ")).append(mqttpublish.getMsgId()).toString());
                        }
                    qos2PubsArrived.put(Integer.toString(mqttpublish.getMsgId()), mqttpublish);
                    MqttPubrec mqttpubrec = new MqttPubrec();
                    mqttpubrec.setMsgId(mqttpublish.getMsgId());
                    try
                    {
                        writePacket(mqttpubrec);
                    }
                    catch(Exception exception3) { }
                }
                catch(MqttPersistenceException mqttpersistenceexception1) { }
            }
        }
    }

    public void process(MqttPubrec mqttpubrec)
    {
        MQeTrace.trace(this, (short)-30022, 0x200000L, new Integer(mqttpubrec.getMsgId()));
        MqttRetry mqttretry = (MqttRetry)outstandingQueue.get(mqttpubrec.getMsgId());
        if(mqttretry == null || mqttretry.getMsgType() != 6)
        {
            MqttPacket mqttpacket = messageAck(mqttpubrec.getMsgId());
            if(mqttpacket != null)
                try
                {
                    writePacket(mqttpacket);
                }
                catch(Exception exception) { }
        }
    }

    public void process(MqttPubrel mqttpubrel)
    {
        boolean flag = false;
        MQeTrace.trace(this, (short)-30023, 0x200000L, new Integer(mqttpubrel.getMsgId()));
        MqttPublish mqttpublish = (MqttPublish)qos2PubsArrived.get(Integer.toString(mqttpubrel.getMsgId()));
        if(mqttpublish != null)
            try
            {
                publishArrived(mqttpublish.topicName, mqttpublish.getPayload(), mqttpublish.getQos(), mqttpublish.isRetain());
            }
            catch(Exception exception)
            {
                flag = true;
                System.out.println("publishArrived Exception caught (QoS 2):");
                exception.printStackTrace();
            }
        if(!flag)
        {
            qos2PubsArrived.remove(Integer.toString(mqttpubrel.getMsgId()));
            Object obj = null;
            try
            {
                if(persistenceLayer != null)
                    synchronized(persistenceLayer)
                    {
                        persistenceLayer.delReceivedMessage(mqttpubrel.getMsgId());
                    }
            }
            catch(MqttPersistenceException mqttpersistenceexception)
            {
                obj = mqttpersistenceexception;
            }
            MqttPubcomp mqttpubcomp = new MqttPubcomp();
            mqttpubcomp.setMsgId(mqttpubrel.getMsgId());
            try
            {
                writePacket(mqttpubcomp);
            }
            catch(Exception exception1) { }
            if(obj != null)
                setRegisteredThrowable((Throwable)obj);
        }
    }

    public void process(MqttSuback mqttsuback)
    {
        MQeTrace.trace(this, (short)-30024, 0x200000L, new Integer(mqttsuback.getMsgId()));
        grantedQoS.put(new Integer(mqttsuback.getMsgId()), new MqttByteArray(mqttsuback.TopicsQoS));
        messageAck(mqttsuback.getMsgId());
    }

    public void process(MqttUnsuback mqttunsuback)
    {
        MQeTrace.trace(this, (short)-30025, 0x200000L, new Integer(mqttunsuback.getMsgId()));
        messageAck(mqttunsuback.getMsgId());
    }

    protected int publish(String s, byte abyte0[], int i, boolean flag)
        throws MqttException, MqttPersistenceException
    {
        int j = 0;
        if(i > 0)
            j = nextMsgId();
        MqttPublish mqttpublish = genPublishPacket(j, i, s, abyte0, flag, false);
        sendPacket(mqttpublish);
        MQeTrace.trace(this, (short)-30026, 0x200000L, new Integer(j), new Integer(i), new Boolean(flag));
        return j;
    }

    public void run()
    {
        MQeTrace.trace(this, (short)-30027, 0x200000L);
        synchronized(readerControl)
        {
            while(!isSocketConnected() && !terminated) 
                try
                {
                    readerControl.wait();
                }
                catch(InterruptedException interruptedexception) { }
        }
        if(!terminated)
        {
            long l = retryPeriod;
            while(!terminated) 
            {
                try
                {
                    process();
                }
                catch(Exception exception1)
                {
                    synchronized(readerControl)
                    {
                        tcpipDisconnect(true);
                        readerControl.notify();
                    }
                    if(isConnected())
                    {
                        try
                        {
                            Thread.sleep(500L);
                        }
                        catch(InterruptedException interruptedexception1) { }
                        setRegisteredThrowable(null);
                        System.out.println("WMQtt client:Lost connection...");
                        (new MqttReconn(this)).start();
                    }
                }
                catch(Throwable throwable)
                {
                    synchronized(readerControl)
                    {
                        tcpipDisconnect(true);
                        readerControl.notify();
                    }
                    setRegisteredThrowable(throwable);
                }
                synchronized(readerControl)
                {
                    while(!isSocketConnected() && !terminated) 
                        try
                        {
                            readerControl.wait();
                        }
                        catch(InterruptedException interruptedexception2) { }
                }
            }
        }
        MQeTrace.trace(this, (short)-30028, 0x200000L);
    }

    public void setRetry(int i)
    {
        if(i < 10)
            i = 10;
        retryPeriod = Math.abs(i * 1000);
    }

    protected int subscribe(String as[], int ai[])
        throws MqttException
    {
        int i = nextMsgId();
        byte abyte0[] = new byte[ai.length];
        grantedQoS.remove(new Integer(i));
        for(int j = 0; j < ai.length; j++)
            abyte0[j] = (byte)ai[j];

        MqttSubscribe mqttsubscribe = new MqttSubscribe();
        mqttsubscribe.setMsgId(i);
        mqttsubscribe.setQos(1);
        mqttsubscribe.topics = as;
        mqttsubscribe.topicsQoS = abyte0;
        mqttsubscribe.setDup(false);
        MQeTrace.trace(this, (short)-30029, 0x200000L, new Integer(i));
        sendPacket(mqttsubscribe);
        return i;
    }

    protected MqttPubrel genPubRelPacket(int i, boolean flag)
    {
        MqttPubrel mqttpubrel = new MqttPubrel();
        mqttpubrel.setMsgId(i);
        mqttpubrel.setDup(flag);
        return mqttpubrel;
    }

    protected int unsubscribe(String as[])
        throws MqttException
    {
        int i = nextMsgId();
        MqttUnsubscribe mqttunsubscribe = new MqttUnsubscribe();
        mqttunsubscribe.setMsgId(i);
        mqttunsubscribe.setQos(1);
        mqttunsubscribe.topics = as;
        mqttunsubscribe.setDup(false);
        MQeTrace.trace(this, (short)-30030, 0x200000L, new Integer(i));
        sendPacket(mqttunsubscribe);
        return i;
    }

    private void sendPacket(MqttPacket mqttpacket)
        throws MqttException, MqttNotConnectedException
    {
        long l = getRetry() * 1000;
        if(!isSocketConnected())
            throw new MqttNotConnectedException();
        if(mqttpacket.getQos() > 0)
        {
            int i = outstandingQueue.size();
            if(i >= maxOutstanding)
            {
                synchronized(outLock)
                {
                    try
                    {
                        if(!outLockNotified)
                            outLock.wait();
                        if(isSocketConnected())
                            outLockNotified = false;
                    }
                    catch(InterruptedException interruptedexception) { }
                }
                if(!isSocketConnected())
                    throw new MqttNotConnectedException();
            }
            if(persistenceLayer != null)
                try
                {
                    synchronized(persistenceLayer)
                    {
                        byte abyte0[] = mqttpacket.toBytes();
                        if(mqttpacket.getPayload() != null)
                            abyte0 = MqttUtils.concatArray(abyte0, mqttpacket.getPayload());
                        persistenceLayer.addSentMessage(mqttpacket.getMsgId(), abyte0);
                    }
                }
                catch(MqttPersistenceException mqttpersistenceexception)
                {
                    throw mqttpersistenceexception;
                }
                catch(Exception exception)
                {
                    throw new MqttPersistenceException((new StringBuilder("sendPacket - toBytes failed, msgid ")).append(mqttpacket.getMsgId()).toString());
                }
            boolean flag = getKeepAlivePeriod() > 0 ? outstandingQueue.size() > 1 : outstandingQueue.size() > 0;
            if(flag)
                l = 0L;
            MqttRetry mqttretry = new MqttRetry(this, mqttpacket, l);
            synchronized(outstandingQueue)
            {
                outstandingQueue.put(mqttpacket.getMsgId(), mqttretry);
            }
            retryQueue.enqueue(mqttretry);
            if(l > 0L)
                try
                {
                    writePacket(mqttpacket);
                }
                catch(MqttException mqttexception) { }
        } else
        {
            writePacket(mqttpacket);
        }
    }

    public static void setWindowSize(int i)
    {
        maxOutstanding = i;
    }

    private MqttHashTable outstandingQueue;
    private int retryPeriod;
    private boolean cleanSession;
    private Hashtable qos2PubsArrived;
    private MqttPersistence persistenceLayer;
    private Object readerControl;
    private MqttTimedEventQueue retryQueue;
    protected static final int conNotify = 1;
    protected static final int subNotify = 4;
    protected static final int unsubNotify = 5;
    private Hashtable grantedQoS;
    private boolean terminated;
    private static int maxOutstanding = 10;
    private boolean outLockNotified;
    private Object outLock;

}
