package com.medicine.repository;

import com.medicine.model.PushSubscription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushSubscriptionRepository extends CrudRepository<PushSubscription, String> {
    List<PushSubscription> findByUsername(String username);
}
