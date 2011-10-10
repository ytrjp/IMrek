package com.ibm.mqtt;

public interface IMqttClient
{

    public abstract void startTrace()
        throws MqttException;

    public abstract void stopTrace();

    public abstract MqttPersistence getPersistence();

    public abstract void registerAdvancedHandler(MqttAdvancedCallback mqttadvancedcallback);

    public abstract void registerSimpleHandler(MqttSimpleCallback mqttsimplecallback);

    public abstract void connect(String s, boolean flag, short word0)
            throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException;
    
    public abstract void connect(String s, boolean flag, short word0, String user, String pass)
            throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException;

    public abstract void connect(String s, boolean flag, short word0, String s1, int i, String s2, boolean flag1)
            throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException;
    
    public abstract void connect(String s, boolean flag, short word0, String s1, int i, String s2, boolean flag1, String user, String pass)
            throws MqttException, MqttPersistenceException, MqttBrokerUnavailableException, MqttNotConnectedException;

    public abstract void disconnect()
        throws MqttPersistenceException;

    public abstract String getConnection();

    public abstract int getRetry();

    public abstract boolean isConnected();

    public abstract boolean outstanding(int i);

    public abstract void ping()
        throws MqttException;

    public abstract int publish(String s, byte abyte0[], int i, boolean flag)
        throws MqttNotConnectedException, MqttPersistenceException, MqttException, IllegalArgumentException;

    public abstract void setRetry(int i);

    public abstract void terminate();

    public abstract int subscribe(String as[], int ai[])
        throws MqttNotConnectedException, MqttException, IllegalArgumentException;

    public abstract int unsubscribe(String as[])
        throws MqttNotConnectedException, MqttException, IllegalArgumentException;

    public static final String TCP_ID = "tcp://";
    public static final String LOCAL_ID = "local://";
}
