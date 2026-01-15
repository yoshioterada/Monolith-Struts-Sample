package com.skishop.dao.address;

import com.skishop.domain.address.Address;
import java.util.List;

public interface UserAddressDao {
  List listByUserId(String userId);

  void save(Address address);
}
