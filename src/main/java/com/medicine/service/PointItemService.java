package com.medicine.service;

import com.medicine.model.PointItem;
import com.medicine.repository.PointItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointItemService {

    private final PointItemRepository pointItemRepository;

    /**
     * 모든 포인트 상품 조회 (포인트 낮은 순)
     */
    public List<PointItem> getAllItems() {
        return pointItemRepository.findAllByOrderByPointsAsc();
    }

    /**
     * 구매 가능한 포인트 상품만 조회 (포인트 낮은 순)
     */
    public List<PointItem> getAvailableItems() {
        return pointItemRepository.findByAvailableTrueOrderByPointsAsc();
    }

    /**
     * ID로 포인트 상품 조회
     */
    public Optional<PointItem> getItemById(Long id) {
        return pointItemRepository.findById(id);
    }

    /**
     * 포인트 상품 생성
     */
    @Transactional
    public PointItem createItem(PointItem item) {
        log.info("Creating point item - Name: {}, Points: {}", item.getName(), item.getPoints());
        return pointItemRepository.save(item);
    }

    /**
     * 포인트 상품 수정
     */
    @Transactional
    public PointItem updateItem(Long id, PointItem updatedItem) {
        PointItem existingItem = pointItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("포인트 상품을 찾을 수 없습니다."));

        existingItem.setName(updatedItem.getName());
        existingItem.setDescription(updatedItem.getDescription());
        existingItem.setPoints(updatedItem.getPoints());
        existingItem.setIcon(updatedItem.getIcon());
        existingItem.setColor(updatedItem.getColor());
        existingItem.setImageUrl(updatedItem.getImageUrl());
        existingItem.setAvailable(updatedItem.getAvailable());

        log.info("Updated point item - ID: {}, Name: {}", id, existingItem.getName());
        return pointItemRepository.save(existingItem);
    }

    /**
     * 포인트 상품 삭제
     */
    @Transactional
    public void deleteItem(Long id) {
        if (!pointItemRepository.existsById(id)) {
            throw new IllegalArgumentException("포인트 상품을 찾을 수 없습니다.");
        }
        pointItemRepository.deleteById(id);
        log.info("Deleted point item - ID: {}", id);
    }
}
