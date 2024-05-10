package com.example.clubservice.dynamo.base;

import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

public class AmazonDynamoDBEmbeddedContextCustomizer implements ContextCustomizer {
    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {

        DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory)context.getBeanFactory();
        AmazonDynamoDBLocal embedded = DynamoDBEmbedded.create();
        listableBeanFactory.registerSingleton("amazonDynamoDB", embedded.amazonDynamoDB());
        listableBeanFactory.registerSingleton("amazonDynamoDBEmbedded", embedded);
        listableBeanFactory.registerDisposableBean("amazonDynamoDBEmbedded", new DisposableBean() {
            @Override
            public void destroy() throws Exception {
                embedded.shutdown();
            }
        });
        listableBeanFactory.registerSingleton("dynamoDbClient", embedded.dynamoDbClient());
    }
}
