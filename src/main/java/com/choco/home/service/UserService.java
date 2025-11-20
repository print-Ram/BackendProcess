package com.choco.home.service;

import org.springframework.stereotype.Repository;

import com.choco.home.pojo.User;

@Repository
public interface UserService {
    void saveUser(User user);
    User findByUsername(String username);
    
    
}
