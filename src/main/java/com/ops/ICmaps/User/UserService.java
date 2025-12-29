
package com.ops.ICmaps.User;
import org.springframework.stereotype.Service;
@Service
public class UserService{

    private UserRepository ur;

    public UserService(UserRepository ur){
        this.ur = ur;
    }
}