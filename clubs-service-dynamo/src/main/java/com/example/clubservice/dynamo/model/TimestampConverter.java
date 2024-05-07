package com.example.clubservice.dynamo.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;

import java.sql.Timestamp;
import java.time.Instant;

public class TimestampConverter implements DynamoDBTypeConverter<Long, Timestamp> {

    @Override
    public Long convert(Timestamp timestamp) {
        return timestamp.getTime();
    }

    @Override
    public Timestamp unconvert(Long value) {
        return new Timestamp(value);
    }
}
