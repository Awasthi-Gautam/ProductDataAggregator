package com.aggregation.data_aggregation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataAggregationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataAggregationApplication.class, args);
	}

}
