package cn.edu.xidian.cephos.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * ceph连接自动装配类
 */
@Configuration
@EnableConfigurationProperties(CephProperties.class)
@ComponentScan(basePackages = "cn.edu.xidian.cephos.*")
public class CephAutoConfiguration {
    @Resource
    private CephProperties properties;

    /**
     * 构造ceph连接
     * @return ceph连接bean
     */
    @Bean
    @ConditionalOnMissingBean(AmazonS3.class)
    public AmazonS3 cephConnection(){
        AWSCredentials credentials = new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey());

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setMaxConnections(properties.getMaxConn());

        /*
        ceph集群 https配置
        https://blog.csdn.net/bxzhu/article/details/104206549
        生成证书时保证Common Name与ceph域名或IP保持一致
        将所访问的SSL站点证书添加至JVM。
        echo -n |openssl s_client -connect 192.168.28.137:443|sed -ne'/BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > xxxxx.cert
        此命令获取服务端证书链。
        keytool -importcert -alias 192.168.28.137-ceph -keystore $JAVA_HOME/jre/lib/security/cacerts -storepass changeit -file xxxxx.cert
        此命令导入上述证书到JVM的证书库中。
         */
        if(properties.getProtocol().equalsIgnoreCase("https")) clientConfig.setProtocol(Protocol.HTTPS);
        else clientConfig.setProtocol(Protocol.HTTP);

        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfig)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(properties.getEndpoint(), null))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
