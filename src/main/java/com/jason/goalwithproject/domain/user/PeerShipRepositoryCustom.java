package com.jason.goalwithproject.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PeerShipRepositoryCustom {
    Page<PeerShip> searchMyPeers(Long userId, String search, Pageable pageable);
}
