package com.steam.cache.demo;


import com.steam.cache.annotation.SteamCache;
import com.steam.cache.dto.UserDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @SteamCache
    @RequestMapping(value = "/user/list",method = RequestMethod.GET)
    @ResponseBody
    public List<UserDTO> getUserList(){
        System.out.println("===>invoke getUserList method begin");
        List<UserDTO> userList = new ArrayList<>();

        //这种写法，会识别成add了一个DemoController(本类)内部类的对象，数据返回上不认为是UserDTO对象；
        /*userList.add(new UserDTO(){{
            setId(123L);
            setCode("0123");
            setName("张三");
            setTelephone("666666");
            setEmail("666666@qq.com");
        }});*/

        UserDTO u1 = new UserDTO();
        u1.setId(456L);
        u1.setCode("0456");
        u1.setName("王五");
        u1.setTelephone("999999");
        u1.setEmail("999999@qq.com");
        userList.add(u1);

        UserDTO u2 = new UserDTO();
        u2.setId(234L);
        u2.setCode("0234");
        u2.setName("李四");
        u2.setTelephone("888888");
        u2.setEmail("888888@qq.com");
        userList.add(u2);

        System.out.println("===>invoke getUserList method end");
        return userList;
    }
}
