package ru.ctf;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import ru.ctf.dao.CommandDao;
import ru.ctf.dao.CommandDaoImpl;

import javax.sql.DataSource;

@Configuration
@PropertySource({"classpath:application.properties"})
public class JpaConfig {
    @Value("${jpa.connection.url}")
    private String url;
    @Value("${jpa.connection.username}")
    private String username;
    @Value("${jpa.connection.password}")
    private String password;

    public JpaConfig() {
    }
    @Autowired
    DataSource dataSource;

    @Bean
    public CommandDao commandDao() {
        CommandDao commandDao = new CommandDaoImpl();
        return commandDao;
    }


    @Bean
    public DataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(this.url);
        dataSource.setUser(this.username);
        dataSource.setPassword(this.password);
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("ru.ctf");
        factory.setDataSource(dataSource);
        return factory;
    }

}
