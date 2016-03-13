package org.schemarepo.config;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.schemarepo.InMemoryRepository;
import org.schemarepo.Repository;
import org.schemarepo.ValidatorFactory;
import org.schemarepo.json.GsonJsonUtil;
import org.schemarepo.json.JsonUtil;

import javax.inject.Singleton;
import java.util.Properties;

public class ConfigBinder extends AbstractBinder {

    private final Properties props;

    public ConfigBinder(Properties props) {
        Properties copy = new Properties(Config.DEFAULTS);
        copy.putAll(props);
        this.props = copy;
    }

    @Override
    protected void configure() {
        bind(InMemoryRepository.class).to(Repository.class);
        bindFactory(ValidatorFactoryProvider.class).to(ValidatorFactory.class).in(Singleton.class);
        bind(GsonJsonUtil.class).to(JsonUtil.class);
        bind(props).to(Properties.class);
    }

    static class ValidatorFactoryProvider implements Factory<ValidatorFactory> {

        @Override
        public ValidatorFactory provide() {
            ValidatorFactory.Builder builder = new ValidatorFactory.Builder();
            return builder.build();
        }

        @Override
        public void dispose(ValidatorFactory validatorFactory) {

        }
    }
}
