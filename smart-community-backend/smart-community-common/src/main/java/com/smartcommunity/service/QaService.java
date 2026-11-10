package com.smartcommunity.service;

import com.smartcommunity.dto.QaResponse;

import java.util.List;

public interface QaService {

    List<String> askStream(String question, Long userId, String userName);

    QaResponse askSync(String question, Long userId, String userName);
}
