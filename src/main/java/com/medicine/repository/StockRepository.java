package com.medicine.repository;

import com.medicine.model.Stock;
import com.medicine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByUserOrderByPurchaseDateDesc(User user);
    List<Stock> findByUser(User user);
}
