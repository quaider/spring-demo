package org.example.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {

    private final StringRedisTemplate stringRedisTemplate;

    @GetMapping("/init")
    public String setData() {

        for (int i = 0; i < 3000; i++) {
            stringRedisTemplate.opsForValue().set("a-demo-key-" + i, "demo value-" + i);
        }

        return "ok";
    }

    @GetMapping("/get")
    public String getData(int suffix) {
        return stringRedisTemplate.opsForValue().get("a-demo-key-" + suffix);
    }
}
