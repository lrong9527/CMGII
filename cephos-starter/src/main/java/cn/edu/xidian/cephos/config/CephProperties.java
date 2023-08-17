package cn.edu.xidian.cephos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ceph配置类
 */
@Data
@ConfigurationProperties(prefix = "ceph")
public class CephProperties {
    /**
     * 访问ceph的accessKey
     */
    private String accessKey = "W6CM6FKY9PG55TDBF2LB";
    /**
     * 访问ceph的secretKey
     */
    private String secretKey = "bp5kimAYvQXeDS03351TVzxBAFLW0hpjBvWCFJCK";
    /**
     * 访问ceph的网关后端节点地址
     */
    private String endpoint = "192.168.203.51:7480";
    /**
     * 访问ceph的网关后端节点协议，支持http与https
     */
    private String protocol = "http";
    /**
     * 使用config.setMaxConnections(maxConn) 设置客户端http最大连接数(不设置则默认50) 使用apache httpclient内置的连接池管理
     */
    private int maxConn = 50;
}
