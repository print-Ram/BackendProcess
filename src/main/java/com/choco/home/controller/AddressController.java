package com.choco.home.controller;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.choco.home.pojo.Address;
import com.choco.home.service.AddressService;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @PostMapping
    public ResponseEntity<String> addAddress(@RequestBody Address address) throws ExecutionException, InterruptedException {
        String id = addressService.saveAddressForUser(address);
        return ResponseEntity.ok(id);
    }
    
    @PutMapping("/{addressId}")
    public ResponseEntity<String> updateAddress(
            @PathVariable String addressId,
            @RequestBody Address address
    ) throws ExecutionException, InterruptedException {

        addressService.updateAddress(addressId, address);
        return ResponseEntity.ok("Address updated successfully");
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Address>> getAddresses(@PathVariable String userId) throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(addressService.getAddressesForUser(userId));
    }

    @DeleteMapping("/user/{userId}/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable String userId, @PathVariable String addressId)
            throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(addressService.deleteAddressForUser(userId, addressId));
    }

    @PostMapping("/setDefault/{userId}/{addressId}")
    public ResponseEntity<String> setDefaultAddress(@PathVariable String userId, @PathVariable String addressId)
            throws ExecutionException, InterruptedException {
        return ResponseEntity.ok(addressService.setDefaultAddress(userId, addressId));
    }
}
