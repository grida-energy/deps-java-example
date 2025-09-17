package org.example;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Properties;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import io.github.grida_energy.deps.preset.bess.v1.ModelV1.BessMeasure;
import io.github.grida_energy.deps.vnd.v1.ParameterV1.ParamMeta;
import io.github.grida_energy.deps.vnd.v1.AlarmV1.AlarmMeta;
import io.github.grida_energy.deps.vnd.v1.AlarmV1.AlarmData;

@Command(name = "deps-java-example", mixinStandardHelpOptions = true, version = "1.0")
class CliOption implements Runnable {
    @Option(names = { "--config" }, description = "config file path")
    public String config;

    @Option(names = { "--mqtt-url" }, description = "MQTT brokwer url")
    public String mqtt_url;
    @Option(names = { "--client-id" }, description = "client id")
    public String client_id;
    @Option(names = { "--truststore" }, description = "truststore path (CA certificate)")
    public String truststore;
    @Option(names = { "--truststore-password" }, description = "truststore password phrase")
    public String truststore_password;

    @Option(names = { "--keystore" }, description = "keystore path (client certificate)")
    public String keystore;
    @Option(names = { "--keystore-password" }, description = "keystore password phrase")
    public String keystore_password;

    @Option(names = { "--topic" }, description = "mqtt topic prefix")
    public String topic;

    @Override
    public void run() {
    }
}

public class SubExample {

    public static class SimpleMqttCallback implements MqttCallback {
        String topic_prefix;

        public SimpleMqttCallback(String topic) {
            this.topic_prefix = topic;
        }

        @Override
        public void connectionLost(Throwable cause) {
            System.out.println("Connection to MQTT broker lost! " + cause.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            try {
                String sub_topic = "-";
                if (topic.startsWith(topic_prefix)) {
                    sub_topic = topic.substring(topic_prefix.length());
                }
                switch (sub_topic) {
                    case "/measure":
                        BessMeasure measure = BessMeasure.parseFrom(message.getPayload());
                        System.out.println("Received message: " + measure.toString() + " on topic " + topic);
                        break;
                    case "/vnd/param-meta":
                        ParamMeta paramMeta = ParamMeta.parseFrom(message.getPayload());
                        System.out.println("Received message: " + paramMeta.toString() + " on topic " + topic);
                        break;
                    case "/vnd/alarm-meta":
                        AlarmMeta alarmMeta = AlarmMeta.parseFrom(message.getPayload());
                        System.out.println("Received message: " + alarmMeta.toString() + " on topic " + topic);
                        break;
                    case "/vnd/alarm":
                        AlarmData alarmData = AlarmData.parseFrom(message.getPayload());
                        System.out.println("Received message: " + alarmData.toString() + " on topic " + topic);
                        break;
                    default:
                        System.out.println("Unknown topic: " + topic + ", " + sub_topic);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            System.out.println("Delivery of message with token " + token.getMessageId() + " complete.");
        }
    }

    static void check_options(CliOption opts) {
        if (opts.mqtt_url == null || opts.mqtt_url.isEmpty()) {
            throw new IllegalArgumentException("mqtt-url is required");
        }
        if (opts.client_id == null) {
            opts.client_id = "";
        }
        Boolean use_ssl = opts.mqtt_url.startsWith("ssl://") || opts.mqtt_url.startsWith("wss://");
        if (use_ssl && (opts.truststore == null || opts.truststore.isEmpty())) {
            throw new IllegalArgumentException("truststore is required when using ssl scheme");
        }
        if (opts.truststore_password == null) {
            opts.truststore_password = "";
        }
        if (use_ssl && (opts.keystore == null || opts.keystore.isEmpty())) {
            throw new IllegalArgumentException("keystore is required when using ssl scheme");
        }
        if (opts.keystore_password == null) {
            opts.keystore_password = "";
        }
        if (opts.topic == null || opts.topic.isEmpty()) {
            throw new IllegalArgumentException("topic is required");
        }
    }

    static SocketFactory make_sock(CliOption opts) throws Exception {
        SocketFactory socketFactory = null;

        if (opts.truststore == null || opts.truststore.isEmpty()) {
            socketFactory = SocketFactory.getDefault();
        } else {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(new FileInputStream(opts.truststore),
                    opts.truststore_password.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(new FileInputStream(opts.keystore),
                    opts.keystore_password.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, opts.keystore_password.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(), null);

            socketFactory = sslContext.getSocketFactory();
        }
        return socketFactory;
    }

    public static void main(String[] args) {
        CliOption opts = new CliOption();
        new CommandLine(opts).execute(args);

        if (opts.config != null) {
            Properties prop = new Properties();
            try (InputStream input = new FileInputStream(opts.config)) {
                prop.load(input);
                opts.mqtt_url = prop.getProperty("mqtt-url", opts.mqtt_url);
                opts.client_id = prop.getProperty("client-id", opts.client_id);
                opts.topic = prop.getProperty("topic", opts.topic);
                opts.truststore = prop.getProperty("truststore", opts.truststore);
                opts.truststore_password = prop.getProperty("truststore-password", opts.truststore_password);
                opts.keystore = prop.getProperty("keystore", opts.keystore);
                opts.keystore_password = prop.getProperty("keystore-password", opts.keystore_password);
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
        }
        try {
            check_options(opts);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid argument: " + e.getMessage());
            return;
        }

        SocketFactory socketFactory = null;
        try {
            socketFactory = make_sock(opts);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try (MqttClient client = new MqttClient(opts.mqtt_url, opts.client_id)) {
            client.setCallback(new SimpleMqttCallback(opts.topic));

            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(socketFactory);
            options.setCleanSession(true);

            System.out.println("Connecting to broker: " + opts.mqtt_url);
            client.connect(options);
            System.out.println("Connected!");

            client.subscribe(opts.topic + "/#");

            System.out.println("Listening for messages on topic: " + opts.topic);
            Thread.sleep(600000);

            System.out.println("Done");

        } catch (MqttException me) {
            me.printStackTrace();
        } catch (InterruptedException ie) {

        }
    }
}
