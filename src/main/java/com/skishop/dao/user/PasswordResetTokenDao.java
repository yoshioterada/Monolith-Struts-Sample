package com.skishop.dao.user;

import com.skishop.domain.user.PasswordResetToken;

public interface PasswordResetTokenDao {
  void insert(PasswordResetToken token);

  PasswordResetToken findByToken(String tokenValue);

  void markUsed(String tokenId);
}
