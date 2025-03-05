package me.dingtou.options.service.impl;

import me.dingtou.options.model.Message;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class AIChatServiceImplTest {


    @Test
    void chat() {
        final String[] lastType = {null};
        new AIChatServiceImpl().chat("你是谁？", new Function<Message, Void>() {
            @Override
            public Void apply(Message message) {
                if (lastType[0] == null) {
                    System.out.print(message.getType());
                    lastType[0] = message.getType();
                } else if (!lastType[0].equals(message.getType())) {
                    System.out.println();
                    System.out.print(message.getType());
                    lastType[0] = message.getType();
                }
                System.out.print(message.getMessage());
                return null;
            }
        });
    }
}