package com.medicine.service;

import com.medicine.model.PointHistory;
import com.medicine.model.PointItem;
import com.medicine.model.User;
import com.medicine.repository.PointHistoryRepository;
import com.medicine.repository.PointItemRepository;
import com.medicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointItemRepository pointItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    /**
     * 사용 가능한 포인트 아이템 목록 조회
     */
    public List<PointItem> getAvailableItems() {
        return pointItemRepository.findByAvailableTrueOrderByPointsAsc();
    }

    /**
     * 모든 포인트 아이템 목록 조회
     */
    public List<PointItem> getAllItems() {
        return pointItemRepository.findAllByOrderByPointsAsc();
    }

    /**
     * 포인트 아이템 구매
     */
    @Transactional
    public void purchaseItem(User user, Long itemId) {
        PointItem item = pointItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        if (!item.getAvailable()) {
            throw new IllegalArgumentException("구매할 수 없는 상품입니다.");
        }

        if (user.getPoints() < item.getPoints()) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }

        // 포인트 차감
        user.setPoints(user.getPoints() - item.getPoints());
        userRepository.save(user);

        // 히스토리 기록
        PointHistory history = new PointHistory();
        history.setUser(user);
        history.setPoints(-item.getPoints());
        history.setType(PointHistory.PointType.PURCHASE);
        history.setDescription(item.getName() + " 구매");
        history.setPointItem(item);
        pointHistoryRepository.save(history);

        log.info("Point item purchased - User: {}, Item: {}, Points: -{}",
                user.getUsername(), item.getName(), item.getPoints());
    }

    /**
     * 포인트 적립
     */
    @Transactional
    public void addPoints(User user, Integer points, PointHistory.PointType type, String description) {
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);

        PointHistory history = new PointHistory();
        history.setUser(user);
        history.setPoints(points);
        history.setType(type);
        history.setDescription(description);
        pointHistoryRepository.save(history);

        log.info("Points added - User: {}, Points: +{}, Type: {}",
                user.getUsername(), points, type);
    }

    /**
     * 사용자 포인트 히스토리 조회
     */
    public List<PointHistory> getUserPointHistory(User user) {
        return pointHistoryRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 최근 포인트 히스토리 조회
     */
    public List<PointHistory> getRecentPointHistory(User user, LocalDateTime since) {
        return pointHistoryRepository.findByUserAndCreatedAtAfter(user, since);
    }
}
