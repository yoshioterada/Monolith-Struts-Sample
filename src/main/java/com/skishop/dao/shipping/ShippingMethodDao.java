package com.skishop.dao.shipping;

import com.skishop.domain.shipping.ShippingMethod;
import java.util.List;

public interface ShippingMethodDao {
  List listActive();

  ShippingMethod findByCode(String code);

  void insert(ShippingMethod method);
}
