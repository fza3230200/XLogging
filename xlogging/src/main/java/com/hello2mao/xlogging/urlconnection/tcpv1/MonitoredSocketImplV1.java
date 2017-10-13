package com.hello2mao.xlogging.urlconnection.tcpv1;

import android.util.Log;

import com.hello2mao.xlogging.Constant;
import com.hello2mao.xlogging.urlconnection.MonitoredSocketInterface;
import com.hello2mao.xlogging.urlconnection.NetworkMonitor;
import com.hello2mao.xlogging.urlconnection.NetworkTransactionState;
import com.hello2mao.xlogging.urlconnection.UrlBuilder;
import com.hello2mao.xlogging.urlconnection.iov1.HttpRequestParsingOutputStreamV1;
import com.hello2mao.xlogging.urlconnection.iov1.HttpResponseParsingInputStreamV1;
import com.hello2mao.xlogging.util.URLUtil;
import com.hello2mao.xlogging.xlog.XLog;
import com.hello2mao.xlogging.xlog.XLogManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PlainSocketImpl;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;

public class MonitoredSocketImplV1 extends PlainSocketImpl implements MonitoredSocketInterface {

    private static final XLog log = XLogManager.getAgentLog();
    private int connectTime;
    private Queue<NetworkTransactionState> transactionStates;
    private String address;
    private HttpResponseParsingInputStreamV1 inputStream;
    private HttpRequestParsingOutputStreamV1 outputStream;

    public MonitoredSocketImplV1() {
        transactionStates = new LinkedList<>();
        this.address = "";
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (inputStream != null) {
            inputStream.notifySocketClosing();
        }
    }

    @Override
    public void connect(String host, int port) throws IOException {
        super.connect(host, port);
        log.error("Unexpected: MonitoredSocketImplV1 connect-1");
    }

    @Override
    public void connect(InetAddress inetAddress, int port) throws IOException {
        super.connect(inetAddress, port);
        log.error("Unexpected: MonitoredSocketImplV1 connect-2");
    }

    @Override
    public void connect(SocketAddress socketAddress, int timeout) throws IOException {
        // /220.181.57.112:443
        // ip.taobao.com/140.205.140.33:80
        String host = "";
        String ipAddress = "";
        try {
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                // 220.181.57.112
                ipAddress = URLUtil.getIpAddress(inetSocketAddress);
                // ip.taobao.com
                host = URLUtil.getHost(inetSocketAddress);
                address = ipAddress;
                Log.d(Constant.TAG, "connect V1 ..3 address:" + address + " host:" + host);
            }
            long currentTimeMillis = System.currentTimeMillis();
            super.connect(socketAddress, timeout);
            this.connectTime = (int) (System.currentTimeMillis() - currentTimeMillis);
            if (this.port == 443) {
                // FIXME: 17/9/22    why not   this.connectTime = (int) (System.currentTimeMillis() - currentTimeMillis);
                NetworkMonitor.addConnectSocketInfo(ipAddress, host, this.connectTime);
            }
            Log.d(Constant.TAG, "connectTime V1  ..3:" + connectTime);
        } catch (IOException ex) {
            // TODO
            throw ex;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            InputStream inputStream = super.getInputStream();
            return this.inputStream = new HttpResponseParsingInputStreamV1(this, inputStream);
        } catch (IOException ex) {
            // TODO
            throw ex;
        }
    }

    @Override
    public Object getOption(int n) throws SocketException {
        return super.getOption(n);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        Log.d(Constant.TAG, "v1 getOutputStream..");
        try {
            OutputStream outputStream = super.getOutputStream();
//            return outputStream;
            if (outputStream == null) {
                Log.d(Constant.TAG, "v1 getOutputStream..null");
                return null;
            }
            return this.outputStream = new HttpRequestParsingOutputStreamV1(this, outputStream);
        } catch (IOException ex) {
            // TODO
            throw ex;
        }
    }

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        super.setOption(optID, value);
    }

    @Override
    public NetworkTransactionState createNetworkTransactionState() {
        NetworkTransactionState networkTransactionState = new NetworkTransactionState();
        networkTransactionState.setAddress((this.address == null) ? "" : this.address);
        networkTransactionState.setPort(this.port);
        if (this.port == 443) {
            networkTransactionState.setScheme(UrlBuilder.Scheme.HTTPS);
        }
        else {
            networkTransactionState.setScheme(UrlBuilder.Scheme.HTTP);
        }
//        networkTransactionState.setCarrier(Agent.getActiveNetworkCarrier());
        Log.d(Constant.TAG, "monitoredSockedImplV1 networkTransactionState setconnectTime:" + connectTime);
        networkTransactionState.setTcpHandShakeTime(this.connectTime);
        return networkTransactionState;
    }

    @Override
    public NetworkTransactionState dequeueNetworkTransactionState() {
        synchronized (this.transactionStates) {
            Log.d(Constant.TAG, "v1 dequeue transaction");
            return this.transactionStates.poll();
        }
    }

    @Override
    public void enqueueNetworkTransactionState(NetworkTransactionState networkTransactionState) {
        synchronized (this.transactionStates) {
            Log.d(Constant.TAG, "v1 enqueue transaction");
            this.transactionStates.add(networkTransactionState);
        }
    }
}