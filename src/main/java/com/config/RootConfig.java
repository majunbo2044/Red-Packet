package com.config;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
//扫描依赖注入
@ComponentScan(value="com.service.*",includeFilters = {@Filter(type = FilterType.ANNOTATION,value = {Service.class})})
@EnableTransactionManagement//使用事务驱动管理
//实现TransactionManagementConfigurer,可以配置注解驱动事务
public class RootConfig implements TransactionManagementConfigurer {
    private DataSource dataSource =null;

    //配置数据源
    @Bean(name="dataSource")
    public DataSource initDataSource(){
        if(dataSource!=null){
            return dataSource;
        }

        /*
        批处理：
        url添加rewriteBatchedStatements=true
        开始保存数据
        耗时：5917插入数量:20000
        url不添加
                开始保存数据
        耗时：13315插入数量:20000
        所以批量处理一定要加上rewriteBatchedStatements=true*/
        Properties props = new Properties();
        props.setProperty("driverClassName","com.mysql.cj.jdbc.Driver");
        props.setProperty("url","jdbc:mysql://127.0.0.1:3306/red?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC&useSSL=false&rewriteBatchedStatements=true");//启动批处理操作)&rewriteBatchedStatements=true
        props.setProperty("username","root");
        props.setProperty("password","123456");
        props.setProperty("maxActive","200");
        props.setProperty("maxIdle","20");
        props.setProperty("maxWait","30000");
        try{
            dataSource = BasicDataSourceFactory.createDataSource(props);

        }catch (Exception e){
            e.printStackTrace();
        }
        return dataSource;
    }
    //配置sqlSessionFactoryBean
    @Bean(name="sqlSessionFactory")
    public SqlSessionFactoryBean initSqlSessionFactory(){

        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(initDataSource());
        Resource resource = new ClassPathResource("mybatis/mybatis-config.xml");
        sqlSessionFactoryBean.setConfigLocation(resource);
        return sqlSessionFactoryBean;
    }
    //通过自动扫描发现mybatis mapper接口 返回扫描器
    @Bean
    public MapperScannerConfigurer initMapperScannerConfigurer(){
        MapperScannerConfigurer msc = new MapperScannerConfigurer();
        msc.setBasePackage("com.*");
        msc.setSqlSessionFactoryBeanName("sqlSessionFactory");
        msc.setAnnotationClass(Repository.class);//扫描类通过注解@Repository和包名”com.*“限定
        return msc;
    }
    //实现接口方法，注册注解事务，当@Transactional使用的时候产生数据库事务
    @Override
    @Bean(name="annotationDrivenTransactionManager")
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(initDataSource());
        return transactionManager;
    }
    @Bean(name = "redisTemplate")
    public RedisTemplate initRedisTemplate() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        //最大空闲数
        poolConfig.setMaxIdle(50);
        //最大连接数
        poolConfig.setMaxTotal(100);
        //最大等待毫秒数
        poolConfig.setMaxWaitMillis(20000);
        //创建Jedis链接工厂
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(poolConfig);
        connectionFactory.setHostName("localhost");
        connectionFactory.setPort(6379);
        //调用后初始化方法，没有他会报错
        connectionFactory.afterPropertiesSet();
        //自定义Redis序列化器
        RedisSerializer jdkSerializationRedisSerializer = new JdkSerializationRedisSerializer();
        RedisSerializer stringRedisSerializer = new StringRedisSerializer();
        //自定义RedisTemplate并设置链接工厂
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        //设置序列化
        redisTemplate.setDefaultSerializer(stringRedisSerializer);
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(stringRedisSerializer);
        return redisTemplate;
    }

}