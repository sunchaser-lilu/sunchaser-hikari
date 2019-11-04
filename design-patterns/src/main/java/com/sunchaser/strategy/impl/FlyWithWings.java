package com.sunchaser.strategy.impl;

import com.sunchaser.strategy.FlyBehavior;

/**
 * @author: sunchaser
 * @date: 2019/9/12
 * @description: 可以飞的行为
 */
public class FlyWithWings implements FlyBehavior {
    @Override
    public void fly() {
        System.out.println("fly with wings");
    }
}
