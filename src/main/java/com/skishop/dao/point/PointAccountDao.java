package com.skishop.dao.point;

import com.skishop.domain.point.PointAccount;

public interface PointAccountDao {
  PointAccount findByUserId(String userId);

  void insert(PointAccount account);

  void increment(String userId, int amount);
}
