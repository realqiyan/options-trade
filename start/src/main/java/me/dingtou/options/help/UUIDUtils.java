package me.dingtou.options.help;

import java.util.UUID;

public class UUIDUtils {

    public String uuid() {
        UUID randomUUID = UUID.randomUUID();
        return randomUUID.toString().replaceAll("-", "");
    }

    public static void main(String[] args) {
        System.out.println(new UUIDUtils().uuid());
    }


}
