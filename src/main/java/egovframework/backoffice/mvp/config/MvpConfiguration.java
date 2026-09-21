package egovframework.backoffice.mvp.config;

import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@Profile("!stage0")
@ComponentScan("egovframework.backoffice.mvp")
@EnableTransactionManagement
public class MvpConfiguration {
    @Bean(name = {"sqlSession", "sqlSessionFactory"})
    @DependsOnDatabaseInitialization
    SqlSessionFactory sqlSessionFactory(DataSource dataSource, DataSourceProperties properties) throws Exception {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new IllegalStateException("DB 연결 설정이 필요합니다. 개발 환경에서는 dev 프로필을 사용하세요.");
        }
        var configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setArgNameBasedConstructorAutoMapping(true);
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        return factory.getObject();
    }

    @Bean
    LeaveaTrace leaveaTrace() { return new LeaveaTrace(); }
}
