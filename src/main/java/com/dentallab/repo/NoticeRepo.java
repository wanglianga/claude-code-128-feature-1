package com.dentallab.repo;

import com.dentallab.model.Notice;
import com.dentallab.model.NoticeAudience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NoticeRepo extends JpaRepository<Notice, Long> {
    List<Notice> findByOrderIdOrderByIdAsc(Long orderId);

    List<Notice> findByAudienceOrderByIdDesc(NoticeAudience audience);

    List<Notice> findByAudienceAndOrderIdInOrderByIdDesc(NoticeAudience audience, Collection<Long> orderIds);
}
