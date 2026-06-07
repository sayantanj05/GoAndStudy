package com.goandstudybackend.service;

import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.util.DateUtil;
import com.goandstudybackend.util.MemberIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberIdGeneratorService {

    private final MemberRepository memberRepository;

    public String generateMemberId() {
        String today = DateUtil.todayRegistrationKey();
        long count = memberRepository.countByRegistrationDate(today);
        long dailySequence = count + 1;
        return MemberIdGenerator.generate(today, dailySequence);
    }
}
