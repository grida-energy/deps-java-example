# DEPS (Distributed Energy Protocol System) Java 사용 예제

## 1 설치

openjdk 21 버전 이상 설치 필요

## 2 인증서 관련 (선택적)

openssl 등으로 인증서를 발급했다면 JAVA에서 사용하는 형식(jks)으로 변환 필요

CA 인증서 변환 (ca.crt.pem => truststore.jks)

```bash
keytool -J-Duser.language=en -import -trustcacerts -alias root-ca -file ca.crt.pem -keystore truststore.jks
```

클라이언트 인증서 변환 (client.crt.pem + client.key.pem => client.p12 => client.jks)

```bash
openssl pkcs12 -export -in client.crt.pem -inkey client.key.pem -out client.p12 -name "client.mqtt"
keytool -J-Duser.language=en -importkeystore -srckeystore client.p12 -srcstoretype PKCS12 -destkeystore client.jks
```

## 3 실행

gradle로 실행 시 --args로 인자를 명시하거나 property 파일(--config 로 지정)을 통해 인자를 설정

커맨드 라인에서 인자를 전달 시 예제

```bash
./gradlew run --args='--mqtt-url=ssl://mqtt-server.com:1883 ...인자들'
```

property 파일을 이용한 예제 (파일 경로는 app 디렉터리에 대한 상대적 경로)

```bash
./gradlew run --args='--config=../config.properties'
```

커맨드 라인 옵션

```txt
Usage: deps-java-example [-hV] [--client-id=<client_id>] [--config=<config>]
                         [--keystore=<keystore>]
                         [--keystore-password=<keystore_password>]
                         [--mqtt-url=<mqtt_url>] [--topic=<topic>]
                         [--truststore=<truststore>]
                         [--truststore-password=<truststore_password>]
      --client-id=<client_id>
                          client id
      --config=<config>   config file path
  -h, --help              Show this help message and exit.
      --keystore=<keystore>
                          keystore path (client certificate)
      --keystore-password=<keystore_password>
                          keystore password phrase
      --mqtt-url=<mqtt_url>
                          MQTT brokwer url
      --topic=<topic>     mqtt topic prefix
      --truststore=<truststore>
                          truststore path (CA certificate)
      --truststore-password=<truststore_password>
                          truststore password phrase
  -V, --version           Print version information and exit.
```

property 파일 예제 (파일 경로는 app 디릭터리에 대한 상대적 경로)

```txt
mqtt-url=ssl://mqtt-server.com:1883 or tcp://mqtt-server.com:1883
client-id=java.example
topic=test-lab/bess/pcs-0
truststore=../cert/truststore.jks
truststore-password=some-password
keystore=../cert/client.jks
keystore-password=some-password
```
