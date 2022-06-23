package com.brevity.rpc.consumer;

import com.brevity.rpc.consumer.controller.MyController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RpcConsumerApplication {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(RpcConsumerApplication.class, args);
        MyController controller = (MyController) applicationContext.getBean("myController");
        controller.test();
    }
}
