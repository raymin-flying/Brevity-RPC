package com.brevity.rpc.facade;

/**
 * 怎么知道服务方提供了哪些服务接口呢？
 * 答：可以通过maven等工具，把这些服务接口依赖到我们项目中，我们就知道服务提供方提供了哪些服务了
 */
public interface ArrayFacade {

    String hello(String name);

}
