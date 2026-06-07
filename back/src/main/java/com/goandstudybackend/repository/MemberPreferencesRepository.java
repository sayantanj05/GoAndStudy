package com.goandstudybackend.repository;

import com.goandstudybackend.entity.MemberPreferences;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MemberPreferencesRepository extends MongoRepository<MemberPreferences, String> {
}
