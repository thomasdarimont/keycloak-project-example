package com.acme.backend.springboot.profileapi.profile.model;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class UserConsentRepository {

    Map<String, Map<String,UserConsent>> consents = new HashMap<>();

    public void updateConsent(String userId, String clientId, Set<String> scopes) {
        consents.computeIfAbsent(userId,ignore -> new HashMap<>()).put(clientId,new UserConsent(userId, clientId, scopes));
    }

    public List<UserConsent> getConsents(String userId) {
        return new ArrayList<>(consents.get(userId).values());
    }

    public UserConsent getConsent(String userId, String clientId) {
        return consents.get(userId).get(clientId);
    }
}
