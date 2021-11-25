/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.wa.starter;

import org.apache.syncope.wa.bootstrap.WAProperties;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.syncope.wa.starter.config.SyncopeWARefreshContextJob;

import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.CasConfigurationPropertiesValidator;
import org.apereo.cas.util.spring.BeanContainer;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(exclude = {
    HibernateJpaAutoConfiguration.class,
    JerseyAutoConfiguration.class,
    GroovyTemplateAutoConfiguration.class,
    GsonAutoConfiguration.class,
    JmxAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    RedisAutoConfiguration.class,
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    CassandraAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
}, proxyBeanMethods = false)
@EnableConfigurationProperties({ WAProperties.class, CasConfigurationProperties.class })
@EnableAsync
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EnableScheduling
public class SyncopeWAApplication extends SpringBootServletInitializer {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeWAApplication.class);

    public static void main(final String[] args) {
        new SpringApplicationBuilder(SyncopeWAApplication.class).
                properties("spring.config.name:wa").
                contextFactory(webApplicationType -> new SyncopeWAApplicationContext()).
                build().run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        return builder.properties(Map.of("spring.config.name", "wa")).
            contextFactory(webApplicationType -> new SyncopeWAApplicationContext()).
            sources(SyncopeWAApplication.class);
    }

    @RestController("cas")
    public static class MyController {
        @Autowired
        private CasConfigurationProperties properties;

        @Autowired
        @Qualifier("syncopeAuthenticationHandlers")
        private BeanContainer<AuthenticationHandler> syncopeAuthenticationHandlers;

        @Autowired
        private ConfigurableApplicationContext applicationContext;
        
        @GetMapping(produces = "application/json")
        public Map value() {
            var map = new LinkedHashMap<>();
            map.put("configName", properties.getAuthn().getSyncope().getName());
            map.put("handlerNames", syncopeAuthenticationHandlers.toList().stream().map(h -> h.getName()).collect(Collectors.joining()));
            map.put("envName", applicationContext.getEnvironment().getProperty("cas.authn.syncope.name"));
            return map;
        }
    }
    
//    @EventListener
//    public void handleApplicationReadyEvent(final ApplicationReadyEvent event) {
//        new CasConfigurationPropertiesValidator(event.getApplicationContext()).validate();
//        scheduleJobToRefreshContext(event.getApplicationContext());
//    }
//
//    protected void scheduleJobToRefreshContext(final ApplicationContext applicationContext) {
//        try {
//            WAProperties waProperties = applicationContext.getBean(WAProperties.class);
//            Date date = Date.from(LocalDateTime.now().plusSeconds(waProperties.getContextRefreshDelay()).
//                    atZone(ZoneId.systemDefault()).toInstant());
//            Trigger trigger = TriggerBuilder.newTrigger().startAt(date).build();
//            JobKey jobKey = new JobKey(getClass().getSimpleName());
//
//            JobDetail job = JobBuilder.newJob(SyncopeWARefreshContextJob.class).
//                    withIdentity(jobKey).
//                    build();
//            LOG.info("Scheduled job to refresh application context @ [{}]", date);
//
//            SchedulerFactoryBean scheduler = applicationContext.getBean(SchedulerFactoryBean.class);
//            scheduler.getScheduler().scheduleJob(job, trigger);
//        } catch (final SchedulerException e) {
//            throw new RuntimeException("Could not schedule refresh job", e);
//        }
//    }

    static class SyncopeWAApplicationContext extends AnnotationConfigServletWebServerApplicationContext {
    }
}
