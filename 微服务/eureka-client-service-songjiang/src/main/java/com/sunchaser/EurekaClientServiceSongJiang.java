package com.sunchaser;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @auther: sunchaser
 * @date: 2019/10/22
 * @description:
 * @since 1.0
 */
@SpringBootApplication
public class EurekaClientServiceSongJiang {
    public static void main(String[] args) {
        new SpringApplicationBuilder(EurekaClientServiceSongJiang.class).web(WebApplicationType.SERVLET).run(args);
    }
}
