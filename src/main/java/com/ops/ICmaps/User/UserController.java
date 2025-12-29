package com.ops.ICmaps.User;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;



@RestController
@CrossOrigin
@RequestMapping("/user")
public class UserController {
    
    private final UserRepository ur;
    private final ObjectMapper objectMapper;
    public UserController(UserRepository ur, ObjectMapper objectMapper){
        this.ur = ur;
        this.objectMapper  = objectMapper;
    }

    @GetMapping("/")
    public User getUser(@RequestParam String email) {
        
        return (User) ur.findByEmail(email);
    }

    @PutMapping("/set-is-admin")
    public void setIsAdmin(@RequestBody ObjectNode args) {

        String email = args.get("email").asText();
        Boolean isAdmin = args.get("isAdmin").asBoolean();
        User curUser= (User) ur.findByEmail(email);

        curUser.setIsAdmin(isAdmin);
        ur.save(curUser);

    }
 
    @PutMapping("/set-is-route-manager")
    public void setIdRouteManager(@RequestBody ObjectNode args) {

        String email = args.get("email").asText();
        Boolean isRouteManager = args.get("isRouteManager").asBoolean();
        User curUser= (User) ur.findByEmail(email);

        curUser.setIsAdmin(isRouteManager);
        ur.save(curUser);

    }
    
}
