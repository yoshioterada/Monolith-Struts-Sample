package com.skishop.dao.point;

import com.skishop.domain.point.PointTransaction;
import java.util.List;

public interface PointTransactionDao {
  void insert(PointTransaction transaction);

  List listByUserId(String userId);
}
