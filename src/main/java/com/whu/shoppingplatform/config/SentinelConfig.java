package com.whu.shoppingplatform.config;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initRules() {
        initFlowRules();
        initDegradeRules();
    }

    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule seckillRule = new FlowRule();
        seckillRule.setResource("seckill-order");
        seckillRule.setCount(100);
        seckillRule.setGrade(FlowRule.FLOW_GRADE_QPS);
        seckillRule.setLimitApp("default");
        rules.add(seckillRule);

        FlowRule orderQueryRule = new FlowRule();
        orderQueryRule.setResource("order-query");
        orderQueryRule.setCount(200);
        orderQueryRule.setGrade(FlowRule.FLOW_GRADE_QPS);
        orderQueryRule.setLimitApp("default");
        rules.add(orderQueryRule);

        FlowRule goodsQueryRule = new FlowRule();
        goodsQueryRule.setResource("goods-query");
        goodsQueryRule.setCount(300);
        goodsQueryRule.setGrade(FlowRule.FLOW_GRADE_QPS);
        goodsQueryRule.setLimitApp("default");
        rules.add(goodsQueryRule);

        FlowRuleManager.loadRules(rules);
    }

    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        DegradeRule seckillDegrade = new DegradeRule();
        seckillDegrade.setResource("seckill-order");
        seckillDegrade.setGrade(DegradeRule.CIRCUIT_BREAKER_STRATEGY_SLOW_REQUEST_RATIO);
        seckillDegrade.setCount(0.5);
        seckillDegrade.setTimeWindow(10);
        seckillDegrade.setMinRequestAmount(5);
        seckillDegrade.setSlowRatioThreshold(0.5);
        rules.add(seckillDegrade);

        DegradeRule orderDegrade = new DegradeRule();
        orderDegrade.setResource("order-query");
        orderDegrade.setGrade(DegradeRule.CIRCUIT_BREAKER_STRATEGY_EXCEPTION_RATIO);
        orderDegrade.setCount(0.5);
        orderDegrade.setTimeWindow(10);
        orderDegrade.setMinRequestAmount(5);
        rules.add(orderDegrade);

        DegradeRuleManager.loadRules(rules);
    }
}