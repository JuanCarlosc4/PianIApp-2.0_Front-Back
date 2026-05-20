package com.piania.auth.service;

import com.piania.auth.entity.RefreshToken;
import com.piania.auth.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyExpiration(RefreshToken token);
}